\connect crabzilla ;

INSERT INTO streams (name, state_type, state_id, created_at, status, migrated_to_stream_id)
VALUES ('Accounts@1', 'Accounts', '1', '2024-02-17T05:11:32.179299Z', 'OPEN', NULL),
       ('Accounts@2', 'Accounts', '2', '2024-02-17T05:11:32.201428Z', 'OPEN', NULL),
       ('Accounts@3', 'Accounts', '3', '2024-02-17T05:11:32.221489Z', 'OPEN', NULL),
       ('Accounts@4', 'Accounts', '4', '2024-02-17T05:11:32.236764Z', 'OPEN', NULL),
       ('Accounts@5', 'Accounts', '5', '2024-02-17T05:11:32.249280Z', 'OPEN', NULL);

INSERT INTO events (id, event_type, event_payload, stream_id, version, causation_id, correlation_id, created_at)
VALUES ('018db57a-1061-7d65-bf70-5a66614fd2ed', 'CustomerAccountRegistered',
        '{"type": "CustomerAccountRegistered", "id": 1, "limit": 100000, "balance": 0, "date": "2024-02-17T02:11:32.193346"}',
        1, 1, '018db57a-1061-7d65-bf70-5a66614fd2ed', '018db57a-1061-7d65-bf70-5a66614fd2ed',
        '2024-02-17T05:11:32.179299Z'),
       ('018db57a-1076-76b4-aa5f-8da8f0a54335', 'CustomerAccountRegistered',
        '{"type": "CustomerAccountRegistered", "id": 2, "limit": 80000, "balance": 0, "date": "2024-02-17T02:11:32.214755"}',
        2, 1, '018db57a-1076-76b4-aa5f-8da8f0a54335', '018db57a-1076-76b4-aa5f-8da8f0a54335',
        '2024-02-17T05:11:32.201428Z'),
       ('018db57a-108a-7c60-af89-f6cc308ccaaf', 'CustomerAccountRegistered',
        '{"type": "CustomerAccountRegistered", "id": 3, "limit": 1000000, "balance": 0, "date": "2024-02-17T02:11:32.234625"}',
        3, 1, '018db57a-108a-7c60-af89-f6cc308ccaaf', '018db57a-108a-7c60-af89-f6cc308ccaaf',
        '2024-02-17T05:11:32.221489Z'),
       ('018db57a-1098-7d29-a595-2b545f162177', 'CustomerAccountRegistered',
        '{"type": "CustomerAccountRegistered", "id": 4, "limit": 10000000, "balance": 0, "date": "2024-02-17T02:11:32.24872"}',
        4, 1, '018db57a-1098-7d29-a595-2b545f162177', '018db57a-1098-7d29-a595-2b545f162177',
        '2024-02-17T05:11:32.236764Z'),
       ('018db57a-10a5-7dbe-855b-707e6c470eaf', 'CustomerAccountRegistered',
        '{"type": "CustomerAccountRegistered", "id": 5, "limit": 500000, "balance": 0, "date": "2024-02-17T02:11:32.26089"}',
        5, 1, '018db57a-10a5-7dbe-855b-707e6c470eaf', '018db57a-10a5-7dbe-855b-707e6c470eaf',
        '2024-02-17T05:11:32.249280Z');

CREATE UNLOGGED TABLE accounts_view
(
    id int2         NOT NULL UNIQUE,
    view_model JSON NOT NULL
);

INSERT INTO accounts_view (id, view_model)
VALUES (1, '{
  "saldo": {
    "limite": 100000,
    "total": 0
  },
  "ultimas_transacoes": []
}'),
       (2, '{
         "saldo": {
           "limite": 80000,
           "total": 0
         },
         "ultimas_transacoes": []
       }'),
       (3, '{
         "saldo": {
           "limite": 1000000,
           "total": 0
         },
         "ultimas_transacoes": []
       }'),
       (4, '{
         "saldo": {
           "limite": 10000000,
           "total": 0
         },
         "ultimas_transacoes": []
       }'),
       (5, '{
         "saldo": {
           "limite": 500000,
           "total": 0
         },
         "ultimas_transacoes": []
       }');
