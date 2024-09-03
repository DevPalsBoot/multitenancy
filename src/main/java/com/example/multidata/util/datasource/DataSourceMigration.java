package com.example.multidata.util.datasource;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataSourceMigration {

    /**
     * 모든 데이터 베이스에 마이그레이션
     *
     * @param dataSourceList
     */
    public void migrateAllTenants(List<Object> dataSourceList) {
        for (Object dataSource : dataSourceList) {
            Flyway flyway = Flyway.configure()
                    .dataSource((HikariDataSource) dataSource)
                    .locations("db/migration")
                    .load();

            log.info("Migration: start migration - " + ((HikariDataSource) dataSource).getJdbcUrl());
            flyway.migrate();
            log.info("Migration: end migration - " + ((HikariDataSource) dataSource).getJdbcUrl());

        }
    }

    /**
     * 주어진 데이터 베이스에 마이그레이션
     *
     * @param dataSource
     */
    public void migrateTenant(HikariDataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource((HikariDataSource) dataSource)
                .locations("db/migration")
                .load();
        log.info("Migration: start migration - " + dataSource.getJdbcUrl());
        flyway.migrate();
        log.info("Migration: end migration - " + dataSource.getJdbcUrl());

    }
}
