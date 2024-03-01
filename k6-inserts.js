import http from "k6/http";

export const options = {
    discardResponseBodies: true,
    scenarios: {
        accounts: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                {duration: '10s', target: 300},
                {duration: '30s', target: 10000},
                {duration: '10s', target: 10},
            ],
            gracefulRampDown: '1s',
        },
    },
};

export default function () {
    let randomClientId = 1; // Math.floor(Math.random() * 5) + 1;
    let payload = JSON.stringify({
        valor: 1,
        tipo: 'c',
        descricao: 'descricao',
    });
    let headers = {'Content-Type': 'application/json'};

    let responses = http.batch([
        ["POST", `http://localhost:8081/clientes/${randomClientId}/transacoes`, payload, {headers: headers}],
        // Add more requests if required
    ]);

    // check(responses[0], {
    //     'response code was 200': (resp) => resp.status === 200,
    // });
}