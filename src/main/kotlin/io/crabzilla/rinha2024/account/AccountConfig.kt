package io.crabzilla.rinha2024.accounts.account

import com.fasterxml.jackson.databind.ObjectMapper
import io.crabzilla.rinha2024.account.effects.AccountViewEffect
import io.crabzilla.rinha2024.account.model.AccountStateFactory
import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.crabzilla.rinha2024.account.model.accountDecideFn
import io.crabzilla.rinha2024.account.model.accountEvolveFn
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.writer.WriterApi
import io.github.crabzilla.writer.WriterApiImpl
import io.github.crabzilla.writer.WriterConfig
import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

class AccountConfig {

    @Inject
    private lateinit var objectMapper: ObjectMapper

    @ApplicationScoped
    fun accountsWriter(context: CrabzillaContext): WriterApi<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent> {
            val config =
                WriterConfig(
                    initialStateFactory = AccountStateFactory(),
                    evolveFunction = accountEvolveFn,
                    decideFunction = accountDecideFn,
                    eventSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerAccountEvent::class),
                    commandSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerAccountCommand::class),
                    viewEffect = AccountViewEffect(mapStateToView),
                    notifyPostgres = false,
                    persistCommands = false
                )
        return WriterApiImpl(context, config)
    }

    companion object {
        val mapStateToView: (CustomerAccount) -> JsonObject = { state ->
            val saldo = JsonObject()
                .put("limite", state.limit)
                .put("total", state.balance)
            val ultimasTransacoes = state.lastTenTransactions
                .map { event ->
                    when (event) {
                        is CustomerAccountEvent.CustomerAccountRegistered -> TODO()
                        is CustomerAccountEvent.DepositCommitted -> JsonObject()
                            .put("valor", event.amount)
                            .put("tipo", "c")
                            .put("descricao", event.description)
                            .put("realizada_em", event.date)
                        is CustomerAccountEvent.WithdrawCommitted -> JsonObject()
                            .put("valor", event.amount)
                            .put("tipo", "d")
                            .put("descricao", event.description)
                            .put("realizada_em", event.date)
                    }
                }
            JsonObject().put("saldo", saldo).put("ultimas_transacoes", ultimasTransacoes)
        }

    }

}