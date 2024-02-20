package io.crabzilla.rinha2024.account.repository

import io.crabzilla.rinha2024.account.AccountRepository
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.UniHelper
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class AccountPgRepository : AccountRepository {

    @Inject
    private lateinit var pgPool: Pool

    override fun getAccount(id: Int): Uni<JsonObject> {
        val databaseResultFuture = pgPool
            .preparedQuery(SQL_SELECT)
            .execute(Tuple.of(id))
            .map {
                if (it.rowCount() == 1) {
                    it.first().getJsonObject("view_model")
                } else throw NotFoundException()
            }
        return UniHelper.toUni(databaseResultFuture)
    }

    companion object {
        private const val SQL_SELECT = "SELECT view_model FROM accounts_view WHERE id = $1"
    }

}