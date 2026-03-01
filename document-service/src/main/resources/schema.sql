-- Secure Dokument Service – H2 schema (dev profile)
-- Används när spring.jpa.hibernate.ddl-auto=none

CREATE TABLE IF NOT EXISTS dokument_arenden (
    id              UUID         NOT NULL,
    organisation_id VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    status          VARCHAR(20)  NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT pk_dokument_arenden PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS dokuments (
    id              UUID         NOT NULL,
    organisation_id VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    filename        VARCHAR(255),
    content_type    VARCHAR(255),
    file_size       BIGINT,
    storage_path    VARCHAR(1000),
    classification  VARCHAR(20)  NOT NULL,
    arende_id       UUID,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP,
    CONSTRAINT pk_dokuments PRIMARY KEY (id),
    CONSTRAINT fk_dokuments_arende FOREIGN KEY (arende_id) REFERENCES dokument_arenden (id)
);
