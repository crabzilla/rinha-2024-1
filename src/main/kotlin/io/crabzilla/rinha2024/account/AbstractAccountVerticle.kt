package io.crabzilla.rinha2024.account

import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.vertx.core.AbstractVerticle
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractAccountVerticle : AbstractVerticle() {

    @Inject
    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    var activeProfile: String? = null

    @Inject
    @ConfigProperty(name = "app.dick.vigarista.mode", defaultValue = "true")
    var dickVigaristaMode: Boolean = true

    companion object {

        val SHARED_DATABASE = ConcurrentHashMap<Int, CustomerAccount>()

        init {
            SHARED_DATABASE[1] = CustomerAccount(id = 1, limit = 100000, balance = 0)
            SHARED_DATABASE[2] = CustomerAccount(id = 2, limit = 80000, balance = 0)
            SHARED_DATABASE[3] = CustomerAccount(id = 3, limit = 1000000, balance = 0)
            SHARED_DATABASE[4] = CustomerAccount(id = 4, limit = 10000000, balance = 0)
            SHARED_DATABASE[5] = CustomerAccount(id = 5, limit = 500000, balance = 0)
        }

        fun getHostname(): String {
            return try {
                InetAddress.getLocalHost().hostName
            } catch (exception: UnknownHostException) {
                "Unable to determine the hostname."
            }
        }
    }

}