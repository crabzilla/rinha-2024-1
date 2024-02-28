package io.crabzilla.rinha2024.account

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.crabzilla.rinha2024.account.effects.AccountViewEffect
import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.crabzilla.rinha2024.account.model.accountDecideFn
import io.crabzilla.rinha2024.account.model.accountEvolveFn
import io.github.crabzilla.command.CommandHandler
import io.github.crabzilla.command.CommandHandlerConfig
import io.github.crabzilla.command.CommandHandlerImpl
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stream.StreamSnapshot
import io.vertx.core.json.JsonObject

object AccountConfig {

    fun accountCache(): Cache<Int, StreamSnapshot<CustomerAccount>> {
        return Caffeine.newBuilder()
            .initialCapacity(5)
            .maximumSize(5)
            .build()
    }

    fun accountsCommandHandler(
        crabzillaContext: CrabzillaContext,
        cache: Cache<Int, StreamSnapshot<CustomerAccount>>,
        objectMapper: ObjectMapper,
    )
            : CommandHandler<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent> {

        val config =
            CommandHandlerConfig(
                initialState = CustomerAccount(id = 0, limit = 0, balance = 0),
                evolveFunction = accountEvolveFn,
                decideFunction = accountDecideFn,
                eventSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerAccountEvent::class),
                commandSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerAccountCommand::class),
                viewEffect = AccountViewEffect(customerAccountToJsonViewMapper),
                snapshotCache = cache,
                notifyPostgres = false,
                persistCommands = false,
            )

        return CommandHandlerImpl(crabzillaContext, config)
    }

    private val customerAccountToJsonViewMapper: (CustomerAccount) -> JsonObject = { state ->
        val saldo = JsonObject()
            .put("total", state.balance)
            .put("limite", state.limit)
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
            }.reversed()
        JsonObject().put("saldo", saldo).put("ultimas_transacoes", ultimasTransacoes)
    }


}