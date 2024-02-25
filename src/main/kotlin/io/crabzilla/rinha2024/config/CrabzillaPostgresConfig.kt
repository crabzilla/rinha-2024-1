package io.crabzilla.rinha2024.config

import io.smallrye.config.ConfigMapping
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions
import java.net.URI

@ConfigMapping(prefix = "crabzilla.datasource")
interface CrabzillaPostgresConfig {
    fun url(): String
    fun username(): String
    fun password(): String
    fun maxSize(): Int

    fun toPgConnectOptions(): PgConnectOptions {
        val uri = URI(url())
        val host = uri.host
        val port = uri.port
        val database = uri.path.removePrefix("/")

        return PgConnectOptions()
            .setPort(port)
            .setHost(host)
            .setDatabase(database)
            .setUser(username())
            .setPassword(password())
    }

    fun toPoolOptions(): PoolOptions {
        return PoolOptions().setMaxSize(maxSize()).setName("crabzilla")
    }

    fun toJsonObject(): JsonObject {
        return JsonObject()
            .put("url", url())
            .put("username", username())
            .put("password", password())
            .put("maxSize", maxSize())
    }

}