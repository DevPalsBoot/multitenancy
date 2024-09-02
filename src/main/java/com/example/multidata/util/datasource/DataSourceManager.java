package com.example.multidata.util.datasource;

import java.sql.Connection;
import java.sql.SQLException;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터소스 관리
 */

@Slf4j
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
     * @param 추가할 데이터 소스의 테넌트 아이디
     */
    public void addDataSource(String tenantId) {
        if (!dataSourceMap.containsKey(tenantId)) {
            DataSource defaultDataSource = routingDataSource.getResolvedDefaultDataSource();
            HikariDataSource newDataSource = null;

            JdbcTemplate jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(defaultDataSource));
            String sql = "SELECT * FROM datasource_info WHERE tenant_id = ?";
            DataSourceInfo dataSourceInfo = jdbcTemplate.queryForObject(
                    sql,
                    new Object[]{tenantId},
                    (rs, rowNum) -> DataSourceInfo.builder()
                            .url(rs.getString("url"))
                            .userName(rs.getString("username"))
                            .password(rs.getString("password"))
                            .driver(rs.getString("driver"))
                            .build()
            );
            if (dataSourceInfo == null) {
                log.error("Not Found: " + tenantId + " info is not found");
                return;
            }
            newDataSource = createDataSource(dataSourceInfo);
            dataSourceMap.put(tenantId, newDataSource);
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

    private void setDataSourcePool() {
        DataSource defaultDataSource = routingDataSource.getResolvedDefaultDataSource();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(defaultDataSource));
        String sql = "SELECT * FROM datasouce_info";
        jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    DataSourceInfo dataSourceInfo = DataSourceInfo.builder()
                            .url(rs.getString("url"))
                            .userName(rs.getString("username"))
                            .password(rs.getString("password"))
                            .driver(rs.getString("driver"))
                            .build();

                    HikariDataSource dataSource = createDataSource(dataSourceInfo);
                    dataSourceMap.put(rs.getString("tenant_id"), dataSource);

                    return dataSource;
                }
        );
    }

    /**
     * 데이터 소스 생성
     *
     * @param dataSourceInfo
     * @return
     */
    private HikariDataSource createDataSource(DataSourceInfo dataSourceInfo) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dataSourceInfo.getUrl());
        dataSource.setUsername(dataSourceInfo.getUserName());
        dataSource.setPassword(dataSourceInfo.getPassword());
        dataSource.setDriverClassName(dataSourceInfo.getDriver());
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
}
