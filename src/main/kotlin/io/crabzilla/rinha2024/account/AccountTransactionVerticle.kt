package io.crabzilla.rinha2024.account

import com.fasterxml.jackson.databind.ObjectMapper
import io.crabzilla.rinha2024.AbstractCrabzillaVerticle
import io.crabzilla.rinha2024.account.AccountConfig.accountCache
import io.crabzilla.rinha2024.account.AccountConfig.accountsCommandHandler
import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.crabzilla.rinha2024.account.model.LimitExceededException
import io.github.crabzilla.command.CommandHandler
import io.github.crabzilla.stream.TargetStream
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

@ApplicationScoped
class AccountTransactionVerticle : AbstractCrabzillaVerticle() {

    @ConfigProperty(name = "app.transacoes.http.port")
    private lateinit var httpPort: String

    @Inject
    private lateinit var objectMapper: ObjectMapper

    override fun start(startPromise: Promise<Void>) {

        logger.info("Starting")

        super.start(startPromise)

        val commandHandler =
            accountsCommandHandler(
                crabzillaContext = crabzillaContext,
                cache = accountCache(),
                objectMapper = objectMapper,
            )

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

        router.route().failureHandler { routingContext ->
            routingContext.failure().printStackTrace()
//            logger.error("Aqui", routingContext.failure())
            routingContext.response()
                .putHeader("Content-type", "application/json; charset=utf-8")
                .setStatusCode(500)
                .end(routingContext.failure().message ?: "internal server error")
        }

        router.post("/clientes/:id/transacoes")
            .handler(BodyHandler.create())
            .handler {
                logger.debug("Will post {}", it.body().asJsonObject())
                val id = it.request().getParam("id").toInt()
                val request = it.body().asJsonObject()
                if ((id < 1 || id > 5) || !validateRequest(request)) {
                    logger.debug("Invalid request")
                    it.fail(400)
                    return@handler
                }
                postTransaction(id, mapRequestToCommand(request), commandHandler)
                    .onSuccess { json ->
                        logger.debug("Returning {}", json)
                        it.response().putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(json.encode())
                    }
                    .onFailure { error ->
                        logger.error("Error when handling command: {}", error?.message)
                        when (error.cause) {
                            is LimitExceededException -> {
                                logger.debug("Limit exceeded, returning 422")
                                it.fail(422)
                            }
                            else -> {
                                it.fail(500)
                            }
                        }
                    }

            }
    }

    private fun postTransaction(
        id: Int, command: CustomerAccountCommand,
        commandHandler: CommandHandler<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent>
    ): Future<JsonObject> {
        val targetStream = TargetStream(name = "Accounts@$id")
        return commandHandler.handle(targetStream, command)
            .map { result ->
                val state = result.snapshot.state
                JsonObject().put("limite", state.limit).put("total", state.balance)
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccountTransactionVerticle::class.java)

        fun validateRequest(json: JsonObject): Boolean {
            val tipo = json.getString("tipo")
            val valor = json.getInteger("valor")
            val descricao = json.getString("descricao")
            return (tipo != null && (tipo == "c" || tipo == "d")
                    && (descricao != null && descricao.length <= 10)
                    && (valor != null && valor > 0))
        }

        fun mapRequestToCommand(json: JsonObject): CustomerAccountCommand {
            val tipo = json.getString("tipo")
            val valor = json.getInteger("valor")
            val descricao = json.getString("descricao")
            return when (tipo) {
                "c" -> CustomerAccountCommand.CommitNewDeposit(amount = valor, description = descricao)
                "d" -> CustomerAccountCommand.CommitNewWithdraw(amount = valor, description = descricao)
                else -> TODO("Never will happen")
            }
        }

    }

}