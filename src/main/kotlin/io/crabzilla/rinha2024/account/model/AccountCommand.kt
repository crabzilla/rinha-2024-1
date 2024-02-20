package io.crabzilla.rinha2024.account.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewDeposit
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewWithdraw
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.RegisterNewAccount

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(RegisterNewAccount::class, name = "RegisterNewAccount"),
    JsonSubTypes.Type(CommitNewDeposit::class, name = "CommitNewDeposit"),
    JsonSubTypes.Type(CommitNewWithdraw::class, name = "CommitNewWithdraw"),
)
sealed interface CustomerAccountCommand {
    data class RegisterNewAccount(val id: Int, val limit: Int, val balance: Int) : CustomerAccountCommand

    data class CommitNewDeposit(val amount: Int, val description: String) : CustomerAccountCommand

    data class CommitNewWithdraw(val amount: Int, val description: String) : CustomerAccountCommand
}

val accountDecideFn: (state: CustomerAccount, command: CustomerAccountCommand) -> List<CustomerAccountEvent> =
    { state, command ->
        when (command) {
            is RegisterNewAccount -> state.register(id = command.id, limit = command.limit, balance = command.balance)
            is CommitNewDeposit -> state.deposit(amount = command.amount, description = command.description)
            is CommitNewWithdraw -> state.withdraw(amount = command.amount, description = command.description)
        }
    }
