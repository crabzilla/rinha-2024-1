package io.crabzilla.rinha2024.account.model


import com.fasterxml.jackson.annotation.JsonIgnore
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.CustomerAccountRegistered
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.DepositCommitted
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.WithdrawCommitted
import java.time.LocalDateTime

data class CustomerAccount(
    val id: Int,
    val limit: Int,
    val balance: Int = 0,
    val lastTenTransactions: List<CustomerAccountEvent> = listOf(),
) {
    // This state model also have the last 10 transactions. Just to skip read/view model and optimize it for perf.

    @JsonIgnore
    @Transient
    var timeGenerator: () -> LocalDateTime = { LocalDateTime.now() }

    fun register(id: Int, limit: Int, balance: Int): List<CustomerAccountEvent> {
        return listOf(
            CustomerAccountRegistered(
                id = id,
                limit = limit,
                balance = balance,
                date = timeGenerator.invoke()
            )
        )
    }

    fun deposit(amount: Int, description: String): List<CustomerAccountEvent> {
        return listOf(
            DepositCommitted(
                amount = amount,
                description = description,
                balance = balance.plus(amount),
                date = timeGenerator.invoke()
            ),
        )
    }

    fun withdraw(amount: Int, description: String): List<CustomerAccountEvent> {
        val newBalance = balance.minus(amount)
        if (newBalance + limit < 0) {
            throw LimitExceededException(
                accountId = id,
                currentBalance = balance,
                newBalance = newBalance,
                amountRequested = amount,
                limit = limit
            )
        }
        return listOf(
            WithdrawCommitted(
                amount = amount,
                description = description,
                balance = newBalance,
                date = timeGenerator.invoke()
            ),
        )
    }

    override fun toString(): String {
        val transactions = if (lastTenTransactions.isNotEmpty()) {
            lastTenTransactions.joinToString(separator = "\n") { it.toString() }
        } else {
            "No transactions available"
        }
        return "Customer Account - ID: $id, Limit: $limit, Balance: $balance, Transactions:\n$transactions"
    }
}

data class LimitExceededException(
    val accountId: Int,
    val currentBalance: Int,
    val newBalance: Int,
    val amountRequested: Int,
    val limit: Int
) :
    RuntimeException("Account $accountId: Can't withdraw $amountRequested: balance is $currentBalance final balance would be $newBalance and the limit is $limit")
