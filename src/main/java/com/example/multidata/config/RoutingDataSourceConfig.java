package com.example.multidata.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.example.multidata.util.datasource.DataSourceManager;

@Configuration
@EnableTransactionManagement
public class RoutingDataSourceConfig {

    @Value("${spring.jpa.properties.hibernate.default_schema:}")
    private String defaultSchema;

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);

        if (defaultSchema != null && !defaultSchema.isEmpty()) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ByteArrayResource(("SET search_path TO " + defaultSchema + ";").getBytes()));
            initializer.setDatabasePopulator(populator);
        }

        return initializer;
    }

    @Bean
    public DataSource routingDataSource(DataSourceManager dataSourceManager) {
        return dataSourceManager.createMultiDataSource();
    }
}
