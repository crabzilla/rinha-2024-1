package io.crabzilla.rinha2024.account.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.CustomerAccountRegistered
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.DepositCommitted
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent.WithdrawCommitted
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(CustomerAccountRegistered::class, name = "CustomerAccountRegistered"),
  JsonSubTypes.Type(DepositCommitted::class, name = "DepositCommitted"),
  JsonSubTypes.Type(WithdrawCommitted::class, name = "WithdrawCommitted"),
)
sealed class CustomerAccountEvent(open val date: LocalDateTime) {
  data class CustomerAccountRegistered(
    val id: Int,
    val limit: Int,
    val balance: Int,
    override val date: LocalDateTime = LocalDateTime.now(),
  ) :
    CustomerAccountEvent(date)

  data class DepositCommitted(
    val amount: Int,
    val description: String,
    val balance: Int,
    override val date: LocalDateTime = LocalDateTime.now(),
  ) : CustomerAccountEvent(date)

  data class WithdrawCommitted(
    val amount: Int,
    val description: String,
    val balance: Int,
    override val date: LocalDateTime = LocalDateTime.now(),
  ) : CustomerAccountEvent(date)
}

val accountEvolveFn: (CustomerAccount, CustomerAccountEvent) -> CustomerAccount = {
    state: CustomerAccount, event: CustomerAccountEvent ->
  fun newList(): MutableList<CustomerAccountEvent> {
    state.lastTenTransactions.add(event)
    return state.lastTenTransactions
  }
  when (event) {
    is CustomerAccountRegistered -> CustomerAccount(id = event.id, limit = event.limit, balance = event.balance)
    is DepositCommitted -> state.copy(balance = state.balance.plus(event.amount), lastTenTransactions = newList())
    is WithdrawCommitted -> state.copy(balance = state.balance.minus(event.amount), lastTenTransactions = newList())
  }
}
