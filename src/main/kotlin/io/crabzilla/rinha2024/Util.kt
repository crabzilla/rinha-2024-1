package io.crabzilla.rinha2024

import java.net.InetAddress
import java.net.UnknownHostException

object Util {
    fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (exception: UnknownHostException) {
            "Unable to determine the hostname."
        }
    }
}