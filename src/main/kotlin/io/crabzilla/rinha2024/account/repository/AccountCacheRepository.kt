package io.crabzilla.rinha2024.account.repository

import io.crabzilla.rinha2024.account.AccountRepository
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject

class AccountCacheRepository : AccountRepository {
    override fun getAccount(id: Int): Uni<JsonObject> {
        TODO("Not yet implemented")
    }
}