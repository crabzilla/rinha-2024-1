package io.crabzilla.rinha2024.account

import com.fasterxml.jackson.databind.ObjectMapper
import io.crabzilla.rinha2024.AbstractCrabzillaVerticle
import io.crabzilla.rinha2024.Util.getHostname
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
import io.vertx.ext.web.client.WebClient
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
        val client = WebClient.create(vertx)

        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(httpPort.toInt()) { http ->
                if (http.succeeded()) {
                    startPromise.complete()
                    logger.info("HTTP server started on hostname $thisNode port $httpPort")
                    return@listen
                }
                startPromise.fail(http.cause())
            }

        router.route().failureHandler { routingContext ->
//            routingContext.failure().printStackTrace()
            logger.error("Aqui", routingContext.failure()?.message ?: "WTF?")
            routingContext.response()
                .putHeader("Content-type", "application/json; charset=utf-8")
                .setStatusCode(500)
                .end(routingContext.failure()?.message ?: "internal server error")
        }

        val handler: (RoutingContext) -> Unit = { routingContext ->

            val id = routingContext.request().getParam("id").toInt()
            val sharedData = vertx.sharedData()
            val lockMap = sharedData.getLocalMap<String, Boolean>("lockMap")

            if (lockMap.getOrDefault(id.toString(), false)) {
                routingContext.response().setStatusCode(500).end("The request is being processed")
            } else {
                lockMap.put(id.toString(), true)
                logger.trace("Will post {}", routingContext.body().asJsonObject())
                val request = routingContext.body().asJsonObject()

                postTransaction(id, mapRequestToCommand(request), commandHandler)
                    .onSuccess { json ->
                        lockMap.remove(id.toString())
                        logger.trace("Returning {} for request {}", json, request)
                        routingContext.response().putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(json.encodePrettily())
                    }
                    .onFailure { error ->
                        lockMap.remove(id.toString())
                        logger.error("Error when handling command for request {}, error {}", request, error?.message)
                        when (error.cause) {
                            is LimitExceededException -> {
                                logger.debug("Limit exceeded, returning 422")
                                routingContext.response().setStatusCode(422).end()
                            }

                            else -> {
                                routingContext.response().setStatusCode(500).end(error?.message)
                            }
                        }
                    }
            }

        }

        router.post("/local/clientes/:id/transacoes")
            .handler(BodyHandler.create())
            .handler(handler)

        router.post("/clientes/:id/transacoes")
            .handler(BodyHandler.create())
            .handler { routingContext ->
                val id = routingContext.request().getParam("id").toInt()
                val request = routingContext.body().asJsonObject()
                if ((id < 1 || id > 5)) {
                    logger.debug("Not found")
                    routingContext.response().setStatusCode(404).end()
                    return@handler
                }
                if (!validateRequest(request)) {
                    logger.debug("Invalid request")
                    routingContext.response().setStatusCode(400).end()
                    return@handler
                }
                if (!dickVigaristaMode || "dev" == activeProfile) {
                    logger.info("Handling it locally id {}:", id)
                    handler.invoke(routingContext)
                    return@handler
                }
                val targetNode = getTarget(id)
                if (targetNode.targetInstance?.hostname == thisNode) {
                    logger.info("Handling it locally id {}: {}", id, targetNode)
                    handler.invoke(routingContext)
                    return@handler
                }
                logger.info("Routing it id {}: {}", id, targetNode)
                routeTransaction(targetNode, client, id, request)
                    .onSuccess { (statusCode, jsonBody) ->
                        when (statusCode) {
                            200 -> routingContext.response().setStatusCode(statusCode).end(jsonBody!!.encodePrettily())
                            else -> routingContext.response().setStatusCode(statusCode).end()
                        }
                    }
                    .onFailure {
                        routingContext.response().setStatusCode(500).end("I'm lost, sorry")
                    }
            }

    }


    fun routeTransaction(
        targetNode: TargetNode,
        client: WebClient,
        id: Int,
        json: JsonObject
    ): Future<Pair<Int, JsonObject?>> {
        return client.post(
            targetNode.targetInstance!!.port,
            targetNode.targetInstance.hostname,
            "/local/clientes/$id/transacoes"
        )
            .sendJsonObject(json)
            .onSuccess { response ->
                logger.debug("Received response with status code ${response.statusCode()}")
            }
            .map {
                when (it.statusCode()) {
                    200 -> Pair(it.statusCode(), it.body().toJsonObject())
                    else -> Pair(it.statusCode(), null)
                }
            }
            .onFailure { error ->
                logger.debug("Something went wrong ${error.message}")
            }
    }


    private fun postTransaction(
        id: Int,
        command: CustomerAccountCommand,
        commandHandler: CommandHandler<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent>
    ): Future<JsonObject> {
        val streamName = "Accounts@$id"
        return commandHandler.handle(TargetStream(name = streamName), command).map {
            val state = it.snapshot.state
            val limit = state.limit
            val balance = state.balance
            JsonObject().put("limite", limit).put("total", balance)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccountTransactionVerticle::class.java)

        val thisNode = getHostname()

        data class Node(val hostname: String, val port: Int)

        data class TargetNode(val targetInstance: Node?)

        private fun isEven(number: Int): Boolean {
            return number % 2 == 0
        }

        fun getTarget(id: Int): TargetNode {
            return when (isEven(id)) {
                true -> TargetNode(Node("api02", 8083))
                false -> TargetNode(Node("api01", 8081))
            }
        }

        fun validateRequest(json: JsonObject): Boolean {
            val tipo = json.getString("tipo")
            val valor = json.getString("valor")
            val descricao = json.getString("descricao")
            fun valorIsInt(): Boolean {
                try {
                    Integer.parseInt(valor)
                    return true
                } catch (e: Exception) {
                    return false
                }
            }

            return (tipo != null && (tipo == "c" || tipo == "d")
                    && (descricao != null && descricao.length <= 10)
                    && (valor != null && !valor.contains(".") && valorIsInt() && Integer.parseInt(valor) > 0))
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