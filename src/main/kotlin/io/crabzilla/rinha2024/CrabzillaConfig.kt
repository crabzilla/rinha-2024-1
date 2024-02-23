package io.crabzilla.rinha2024

import io.crabzilla.rinha2024.testing.TestRepository
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.stream.StreamWriterLockEnum
import io.quarkus.runtime.Startup
import io.quarkus.runtime.configuration.ProfileManager
import io.vertx.core.Vertx
import io.vertx.mutiny.pgclient.PgPool
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.util.*

class CrabzillaConfig {

    @ConfigProperty(name = "quarkus.profile")
    lateinit var profile: String

    @Inject
    private lateinit var pgPool: PgPool

    @Inject
    private lateinit var vertx: Vertx

    @Inject
    private lateinit var pgConfig: QuarkusPgConfig

    @Inject
    private lateinit var testRepository: TestRepository

    @Startup
    @ApplicationScoped
    fun crabzilla(): CrabzillaContext {
        logger.info("Crabzilla context will be created. Quarkus profile: {}", profile)
        if ("dev" == profile) {
            logger.info("Postgres config: {}", pgConfig.toCrabzillaJsonObject().encodePrettily())
        }
        val context = QuarkusContext(
            vertx = vertx,
            pgPool = pgPool.delegate,
            pgConfig = pgConfig.toCrabzillaJsonObject(),
            // these 2 will mess with native packaging
//          uuidFunction = { UuidCreator.getTimeOrderedEpoch(); }
//          uuidFunction = { Generators.timeBasedEpochGenerator().generate() }
            uuidFunction = { UUID.randomUUID() }
        )
        context.pgPool.preparedQuery("SELECT 1").execute()
            .onSuccess { logger.info("Crabzilla context created") }
            .onFailure { logger.error("When creating Crabzilla context", it) }
            .andThen {
                if ("dev" == profile) {
                    vertx.setTimer(5_000) {
                        testRepository.printOverview()
                    }
                }
            }
        return context
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CrabzillaConfig::class.java)
    }

}