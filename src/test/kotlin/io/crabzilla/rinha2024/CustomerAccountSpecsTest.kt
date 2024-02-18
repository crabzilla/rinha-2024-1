package io.crabzilla.rinha2024

import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewDeposit
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewWithdraw
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.RegisterNewAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.CustomerAccountRegistered
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.WithdrawCommitted
import io.crabzilla.rinha2024.account.model.LimitExceededException
import io.crabzilla.rinha2024.account.model.accountDecideFn
import io.crabzilla.rinha2024.account.model.accountEvolveFn
import io.github.crabzilla.core.CrabzillaCommandsSession
import io.github.crabzilla.core.TestSpecification
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Customer account scenarios - Rinha 2004")
class CustomerAccountSpecsTest {
  private val id = 1
  private lateinit var session: CrabzillaCommandsSession<CustomerAccountCommand, CustomerAccount, CustomerAccountEvent>

  // TODO add a test using a custom timeGenerator and then assert on events dates

  @BeforeEach
  fun setup() {
    session =
      CrabzillaCommandsSession(
        initialState = CustomerAccount(id = 0, limit = 0, balance = 0),
        evolveFunction = accountEvolveFn,
        decideFunction = accountDecideFn,
      )
  }

  @Test
  fun `given a RegisterNewAccount command, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 10, balance = 5))
      .then { // assert events
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 10, balance = 5))
      }
      .then { // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 10, balance = 5))
      }
  }

  @Test
  fun `given a RegisterNewAccount then a CommitNewDeposit $20, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 10, balance = 5))
      .whenCommand(CommitNewDeposit(amount = 20, description = "ya ya"))
      .then { // assert events
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 10, balance = 5))
        it.appliedEvents()[1].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 20,
                description = "ya ya",
                balance = 25
            )
        )
      }
      .then { // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 10, balance = 25))
        it.appliedEvents()[1].shouldBe(it.currentState().lastTenTransactions[0])
      }
  }

  @Test
  fun `given a RegisterNewAccount then a CommitNewWithdraw $10, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 50, balance = 100))
      .whenCommand(CommitNewWithdraw(amount = 10, description = "ya ya"))
      .then {
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 50, balance = 100))
        it.appliedEvents()[1].shouldBe(
            WithdrawCommitted(
                amount = 10,
                description = "ya ya",
                balance = 90
            )
        )
      }
      .then { // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 50, balance = 90))
      }
  }

  @Test
  fun `given a RegisterNewAccount $5 then a CommitNewWithdraw $100, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 0, balance = 5))
      .whenCommand(CommitNewWithdraw(amount = 10, description = "ya ya"))
      .then {
        // assert exception
        it.lastException() shouldBe LimitExceededException(amount = 10, limit = 0)
      }
      .then {
        // assert events
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 0, balance = 5))
      }
      .then {
        // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 0, balance = 5))
      }
  }

  @Test
  fun `given a RegisterNewAccount then 10 CommitNewDeposit then 5 CommitNewWithdraw $1, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 0, balance = 0))
      .whenCommand(CommitNewDeposit(amount = 1, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 2, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 3, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 4, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 5, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 6, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 7, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 8, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 9, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 10, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 11, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 12, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 13, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 14, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 15, description = "ya ya"))
      .then {
        it.appliedEvents().size shouldBe 15
      }
      .then { // assert state

        // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 0, balance = 5))

        val stateTransactions = it.currentState().lastTenTransactions
        stateTransactions.size shouldBe 10
        stateTransactions[0].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 5,
                description = "ya ya",
                balance = 15
            )
        )
        stateTransactions[1].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 6,
                description = "ya ya",
                balance = 21
            )
        )
        stateTransactions[2].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 7,
                description = "ya ya",
                balance = 28
            )
        )
        stateTransactions[3].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 8,
                description = "ya ya",
                balance = 36
            )
        )
        stateTransactions[4].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 9,
                description = "ya ya",
                balance = 45
            )
        )
        stateTransactions[5].shouldBe(
            CustomerAccountEvent.DepositCommitted(
                amount = 10,
                description = "ya ya",
                balance = 55
            )
        )
        stateTransactions[6].shouldBe(
            WithdrawCommitted(
                amount = 11,
                description = "ya ya",
                balance = 44
            )
        )
        stateTransactions[7].shouldBe(
            WithdrawCommitted(
                amount = 12,
                description = "ya ya",
                balance = 32
            )
        )
        stateTransactions[8].shouldBe(
            WithdrawCommitted(
                amount = 13,
                description = "ya ya",
                balance = 19
            )
        )
        stateTransactions[9].shouldBe(
            WithdrawCommitted(
                amount = 14,
                description = "ya ya",
                balance = 5
            )
        )

        // assert exception
        it.lastException() shouldBe LimitExceededException(amount = 15, limit = 0)
      }
  }

    private fun CustomerAccount.shouldBe(customerAccount: CustomerAccount) {
        try {
            this.shouldBeEqualToIgnoringFields(customerAccount, CustomerAccount::lastTenTransactions)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun CustomerAccountEvent.shouldBe(customerAccountEvent: CustomerAccountEvent) {
        try {
            this.shouldBeEqualToIgnoringFields(customerAccountEvent, CustomerAccountEvent::date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}