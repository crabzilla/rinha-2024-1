package io.crabzilla.rinha2024.testing


import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.UniHelper
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Path("/")
class TestController {

    @Inject
    private lateinit var testRepository: TestRepository

    @Path("/dump")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun dump(): Uni<JsonObject> {
        return UniHelper.toUni(testRepository.printOverview())
    }

//    @Inject
//    private lateinit var writerApi: CommandHandler<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent>

//    @Path("/init")
//    @GET
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    fun init(): Future<Void> {
//        // Não precisa mais já que os eventos foram inseridos no Postgres
//        fun initializeDatabase(): Future<Void> {
//            val clients = listOf(
//                Pair(1, 100000),
//                Pair(2, 80000),
//                Pair(3, 1000000),
//                Pair(4, 10000000),
//                Pair(5, 500000),
//            )
//            val targetStream1 = TargetStream(name = "Accounts@${clients[0].first}")
//            val command1 = RegisterNewAccount(id = clients[0].first, limit = clients[0].second, balance = 0)
//            return writerApi.handle(targetStream1, command1)
//                .compose {
//                    val targetStream = TargetStream(name = "Accounts@${clients[1].first}")
//                    val command = RegisterNewAccount(id = clients[1].first, limit = clients[1].second, balance = 0)
//                    writerApi.handle(targetStream, command)
//                }
//                .compose {
//                    val targetStream = TargetStream(name = "Accounts@${clients[2].first}")
//                    val command = RegisterNewAccount(id = clients[2].first, limit = clients[2].second, balance = 0)
//                    writerApi.handle(targetStream, command)
//                }
//                .compose {
//                    val targetStream = TargetStream(name = "Accounts@${clients[3].first}")
//                    val command = RegisterNewAccount(id = clients[3].first, limit = clients[3].second, balance = 0)
//                    writerApi.handle(targetStream, command)
//                }
//                .compose {
//                    val targetStream = TargetStream(name = "Accounts@${clients[4].first}")
//                    val command = RegisterNewAccount(id = clients[4].first, limit = clients[4].second, balance = 0)
//                    writerApi.handle(targetStream, command)
//                }.mapEmpty()
//        }
//        return initializeDatabase()
//                .compose { testRepository.cleanDatabase() }
//                .compose { initializeDatabase() }
//                .onSuccess { testRepository.printOverview() }
//    }

}