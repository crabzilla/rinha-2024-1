CREATE DATABASE crabzilla OWNER quarkus;

\connect crabzilla;

CREATE TYPE stream_status AS ENUM ('OPEN', 'CLOSED', 'MIGRATED');

CREATE UNLOGGED TABLE streams (
                                  id INT GENERATED ALWAYS AS IDENTITY (cache 1) UNIQUE,
                                  name VARCHAR(100) NOT NULL UNIQUE,
                                  state_type VARCHAR(100) NOT NULL,
                                  state_id VARCHAR(100) NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                                  status stream_status NOT NULL DEFAULT 'OPEN',
                                  migrated_to_stream_id INT NULL REFERENCES streams (id)
);

CREATE INDEX state_id ON streams USING HASH (state_id);
CREATE INDEX state_type ON streams USING HASH (state_type);

CREATE UNLOGGED TABLE events (
                                 id UUID NOT NULL UNIQUE, --
                                 sequence BIGINT GENERATED ALWAYS AS IDENTITY (cache 1000) UNIQUE,
                                 event_type VARCHAR(100) NOT NULL,
                                 event_payload JSON NOT NULL,
                                 stream_id INT NOT NULL REFERENCES streams (id),
                                 version INT NOT NULL,
                                 causation_id UUID NOT NULL REFERENCES events (id),
                                 correlation_id UUID NOT NULL REFERENCES events (id),
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                                 UNIQUE (stream_id, version)
)
;

CREATE INDEX stream_id ON events USING HASH (stream_id);

-- not used for this example

CREATE UNLOGGED TABLE commands (
                                   command_id UUID NOT NULL UNIQUE,
                                   stream_id INT NOT NULL REFERENCES streams (id),
                                   causation_id UUID NULL REFERENCES events (id),
                                   correlation_id UUID NULL REFERENCES events (id),
                                   command_payload JSON NULL,
                                   command_metadata JSON NULL,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE UNLOGGED TABLE subscriptions (
                                        name VARCHAR(100) PRIMARY KEY NOT NULL,
                                        sequence BIGINT
);