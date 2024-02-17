package io.crabzilla.rinha2024

import io.github.crabzilla.context.CrabzillaContext
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.Pool
import java.util.*

class QuarkusContext(
    override val vertx: Vertx,
    override val pgPool: Pool,
    override val uuidFunction: () -> UUID,
    private val pgConfig: JsonObject
) : CrabzillaContext {
    override fun newPgSubscriber(): PgSubscriber {
        return PgSubscriber.subscriber(vertx, CrabzillaContext.toPgConnectionOptions(pgConfig))
    }
}