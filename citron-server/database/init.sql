CREATE DATABASE IF NOT EXISTS `citron` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `citron`;

CREATE TABLE citron_spam__ignored_hostname
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT  NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    wiki_id    VARCHAR(20)                        NOT NULL,
    hostname   VARCHAR(255)                       NOT NULL,
    CONSTRAINT unique__wiki_id__hostname UNIQUE (wiki_id, hostname)
);

CREATE TABLE citron_spam__reported_hostname
(
    id                 BIGINT PRIMARY KEY NOT NULL,
    created_at         DATETIME           NOT NULL,
    wiki_id            VARCHAR(20)        NOT NULL,
    user               VARCHAR(255)       NOT NULL,
    page               VARCHAR(255)       NOT NULL,
    revision_id        BIGINT             NOT NULL,
    revision_timestamp BIGINT             NOT NULL,
    hostname           VARCHAR(255)       NOT NULL,
    score              DOUBLE             NOT NULL,
    model_number       TINYINT            NOT NULL
);

CREATE INDEX idx__wiki_id__created_at ON citron_spam__reported_hostname (wiki_id, created_at);

CREATE TABLE citron_spam__feedback
(
    id          BIGINT PRIMARY KEY NOT NULL,
    created_at  DATETIME           NOT NULL,
    created_by  BIGINT             NOT NULL,
    wiki_id     VARCHAR(20)        NOT NULL,
    report_date VARCHAR(10)        NOT NULL,
    hostname    VARCHAR(255)       NOT NULL,
    status      TINYINT            NOT NULL,
    hash        VARCHAR(40)        NOT NULL,
    CONSTRAINT unique__hash UNIQUE (hash)
);

CREATE INDEX idx__wiki_id__report_date ON citron_spam__feedback (wiki_id, report_date);
