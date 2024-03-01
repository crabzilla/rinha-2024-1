# rinha-2024-q1

Eu comecei este repo com a intenção de submeter uma solução usando o Crabzilla, meu projeto pet.
O que eu esperava era ao menos passar nos testes de consistência. Quando rodei o primeiro teste,
notei que iria dar trabalho, rs. Tentei vários mecanismos de locks, etc

TODO : contar a estoria toda

Resumindo: ainda não desisti, mas por resolvi testar só com o crabzilla-core, que tem só uma classe e nenhuma dependência.

E trapaceei de 2 formas:

1. O NGNX continua fazendo roud robin mas quando a app recebe a requisição, ela delega contas pares para app02 e ímpares para app01 via HTTP. 
2. A app que atende a requisição usa apenas um COncurrentHashMap para armazenar o último estado da conta. TUdo na memória. E a operação de escrita ainda usa um syncronized.

Isso tudo só pra testar a consistência da minha API. Mas mesmo assim, os testes continuam terríveis. 
Nenhuma transação de crédito passa. Isso pode significar que minha API tá bugada. 

Acabei de testar, depois avalio os resultados. 

----

* Using Quarkus
* Using Crabzilla (so far, published only to jitpack)
* Using Event Sourcing (not so competitive in terms of performance)

# How to run 

* `mvn compile quarkus:dev`       # to start app

Then you can use the http tests within http.tests folder. 
There is a k6 stress test: k6-inserts.js. To run it:  `run -v k6-inserts.js`

# How to run Zan test

* `docker-compose up`             # to start NGNX and api01 and api02
* Then run that `executar-teste-local.sh`

WIP
