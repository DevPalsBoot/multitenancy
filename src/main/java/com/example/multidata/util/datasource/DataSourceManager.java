package com.example.multidata.util.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import com.example.multidata.entity.DataSourceInfo;
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

    private final Map<Object, Object> dataSourceMap = new ConcurrentHashMap<>(); // tenantId - dataSourceInfo Map

    private AbstractRoutingDataSource routingDataSource;
    private final DataSourceMigration dataSourceMigration;

    /**
     * 데이터소스 라우팅 초기화
     */
    public DataSource createMultiDataSource() {
        HikariDataSource defaultDataSource = loadDefaultDataSource();
        routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.afterPropertiesSet();
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


//    /**
//     * 새로운 데이터 소스를 DataSource Map에 추가
//     *
//     * @param 추가할 데이터 소스의 테넌트 아이디
//     */
//    public void addDataSource(String tenantId) {
//        if (!dataSourceMap.containsKey(tenantId)) {
//            DataSource defaultDataSource = routingDataSource.getResolvedDefaultDataSource();
//            HikariDataSource newDataSource = null;
//
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(defaultDataSource));
//            String sql = "SELECT * FROM datasource_info WHERE tenant_id = ?";
//            DataSourceInfo dataSourceInfo = jdbcTemplate.queryForObject(
//                    sql,
//                    new Object[]{tenantId},
//                    (rs, rowNum) -> DataSourceInfo.builder()
//                            .url(rs.getString("url"))
//                            .driver(rs.getString("driver"))
//                            .build()
//            );
//            if (dataSourceInfo == null) {
//                log.error("Not Found: " + tenantId + " info is not found");
//                return;
//            }
//            newDataSource = createDataSource(dataSourceInfo);
//            dataSourceMap.put(tenantId, newDataSource);
//
//            if (!isTenantExists(tenantId)) {
//                createTenant(tenantId, (HikariDataSource) dataSourceMap.get(tenantId));
//            }
//
//            try (Connection c = newDataSource.getConnection()) {
//                dataSourceMap.put(tenantId, newDataSource);
//                routingDataSource.afterPropertiesSet();
//                log.debug("Added DataSource: " + newDataSource.getJdbcUrl());
//            } catch (SQLException e) {
//                log.error("Error adding DataSource: " + e.getMessage(), e);
//                throw new IllegalArgumentException("Invalid connection information.", e);
//            }
//        }
//    }


//    public void setDataSourcePool() {
//        DataSource defaultDataSource = routingDataSource.getResolvedDefaultDataSource();
//
//        // Data source 정보로 Map 초기화
//        JdbcTemplate jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(defaultDataSource));
//        String sql = "SELECT * FROM datasource_info";
//        jdbcTemplate.query(
//                sql,
//                (rs, rowNum) -> {
//                    DataSourceInfo dataSourceInfo = DataSourceInfo.builder()
//                            .url(rs.getString("url"))
//                            .driver(rs.getString("driver"))
//                            .build();
//
//                    HikariDataSource dataSource = createDataSource(dataSourceInfo);
//                    dataSourceMap.put(rs.getString("tenant_id"), dataSource);
//
//                    return dataSource;
//                }
//        );
//
//        // tenant db 생성
//        for (Object key : dataSourceMap.keySet()) {
//            String tenantId = (String) key;
//            if (!isTenantExists(tenantId)) {
//                createTenant(tenantId);
//            }
//        }
//    }

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
        log.debug("Set Default DataSource: " + defaultDataSource.getJdbcUrl());

        return defaultDataSource;
    }

    private String changeDatabaseName(String jdbcUrl, String newDatabaseName) {
        int lastSlashIndex = jdbcUrl.lastIndexOf('/');
        if (lastSlashIndex == -1 || lastSlashIndex == jdbcUrl.length() - 1) {
            throw new IllegalArgumentException("Invalid JDBC URL format: " + jdbcUrl);
        }

        return jdbcUrl.substring(0, lastSlashIndex + 1) + newDatabaseName;
    }
}
