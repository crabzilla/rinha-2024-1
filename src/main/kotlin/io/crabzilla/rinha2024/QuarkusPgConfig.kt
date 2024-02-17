package io.crabzilla.rinha2024

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import io.vertx.core.json.JsonObject

@ConfigMapping(prefix = "quarkus.datasource")
interface QuarkusPgConfig {
  fun dbKind(): String
  fun username(): String
  fun password(): String
  @WithName("reactive.url")
  fun url(): String
  fun toCrabzillaJsonObject() : JsonObject {
    return JsonObject()
      .put("username", username())
      .put("password", password())
      .put("uri", url())
  }
}
