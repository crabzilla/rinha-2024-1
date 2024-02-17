package io.crabzilla.rinha2024

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.context.CrabzillaContext
import io.vertx.core.Vertx
import io.vertx.mutiny.pgclient.PgPool
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

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
            uuidFunction = { UuidCreator.getTimeOrderedEpoch(); }
        )
   }

}