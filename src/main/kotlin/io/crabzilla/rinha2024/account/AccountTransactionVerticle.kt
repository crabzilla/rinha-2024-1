package io.crabzilla.rinha2024.account

import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.LimitExceededException
import io.crabzilla.rinha2024.account.model.accountDecideFn
import io.crabzilla.rinha2024.account.model.accountEvolveFn
import io.github.crabzilla.core.Session
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

@ApplicationScoped
class AccountTransactionVerticle : AbstractAccountVerticle() {

    @ConfigProperty(name = "app.transacoes.http.port")
    private lateinit var httpPort: String

    override fun start(startPromise: Promise<Void>) {

        logger.info("Starting")

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
            val response: HttpServerResponse = routingContext.response()
            fun isExpectedResponse(): Boolean {
                return response.statusCode == 200 || response.statusCode == 400 || response.statusCode != 422
            }
            if (!isExpectedResponse()) {
                logger.error(
                    "Request {} got response {} for {}",
                    routingContext.request(),
                    response.statusCode,
                    routingContext.request().uri()
                )
            }
        }

        val requestHandler: (RoutingContext) -> Unit = { routingContext ->

            val id = routingContext.request().getParam("id").toInt()
            val request = routingContext.body().asJsonObject()

            postTransaction(id, mapRequestToCommand(request))
                .onSuccess { newState ->
                    logger.trace("Returning {} for request {}", newState, request)
                    routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(mapStateToView(newState).encode())
                }
                .onFailure { error ->
                    when (error) {
                        is LimitExceededException -> {
                            logger.debug("Limit exceeded, returning 422")
                            routingContext.response().setStatusCode(422).end()
                        }

                        else -> {
                            logger.error(
                                "Error when handling command for request {}, error {}",
                                request,
                                error?.message
                            )
                            routingContext.response().setStatusCode(500).end(error?.message)
                        }
                    }
                }

        }

        router.post("/local/clientes/:id/transacoes")
            .handler(BodyHandler.create())
            .handler(requestHandler)

        router.post("/clientes/:id/transacoes")
            .handler(BodyHandler.create())
            .handler { routingContext ->
                val id = routingContext.request().getParam("id").toInt()
                val requestAsJson = routingContext.body().asJsonObject()
                fun routeTransaction(
                    targetNode: TargetNode
                ): Future<HttpResponse<Buffer>>? {
                    return client.post(
                        targetNode.targetInstance!!.port,
                        targetNode.targetInstance.hostname,
                        "/local/clientes/$id/transacoes"
                    )
                        .sendJsonObject(requestAsJson)
                        .onSuccess { response ->
                            val statusCode = response.statusCode()
                            val jsonBody = response.bodyAsJsonObject()
                            logger.trace("Received response with status code {}", statusCode)
                            when (statusCode) {
                                200 -> routingContext.response().setStatusCode(statusCode).end(jsonBody!!.encode())
                                else -> when (jsonBody) {
                                    null -> routingContext.response().setStatusCode(statusCode).end()
                                    else -> routingContext.response().setStatusCode(statusCode).end(jsonBody.encode())
                                }
                            }
                        }
                        .onFailure { error ->
                            logger.warn("Something went wrong when routing ${error.message}")
                        }
                }
                if ((id < 1 || id > 5)) {
                    logger.debug("Not found")
                    routingContext.response().setStatusCode(404).end()
                    return@handler
                }
                if (!validateRequest(requestAsJson)) {
                    logger.debug("Invalid request")
                    routingContext.response().setStatusCode(400).end()
                    return@handler
                }
                if (!dickVigaristaMode || "dev" == activeProfile) {
                    logger.trace("Handling it locally id {}:", id)
                    requestHandler.invoke(routingContext)
                    return@handler
                }
                val targetNode = getTarget(id)
                if (targetNode.targetInstance?.hostname == thisNode) {
                    logger.trace("Handling it locally id {}: {}", id, targetNode)
                    requestHandler.invoke(routingContext)
                    return@handler
                }
                logger.trace("Routing it id {}: {}", id, targetNode)
                routeTransaction(targetNode)
            }
    }

    private fun postTransaction(id: Int, command: CustomerAccountCommand): Future<CustomerAccount> {
        val future = Promise.promise<CustomerAccount>()
        val callable = Callable<CustomerAccount> {
            synchronized(SHARED_DATABASE) {
                val state = SHARED_DATABASE[id]!!
                val session = Session(
                    initialState = state,
                    evolveFunction = accountEvolveFn,
                    decideFunction = accountDecideFn,
                )
                session.decide(command)
                SHARED_DATABASE[id] = session.currentState()
                SHARED_DATABASE[id]
            }
        }
        vertx.executeBlocking(/* blockingCodeHandler = */ callable, /* ordered = */ true)
            .onFailure { logger.error(it.message); future.fail(it) }
            .onSuccess { logger.trace("Ok !"); future.complete(it) }
        return future.future()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccountTransactionVerticle::class.java)

        val thisNode = getHostname()

        data class Node(val hostname: String, val port: Int)

        data class TargetNode(val targetInstance: Node?)

        fun getTarget(id: Int): TargetNode {
            fun isEven(number: Int): Boolean {
                return number % 2 == 0
            }
            return when (isEven(id)) {
                true -> TargetNode(Node("api02", 8083))
                false -> TargetNode(Node("api01", 8081))
            }
        }

        fun validateRequest(json: JsonObject): Boolean {
            val tipo = json.getString("tipo")
            val valor = json.getString("valor")
            val descricao = json.getString("descricao")
            fun valorIsIntPositive(): Boolean {
                return try {
                    Integer.parseInt(valor) > 1
                } catch (e: Exception) {
                    false
                }
            }

            return (tipo != null && (tipo == "c" || tipo == "d")
                    && (descricao != null && descricao.length <= 10)
                    && (valor != null && valorIsIntPositive()))
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

        fun mapStateToView(state: CustomerAccount): JsonObject {
            val limit = state.limit
            val balance = state.balance
            return JsonObject().put("total", balance).put("limite", limit)
        }

    }

}