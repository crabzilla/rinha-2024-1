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
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Duration


class AccountConfig {

    @Inject
    private lateinit var objectMapper: ObjectMapper

    @ApplicationScoped
    fun accountCache(): Cache<Int, StreamSnapshot<CustomerAccount>> {
        return Caffeine.newBuilder()
            .initialCapacity(5)
            .maximumSize(5)
            .build()
    }

    @ApplicationScoped
    fun accountsCommandHandler(context: CrabzillaContext,
                               cache: Cache<Int, StreamSnapshot<CustomerAccount>>)
            : CommandHandler<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent> {

        val config =
            CommandHandlerConfig(
                initialState = CustomerAccount(id = 0, limit = 0, balance = 0),
                evolveFunction = accountEvolveFn,
                decideFunction = accountDecideFn,
//                    injectorFunction = { account -> account.timeGenerator = { LocalDateTime.now() }; account  },
                eventSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerAccountEvent::class),
                commandSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerAccountCommand::class),
                viewEffect = AccountViewEffect(mapStateToView),
                snapshotCache = cache,
                notifyPostgres = false,
                persistCommands = false
            )
        return CommandHandlerImpl(context, config)
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