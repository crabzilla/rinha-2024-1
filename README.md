# rinha-2024-q1

* Using Quarkus
* Using Crabzilla (so far, published only to jitpack)
* Using Event Sourcing (not so competitive in terms of performance)

# How to run

* `docker-compose up`             # to start postgres
* `mvn compile quarkus:dev`       # to start app
* http://localhost:8080/q/swagger-ui/


echo '{
"valor": 1000,
"tipo" : "c",
"descricao" : "descricao"
}' | http POST http://localhost:8080/clientes//1/transacoes



https://github.com/eclipse-vertx/vertx-sql-client/issues/796

https://stackoverflow.com/questions/71709685/vert-x-rare-unhandled-exception-in-router-despite-errorhandler-with-statuscod

