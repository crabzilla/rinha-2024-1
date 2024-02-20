package io.crabzilla.rinha2024.account

import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject

interface AccountRepository {
    fun getAccount(id: Int): Uni<JsonObject>
}