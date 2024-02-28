//package io.crabzilla.rinha2024
//
//import io.vertx.core.Future
//import io.vertx.redis.client.Command
//import io.vertx.redis.client.Request
//import io.vertx.redis.client.RedisAPI
//import io.vertx.redis.client.Response
//import jakarta.enterprise.context.ApplicationScoped
//import java.util.*
//
//@ApplicationScoped
//class RedisLock(private val redis: RedisAPI) {
//
//    fun acquireLock(key: String, requester: String, timeout: Int = 500): Future<Boolean> {
////        val setCommand = Request.cmd(Command.SET)
////            .arg(key)
////            .arg(requester)
////            .arg("NX")
////            .arg("PX")
////            .arg(timeout.toString())
//        return redis.send(Command.SET, key, requester, "NX", "PX", timeout.toString())
//            .map { response: Response ->
//              when (response.toString()) {
//                "OK" -> true
//                else -> false
//              }
//            }
//    }
//
//    fun releaseLock(key: String, requester: String): Future<Boolean> {
////        val script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
////        val evalCommand = Request.cmd(Command.EVAL)
////            .arg(script)
////            .arg("1")
////            .arg(key)
////            .arg(requester)
//        val script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
//        return redis.send(Command.EVAL, script, "1", key, requester
//        ).map { response ->
//            when (response.toString()) {
//                "OK" -> true
//                else -> false
//            }
//        }
//    }
//}