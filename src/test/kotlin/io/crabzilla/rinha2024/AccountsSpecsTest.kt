package io.crabzilla.rinha2024

import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewDeposit
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewWithdraw
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.RegisterNewAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.CustomerAccountRegistered
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.DepositCommitted
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.WithdrawCommitted
import io.crabzilla.rinha2024.account.model.LimitExceededException
import io.crabzilla.rinha2024.account.model.accountDecideFn
import io.crabzilla.rinha2024.account.model.accountEvolveFn
import io.github.crabzilla.core.Session
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class AccountsSpecsTest : BehaviorSpec({

    val id = 1
    val now = LocalDateTime.now()
    val initialState = CustomerAccount(id = 0, limit = 0, balance = 0)

    val session = Session(
        initialState = initialState,
        evolveFunction = accountEvolveFn,
        decideFunction = accountDecideFn,
        injectFunction = { account -> account.timeGenerator = { now }; account }
    )

    fun checkTransactionsOrder(transactions: List<CustomerAccountEvent>): Boolean {
        // Check if the list is sorted in descending order by date
        for (i in 0 until transactions.size - 1) {
            if (transactions[i].date < transactions[i + 1].date) {
                return false
            }
        }
        return true
    }

    Given("a RegisterNewAccount command #1") {
        val command = RegisterNewAccount(id, limit = 10, balance = 5)
        When("it's handled") {
            session.reset().decide(command)
            Then("the state is correct") {
                session.currentState() shouldBeEqual CustomerAccount(id, limit = 10, balance = 5)
            }
            Then("the events are correct") {
                session.appliedEvents() shouldBeEqual listOf(
                    CustomerAccountRegistered(id, limit = 10, balance = 5, date = now)
                )
            }
        }
    }

    Given("a RegisterNewAccount command #2") {
        val command1 = RegisterNewAccount(id, limit = 10, balance = 5)
        And("a CommitNewDeposit ") {
            val command2 = CommitNewDeposit(amount = 20, description = "ya ya")
            When("it's handled") {
                session.reset().decide(command1).decide(command2)
                Then("the state is correct") {
                    val expectedTransactions = mutableListOf<CustomerAccountEvent>(
                        DepositCommitted(amount = 20, description = "ya ya", balance = 25, date = now)
                    )
                    session.currentState() shouldBeEqual
                            CustomerAccount(id, limit = 10, balance = 25, lastTenTransactions = expectedTransactions)
                }
                Then("the events are correct") {
                    session.appliedEvents() shouldBeEqual listOf(
                        CustomerAccountRegistered(id, limit = 10, balance = 5, date = now),
                        DepositCommitted(amount = 20, description = "ya ya", balance = 25, date = now),
                    )
                }
            }
        }
    }

    Given("a RegisterNewAccount command #3") {
        val command1 = RegisterNewAccount(id, limit = 10, balance = 5)
        And("a CommitNewWithDraw ") {
            val command2 = CommitNewWithdraw(amount = 5, description = "i need it")
            When("it's handled") {
                session.reset().decide(command1).decide(command2)
                val event = WithdrawCommitted(
                    amount = 5,
                    description = "i need it",
                    balance = 0,
                    date = now
                )
                Then("the state is correct") {
                    val expectedTransactions = mutableListOf<CustomerAccountEvent>(event)
                    session.currentState() shouldBeEqual CustomerAccount(
                        id,
                        limit = 10,
                        balance = 0,
                        lastTenTransactions = expectedTransactions
                    )
                }
                Then("the events are correct") {
                    session.appliedEvents() shouldBeEqual listOf(
                        CustomerAccountRegistered(
                            id,
                            limit = 10,
                            balance = 5,
                            date = now
                        ), event
                    )
                }
            }
        }
    }

    Given("a RegisterNewAccount command #4") {
        val command1 = RegisterNewAccount(id, limit = 10, balance = 5)
        And("a CommitNewWithDraw exceeding the limit") {
            val command2 = CommitNewWithdraw(amount = 16, description = "i need it")
            When("it's handled") {
                val exception =
                    shouldThrow<Exception> {
                        session.reset().decide(command1).decide(command2)
                    }
                Then("the exception is correct") {
                    val state = session.currentState()
                    exception shouldBeEqual LimitExceededException(
                        accountId = state.id,
                        currentBalance = state.balance,
                        newBalance =  state.balance.minus(command2.amount),
                        amountRequested = command2.amount,
                        limit = state.limit
                        )
                }
                Then("the state is correct") {
                    session.currentState() shouldBeEqual CustomerAccount(id, limit = 10, balance = 5)
                }
                Then("the events are correct") {
                    session.appliedEvents() shouldBeEqual listOf(
                        CustomerAccountRegistered(id, limit = 10, balance = 5, date = now)
                    )
                }
            }
        }
    }

    Given("a RegisterNewAccount command and a 15 CommitNewDeposit commands") {
        val commands = listOf(
            RegisterNewAccount(id, limit = 0, balance = 0),
            CommitNewDeposit(amount = 1, description = "ya ya"),
            CommitNewDeposit(amount = 2, description = "ya ya"),
            CommitNewDeposit(amount = 3, description = "ya ya"),
            CommitNewDeposit(amount = 4, description = "ya ya"),
            CommitNewDeposit(amount = 5, description = "ya ya"),
            CommitNewDeposit(amount = 6, description = "ya ya"),
            CommitNewDeposit(amount = 7, description = "ya ya"),
            CommitNewDeposit(amount = 8, description = "ya ya"),
            CommitNewDeposit(amount = 9, description = "ya ya"),
            CommitNewDeposit(amount = 10, description = "ya ya"),
            CommitNewDeposit(amount = 11, description = "ya ya"),
            CommitNewDeposit(amount = 12, description = "ya ya"),
            CommitNewDeposit(amount = 13, description = "ya ya"),
            CommitNewDeposit(amount = 14, description = "ya ya"),
            CommitNewDeposit(amount = 15, description = "ya ya")
        )
        When("all commands are handled") {
            session.reset()
            commands.forEach {
                session.decide(it)
                assert(true)
            }
        }
        Then("Only the last 10 events are within lastTenTransactions") {
            session.currentState().lastTenTransactions.size shouldBeEqual 10
        }
        Then("LastTenTransactions is sorted by date in descendent order") {
            print(session.currentState())
            checkTransactionsOrder(session.currentState().lastTenTransactions) shouldBe true
        }
    }

    // TODO check transactions in descendent order
})
