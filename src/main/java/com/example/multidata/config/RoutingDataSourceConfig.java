package com.example.multidata.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.example.multidata.util.datasource.DataSourceManager;

@Configuration
@EnableTransactionManagement
public class RoutingDataSourceConfig {
    @Bean
    public DataSource routingDataSource(DataSourceManager dataSourceManager) {
        return dataSourceManager.createMultiDataSource();
    }
}
