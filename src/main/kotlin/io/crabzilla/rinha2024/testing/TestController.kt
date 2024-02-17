package io.crabzilla.rinha2024.testing


import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.RegisterNewAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.github.crabzilla.stream.TargetStream
import io.github.crabzilla.writer.WriterApi
import io.vertx.core.Future
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Path("/")
class TestController {

    @Inject
    private lateinit var writerApi: WriterApi<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent>

    @Inject
    private lateinit var testRepository: TestRepository

    @Path("/init")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun init(): Future<Void> {
        return doSomething()
                .compose { testRepository.cleanDatabase() }
                .compose { doSomething() }
                .onSuccess { testRepository.printOverview() }
    }

    @Path("/dump")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun dump(): Future<Void> {
        return  testRepository.printOverview().mapEmpty()
    }

    fun doSomething(): Future<Void> {
        val clients = listOf(
            Pair(1, 100000),
            Pair(2, 80000),
            Pair(3, 1000000),
            Pair(4, 10000000),
            Pair(5, 500000),
        )
        val targetStream = TargetStream(name = "Accounts@${clients[0].first}")
        val command = RegisterNewAccount(id = clients[0].first, limit = clients[0].second, balance = 0)
        return writerApi.handle(targetStream, command)
            .compose {
                val targetStream = TargetStream(name = "Accounts@${clients[1].first}")
                val command = RegisterNewAccount(id = clients[1].first, limit = clients[1].second, balance = 0)
                writerApi.handle(targetStream, command)
            }
            .compose {
                val targetStream = TargetStream(name = "Accounts@${clients[2].first}")
                val command = RegisterNewAccount(id = clients[2].first, limit = clients[2].second, balance = 0)
                writerApi.handle(targetStream, command)
            }
            .compose {
                val targetStream = TargetStream(name = "Accounts@${clients[3].first}")
                val command = RegisterNewAccount(id = clients[3].first, limit = clients[3].second, balance = 0)
                writerApi.handle(targetStream, command)
            }
            .compose {
                val targetStream = TargetStream(name = "Accounts@${clients[4].first}")
                val command = RegisterNewAccount(id = clients[4].first, limit = clients[4].second, balance = 0)
                writerApi.handle(targetStream, command)
            }.mapEmpty()
      }

}