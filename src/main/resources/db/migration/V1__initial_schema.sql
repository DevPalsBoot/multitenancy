DROP TABLE IF EXISTS sc_user;

CREATE TABLE sc_user
(
    id        SERIAL PRIMARY KEY,
    user_name VARCHAR(32)  NOT NULL,
    email     VARCHAR(128) NOT NULL UNIQUE,
    pwd       VARCHAR(128) NOT NULL
);