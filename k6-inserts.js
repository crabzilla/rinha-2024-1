import http from "k6/http";
import {check} from "k6";

export let options = {
    vus: 5,  // number of virtual users
    duration: '30s',  // duration of the test
};
export default function () {
    let randomClientId = Math.floor(Math.random() * 5) + 1;
    let payload = JSON.stringify({
        valor: 1000,
        tipo: 'c',
        descricao: 'descricao',
    });
    let headers = { 'Content-Type': 'application/json' };

    let responses = http.batch([
        ["POST", `http://localhost:8081/clientes/${randomClientId}/transacoes`, payload, { headers: headers }],
        // Add more requests if required
    ]);

    // check(responses[0], {
    //     'response code was 200': (resp) => resp.status === 200,
    // });
}