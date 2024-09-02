DROP TABLE IF EXISTS datasource_info;

CREATE TABLE datasource_info
(
    datasource_map_id BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(255) UNIQUE NOT NULL,
    company           VARCHAR(255),
    url               VARCHAR(255),
    username          VARCHAR(255),
    password          VARCHAR(255),
    driver            VARCHAR(255)
);

INSERT INTO datasource_info (tenant_id, company, url, username, password, driver)
VALUES ('tenant1', 'Company A', 'jdbc:postgresql://localhost:5432/tenant1', 'postgres', '1111',
        'org.postgresql.Driver'),
       ('tenant2', 'Company B', 'jdbc:postgresql://localhost:5432/tenant2', 'postgres', '2222',
        'org.postgresql.Driver');