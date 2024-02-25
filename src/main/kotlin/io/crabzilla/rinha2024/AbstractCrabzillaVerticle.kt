package io.crabzilla.rinha2024

import io.crabzilla.rinha2024.config.CrabzillaPostgresConfig
import io.github.crabzilla.context.CrabzillaContextImpl
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.tracing.TracingPolicy
import jakarta.inject.Inject

abstract class AbstractCrabzillaVerticle : AbstractVerticle() {

    @Inject
    lateinit var crabzillaPostgresConfig: CrabzillaPostgresConfig

    lateinit var crabzillaContext: CrabzillaContextImpl

    override fun start(startPromise: Promise<Void>) {

        val pgConnectOptions = crabzillaPostgresConfig.toPgConnectOptions()
        pgConnectOptions.setTracingPolicy(TracingPolicy.ALWAYS)
        val poolOptions = crabzillaPostgresConfig.toPoolOptions()

        crabzillaContext = CrabzillaContextImpl(vertx = vertx, connectionOptions = pgConnectOptions, poolOptions = poolOptions)

    }
}