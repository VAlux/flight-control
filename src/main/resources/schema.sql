CREATE TABLE IF NOT EXISTS flight (
  id             VARCHAR(36)  NOT NULL,
  flight_number  VARCHAR(6)   NOT NULL,
  airline        VARCHAR(100) NOT NULL,
  origin         CHAR(3)      NOT NULL,
  destination    CHAR(3)      NOT NULL,
  departure_time TIMESTAMP    NOT NULL,
  arrival_time   TIMESTAMP    NOT NULL,
  status         VARCHAR(20)  NOT NULL,
  created_at     TIMESTAMP    NOT NULL,
  updated_at     TIMESTAMP    NOT NULL,
  CONSTRAINT pk_flight         PRIMARY KEY (id),
  CONSTRAINT uq_flight_number  UNIQUE (flight_number)
);
