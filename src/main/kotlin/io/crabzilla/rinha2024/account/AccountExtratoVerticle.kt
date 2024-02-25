package io.crabzilla.rinha2024.account

import io.crabzilla.rinha2024.AbstractCrabzillaVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@ApplicationScoped
class AccountExtratoVerticle : AbstractCrabzillaVerticle() {

    @ConfigProperty(name = "app.extrato.http.port")
    private lateinit var httpPort: String

    override fun start(startPromise: Promise<Void>) {

        logger.info("Starting")

        super.start(startPromise)

        val router = Router.router(vertx)

        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(httpPort.toInt()) { http ->
                if (http.succeeded()) {
                    startPromise.complete()
                    logger.info("HTTP server started on port $httpPort")
                } else {
                    startPromise.fail(http.cause())
                }
            }

        router.get("/clientes/:id/extrato")
            .handler {
                val id = it.request().getParam("id").toInt()
                if ((id < 1 || id > 5)) {
                    it.fail(404)
                } else {
                    getExtrato(id, crabzillaContext.pgPool)
                        .onSuccess { json ->
                            if (json == null) {
                                it.fail(404)
                                return@onSuccess
                            }
                            val saldo = json.getJsonObject("saldo")
                            saldo.put("data_extrato", LocalDateTime.now())
                            it.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .end(json.encode())
                        }
                        .onFailure { error ->
                            logger.error("When getting extrato: {}", error.message)
                            it.fail(error)
                        }
                }
            }

    }

    private fun getExtrato(id: Int, pgPool: Pool): Future<JsonObject?> {
         return pgPool
            .preparedQuery(SQL_SELECT)
            .execute(Tuple.of(id))
            .map {
                if (it.rowCount() == 1) {
                    it.first().getJsonObject("view_model")
                } else {
                    null
                }
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccountExtratoVerticle::class.java)
        private const val SQL_SELECT = "SELECT view_model FROM accounts_view WHERE id = $1"
    }

}