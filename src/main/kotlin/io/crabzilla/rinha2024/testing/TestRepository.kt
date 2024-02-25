package io.crabzilla.rinha2024.testing

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory

@ApplicationScoped
class TestRepository(private val pgPool: Pool) {

    fun cleanDatabase(): Future<Void> {
        return pgPool.preparedQuery(SQL_TRUNCATE_ALL).execute()
            .compose { pgPool.preparedQuery(SQL_UPDATE_SUBSCRIPTIONS).execute() }
            .mapEmpty()
    }

    fun printOverview(): Future<JsonObject> {
        return getStreams()
            .map { JsonObject().put("streams", it) }
            .compose { json ->
                getEvents(0, 1000).map { json.put("events", it) }
            }
            .compose { json ->
                getCommands().map { json.put("commands", it) }
            }
            .compose { json ->
                getSubscriptions().map { json.put("subscriptions", it) }
            }
            .compose { json ->
                getAccounts().map { json.put("accounts-view", it) }
            }
            .onComplete {
                logger.info("-------------------------- Crabzilla state overview")
                logger.info(it.result()?.encodePrettily())
            }
    }

    fun getStreams(): Future<List<JsonObject>> {
        return pgPool.preparedQuery(SQL_STREAMS)
            .execute()
            .map { rowSet ->
                rowSet.map {
                    it.toJson()
                }
            }.map {
                it ?: emptyList()
            }
    }

    fun getEvents(
        afterSequence: Long,
        numberOfRows: Int,
    ): Future<List<JsonObject>> {
        return pgPool.withConnection { client ->
            client.prepare(SQL_SELECT_AFTER_OFFSET)
                .compose { preparedStatement ->
                    preparedStatement.query().execute(Tuple.of(afterSequence, numberOfRows))
                }
                .map { rowSet ->
                    rowSet.map {
                        it.toJson()
                    }
                }.map {
                    it ?: emptyList()
                }
        }
    }

    fun getAccounts(): Future<List<JsonObject>> {
        return pgPool.preparedQuery(SQL_ACCOUNTS)
            .execute()
            .map { rowSet ->
                rowSet.map {
                    it.toJson()
                }
            }.map {
                it ?: emptyList()
            }
    }

    fun getCommands(): Future<List<JsonObject>> {
        return pgPool.preparedQuery(SQL_COMMANDS)
            .execute()
            .map { rowSet ->
                rowSet.map {
                    it.toJson()
                }
            }.map {
                it ?: emptyList()
            }
    }

    fun getSubscriptions(): Future<List<JsonObject>> {
        return pgPool.preparedQuery(SQL_SUBSCRIPTIONS)
            .execute()
            .map { rowSet ->
                rowSet.map {
                    it.toJson()
                }
            }.map {
                it ?: emptyList()
            }
    }

    fun getSubscription(name: String): Future<Long> {
        return pgPool.preparedQuery(SQL_SUBSCRIPTION)
            .execute(Tuple.of(name))
            .map { rowSet: RowSet<Row> ->
                rowSet.first().getLong(0)
            }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TestRepository::class.java)

        private const val SQL_TRUNCATE_ALL = "TRUNCATE streams, events, commands, accounts_view RESTART IDENTITY"

        private const val SQL_UPDATE_SUBSCRIPTIONS = "UPDATE subscriptions SET sequence = 0"

        private const val SQL_STREAMS = "SELECT * FROM streams"

        private const val SQL_ACCOUNTS = "SELECT * FROM accounts_view"

        private const val SQL_COMMANDS = "SELECT * FROM commands"

        private const val SQL_SUBSCRIPTIONS = "SELECT * FROM subscriptions ORDER BY name"

        private const val SQL_SELECT_AFTER_OFFSET =
            """
      SELECT *
      FROM events
      WHERE sequence > $1
      ORDER BY sequence
      limit $2
    """

        private const val SQL_SUBSCRIPTION = "SELECT sequence FROM subscriptions WHERE name = $1"

    }
}
