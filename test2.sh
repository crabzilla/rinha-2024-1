for i in {1..1000}
do
  RANDOM_ID=$((1 + RANDOM % 5))

  echo '{
      "valor": 1000,
      "tipo": "c",
      "descricao": "descricao"
  }' | http POST http://localhost:8081/clientes/$RANDOM_ID/transacoes
done