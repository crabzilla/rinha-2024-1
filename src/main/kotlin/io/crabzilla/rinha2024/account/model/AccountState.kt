package io.crabzilla.rinha2024.account.model


import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.CustomerAccountRegistered
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.DepositCommitted
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.WithdrawCommitted
import java.time.LocalDateTime

data class CustomerAccount(
  val id: Int,
  val limit: Int,
  val balance: Int = 0,
  val lastTenTransactions: MutableList<CustomerAccountEvent> = withMaxSize(10),
) {
  // This state model also have the last 10 transactions. Just to skip read/view model and optimize it for perf.

  var timeGenerator: (() -> LocalDateTime)? = null

  fun register(id: Int, limit: Int, balance: Int): List<CustomerAccountEvent> {
    return listOf(CustomerAccountRegistered(
      id = id,
      limit = limit,
      balance = balance,
      date = timeGenerator!!.invoke()
    ))
  }

  fun deposit(amount: Int, description: String): List<CustomerAccountEvent> {
    return listOf(
      DepositCommitted(
        amount = amount,
        description = description,
        balance = balance.plus(amount),
        date = timeGenerator!!.invoke()
      ),
    )
  }

  fun withdraw(amount: Int, description: String): List<CustomerAccountEvent> {
    val newBalance = balance.minus(amount)
    if (newBalance + limit <0) {
      throw LimitExceededException(amount, limit)
    }
    return listOf(
      WithdrawCommitted(
        amount = amount,
        description = description,
        balance = newBalance,
        date = timeGenerator!!.invoke()
      ),
    )
  }
}

data class LimitExceededException(val amount: Int, val limit: Int) :
  RuntimeException("Amount $amount exceeds limit $limit")

fun <T> withMaxSize(maxSize: Int): MutableList<T> {
  return object : ArrayList<T>(maxSize) {
    override fun add(element: T): Boolean {
      if (size >= maxSize) {
        removeAt(0) // Remove the oldest element
      }
      return super.add(element)
    }
  }
}
