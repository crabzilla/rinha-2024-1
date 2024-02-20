package io.crabzilla.rinha2024

import io.github.crabzilla.context.CrabzillaContext
import io.vertx.core.Vertx
import io.vertx.mutiny.pgclient.PgPool
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

class CrabzillaConfig {

    @Inject
    private lateinit var pgPool: PgPool

    @Inject
    private lateinit var vertx: Vertx

    @Inject
    private lateinit var pgConfig: QuarkusPgConfig

    @ApplicationScoped
    fun crabzilla(): CrabzillaContext {
        return QuarkusContext(
            vertx = vertx,
            pgPool = pgPool.delegate,
            pgConfig = pgConfig.toCrabzillaJsonObject(),
            // these 2 will mess with native packaging
//          uuidFunction = { UuidCreator.getTimeOrderedEpoch(); }
//          uuidFunction = { Generators.timeBasedEpochGenerator().generate() }
            uuidFunction = { UUID.randomUUID() }
        )
    }

}