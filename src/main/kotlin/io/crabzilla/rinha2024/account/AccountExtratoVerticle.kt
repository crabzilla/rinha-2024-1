package io.crabzilla.rinha2024.account

import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@ApplicationScoped
class AccountExtratoVerticle : AbstractAccountVerticle() {

    @ConfigProperty(name = "app.extrato.http.port")
    private lateinit var httpPort: String

    override fun start(startPromise: Promise<Void>) {

        logger.info("Starting")

        val router = Router.router(vertx)

        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(httpPort.toInt()) { http ->
                if (http.succeeded()) {
                    startPromise.complete()
                    logger.info("HTTP server started on port $httpPort")
                    return@listen
                }
                startPromise.fail(http.cause())
            }

        router.get("/clientes/:id/extrato")
            .handler { routingContext ->
                val id = routingContext.request().getParam("id").toInt()
                if ((id < 1 || id > 5)) {
                    routingContext.response().setStatusCode(404).end()
                    return@handler
                }
                val state = SHARED_DATABASE[id]
                logger.debug("Found state {}", state)
                val view = mapStateToExtratoView(state!!)
                val saldo = view.getJsonObject("saldo")
                saldo.put("data_extrato", LocalDateTime.now())
                routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(view.encode())
            }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AccountExtratoVerticle::class.java)

        val mapStateToExtratoView: (CustomerAccount) -> JsonObject = { state ->
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
                }
            JsonObject().put("saldo", saldo).put("ultimas_transacoes", ultimasTransacoes)
        }
    }

}