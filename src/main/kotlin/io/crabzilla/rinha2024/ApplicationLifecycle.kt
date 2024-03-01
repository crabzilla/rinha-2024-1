package io.crabzilla.rinha2024

import io.crabzilla.rinha2024.account.AccountExtratoVerticle
import io.crabzilla.rinha2024.account.AccountTransactionVerticle
import io.quarkus.runtime.StartupEvent
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.slf4j.LoggerFactory

@ApplicationScoped
class ApplicationLifecycle {

    @Inject
    private lateinit var vertx: Vertx

    @Inject
    private lateinit var accountTransactionVerticle: AccountTransactionVerticle

    @Inject
    private lateinit var accountExtratoVerticle: AccountExtratoVerticle

    fun onStart(@Observes ev: StartupEvent) {
        logger.info("Starting verticles")
        vertx.deployVerticle(accountExtratoVerticle)
            .compose { vertx.deployVerticle(accountTransactionVerticle) }
            .onSuccess { logger.info("Successfully deployed verticles") }
            .onFailure { logger.error("Error when deploying verticles", it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationLifecycle::class.java)
    }
}