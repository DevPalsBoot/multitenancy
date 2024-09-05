package com.example.multidata.util.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import com.example.multidata.entity.DataSourceInfo;
import com.example.multidata.service.redis.TenantService;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터소스 관리
 */

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class DataSourceManager {

    @Value("${spring.datasource.url}")
    private String defaultUrl;
    @Value("${spring.datasource.username}")
    private String defaultUsername;
    @Value("${spring.datasource.password}")
    private String defaultPwd;
    @Value("${spring.datasource.hikari.driver-class-name}")
    private String defaultDriver;
    @Value("${spring.jpa.properties.hibernate.default_schema:}")
    private String defaultSchema;

    private final Map<Object, Object> dataSourceMap = new ConcurrentHashMap<>(); // tenantId - dataSourceInfo Map

    private AbstractRoutingDataSource routingDataSource;
    private final DataSourceMigration dataSourceMigration;
    private final TenantService tenantService;

    /**
     * 데이터소스 라우팅 초기화
     */
    public DataSource createMultiDataSource() {
        HikariDataSource defaultDataSource = loadDefaultDataSource();
        routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.afterPropertiesSet();
        setDataSourcePool();
        migrateDataSourcePool();
        return routingDataSource;
    }

    public void setCurrent(String tenantId) {
        if (isAbsent(tenantId)) {
            addDataSource(tenantId);
        }
    }

    /**
     * DataSource Map 에 tenant Id로 검색
     *
     * @return 존재 여부
     */
    public boolean isAbsent(String tenantId) {
        return !dataSourceMap.containsKey(tenantId);
    }

    /**
     * 새로운 데이터 소스를 DataSource Map에 추가
     *
     * @param tenantId
     */
    public void addDataSource(String tenantId) {
        if (!dataSourceMap.containsKey(tenantId)) {
            if (!isTenantExists(tenantId)) {
                createTenant(tenantId);
            }
            HikariDataSource newDataSource = createDataSource(changeDatabaseName(defaultUrl, tenantId));
            try (Connection c = newDataSource.getConnection()) {
                dataSourceMap.put(tenantId, newDataSource);
                routingDataSource.afterPropertiesSet();
                log.debug("Added DataSource: " + newDataSource.getJdbcUrl());
            } catch (SQLException e) {
                log.error("Error adding DataSource: " + e.getMessage(), e);
                throw new IllegalArgumentException("Invalid connection information.", e);
            }
        }
    }

    /**
     * 레디스에 저장된 테넌트로
     * 데이터 소스 풀 저장
     */
    public void setDataSourcePool() {
        DataSource defaultDataSource = routingDataSource.getResolvedDefaultDataSource();
        Set<String> tenantIds = tenantService.getAllTenantIds();
        for (String tenantId : tenantIds) {
            HikariDataSource dataSource = createDataSource(changeDatabaseName(defaultUrl, tenantId));
            try (Connection c = dataSource.getConnection()) {
                dataSourceMap.put(tenantId, dataSource);
                routingDataSource.afterPropertiesSet();
                log.debug("Added DataSource: " + dataSource.getJdbcUrl());
            } catch (SQLException e) {
                log.error("Error adding DataSource: " + e.getMessage(), e);
                throw new IllegalArgumentException("Invalid connection information.", e);
            }
        }
    }

    /**
     * 데이터 소스 전체 마이그레이션
     */
    public void migrateDataSourcePool() {
        if (dataSourceMap.isEmpty()) {
            return;
        }

        dataSourceMigration.migrateAllTenants(dataSourceMap.values().stream().toList());
    }

    /**
     * 테넌트 아이디로 데이터 소스 마이그레이션
     *
     * @param tenantId
     */
    public void migrateDataSource(String tenantId) {
        HikariDataSource dataSource = (HikariDataSource) dataSourceMap.get(tenantId);
        if (dataSource == null) {
            log.error("DataSource not found: " + tenantId);
            return;
        }

        dataSourceMigration.migrateTenant(dataSource);
    }


    /**
     * 테넌트 디비 존재 확인
     *
     * @param tenantId
     * @return
     */
    private boolean isTenantExists(String tenantId) {
        HikariDataSource dataSource = (HikariDataSource) dataSourceMap.get(tenantId);
        try (Connection conn = DriverManager.getConnection(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword())) {
            // 데이터베이스가 존재하는 경우
            return true;
        } catch (Exception e) {
            // 데이터베이스가 존재하지 않는 경우
            return false;
        }
    }

    /**
     * 테넌트 디비 생성
     *
     * @param tenantId
     */
    private void createTenant(String tenantId) {
        String createDatabaseSQL = String.format(
                "CREATE DATABASE \"%s\" ENCODING = 'UTF8' TABLESPACE = pg_default CONNECTION LIMIT = -1;",
                tenantId);
        try (Connection conn = routingDataSource.getResolvedDefaultDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createDatabaseSQL);
            System.out.println("Success: Created database " + tenantId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating database " + tenantId, e);
        }
    }

    /**
     * 데이터 소스 생성
     */
    private HikariDataSource createDataSource(String url) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(defaultUsername);
        dataSource.setPassword(defaultPwd);
        dataSource.setDriverClassName(defaultDriver);
        dataSource.setMaximumPoolSize(10);
        dataSource.setConnectionInitSql("SET search_path TO " + defaultSchema);

        return dataSource;
    }

    private HikariDataSource loadDefaultDataSource() {
        HikariDataSource defaultDataSource = new HikariDataSource();

        defaultDataSource.setPoolName("Default");
        defaultDataSource.setJdbcUrl(defaultUrl);
        defaultDataSource.setUsername(defaultUsername);
        defaultDataSource.setPassword(defaultPwd);
        defaultDataSource.setDriverClassName(defaultDriver);
        defaultDataSource.setMaximumPoolSize(10);
        defaultDataSource.setConnectionInitSql("SET search_path TO " + defaultSchema);

        log.debug("Set Default DataSource: " + defaultDataSource.getJdbcUrl());

        return defaultDataSource;
    }

    private String changeDatabaseName(String jdbcUrl, String newDatabaseName) {
        int lastSlashIndex = jdbcUrl.lastIndexOf('/');
        int queryParamIndex = jdbcUrl.indexOf('?');

        if (lastSlashIndex == -1 || (queryParamIndex != -1 && lastSlashIndex > queryParamIndex)) {
            throw new IllegalArgumentException("Invalid JDBC URL format: " + jdbcUrl);
        }

        String baseUrl = jdbcUrl.substring(0, lastSlashIndex + 1);
        String remainingUrl = queryParamIndex == -1 ? "" : jdbcUrl.substring(queryParamIndex);

        return baseUrl + newDatabaseName + remainingUrl;
    }
}
