package io.crabzilla.rinha2024.account


import io.crabzilla.rinha2024.account.AccountConfig.Companion.MAP_ACCOUNT_TO_JSON_VIEW_FUNCTION
import io.crabzilla.rinha2024.account.model.CustomerAccount
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewDeposit
import io.crabzilla.rinha2024.account.model.CustomerAccountCommand.CommitNewWithdraw
import io.crabzilla.rinha2024.account.model.CustomerAccountEvent
import io.crabzilla.rinha2024.account.model.LimitExceededException
import io.github.crabzilla.command.CommandHandler
import io.github.crabzilla.stream.TargetStream
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.UniHelper
import io.vertx.core.Future.failedFuture
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestPath
import java.time.LocalDateTime


@Path("/")
class AccountController {

    @Inject
    private lateinit var accountPgRepository: AccountPgRepository

    @Inject
    private lateinit var commandHandler: CommandHandler<CustomerAccount, CustomerAccountCommand, CustomerAccountEvent>

    @Path("/clientes/{id}/extrato")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getExtrato(@RestPath("id") id: Int): Uni<JsonObject> {
        if ((id < 0 || id > 5)) {
            throw NotFoundException()
        }
        return accountPgRepository.getAccount(id)
            .map { json ->
                val saldo = json.getJsonObject("saldo")
                saldo.put("data_extrato", LocalDateTime.now())
                json
            }
    }

    @Path("/clientes/{id}/transacoes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun postTransaction(@RestPath("id") id: Int, request: TransactionRequest): Uni<JsonObject> {
        if ((id < 0 || id > 5)) {
            throw NotFoundException()
        }
        if (!request.validate()) {
            throw BadRequestException()
        }
        val targetStream = TargetStream(name = "Accounts@$id")
        val command = mapRequestToCommand(request)
        val viewModelFuture = commandHandler.handle(targetStream, command)
            .map { MAP_ACCOUNT_TO_JSON_VIEW_FUNCTION(it.snapshot.state) }
            .map { json ->
                val saldo = json.getJsonObject("saldo")
                saldo.put("data_extrato", LocalDateTime.now())
                json
            }
            .recover {
                when (val error = it.cause) {
                    is LimitExceededException -> failedFuture(ClientErrorException(error.message, 422))
                    else -> failedFuture(error)
                }
            }
        return UniHelper.toUni(viewModelFuture)
    }

    companion object {

        data class TransactionRequest(val tipo: String, val descricao: String, val valor: Int) {
            fun validate(): Boolean {
                return ((this.tipo == "c" || this.tipo == "d")
                        && (this.descricao.length <= 10)
                        && (this.valor > 0))
            }
        }

        private fun mapRequestToCommand(request: TransactionRequest): CustomerAccountCommand {
            return when (request.tipo) {
                "c" -> CommitNewDeposit(request.valor, request.descricao)
                "d" -> CommitNewWithdraw(request.valor, request.descricao)
                else -> TODO("Never will happen")
            }
        }

    }
}