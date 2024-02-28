package io.crabzilla.rinha2024

import io.crabzilla.rinha2024.config.CrabzillaPostgresConfig
import io.github.crabzilla.context.CrabzillaContextImpl
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.tracing.TracingPolicy
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty

abstract class AbstractCrabzillaVerticle : AbstractVerticle() {

    @Inject
    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    var activeProfile: String? = null

    @Inject
    @ConfigProperty(name = "app.dick.vigarista.mode", defaultValue = "true")
    var dickVigaristaMode: Boolean = true

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