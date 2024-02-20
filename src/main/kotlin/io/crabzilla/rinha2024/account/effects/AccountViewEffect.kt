package io.crabzilla.rinha2024.account.effects

import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.github.crabzilla.command.CommandHandlerResult
import io.github.crabzilla.command.ViewEffect
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.lang.Integer.parseInt

class AccountViewEffect(val toJsonView: (CustomerAccount) -> JsonObject) :
    ViewEffect.GivenAllEventsViewEffect<CustomerAccount, CustomerAccountEvent> {
    override fun handle(
        sqlConnection: SqlConnection,
        result: CommandHandlerResult<CustomerAccount, CustomerAccountEvent>,
    ): Future<JsonObject?> {
        fun upsert(tuple: Tuple): Future<JsonObject?> {
            return sqlConnection
                .preparedQuery(SQL_UPSERT)
                .execute(tuple)
                .map { it.first().getJsonObject("view_model") }
        }

        val id = parseInt(result.metadata.last().stateId)
        return upsert(Tuple.of(id, toJsonView(result.snapshot.state)))
    }

    companion object {
        private const val SQL_UPSERT = """
        INSERT INTO accounts_view (id, view_model) 
        VALUES ($1, $2)  
        ON CONFLICT (id) DO UPDATE SET view_model = $2  
        RETURNING view_model"""
    }
}
