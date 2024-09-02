package com.example.multidata.util.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 현재 설정된 컨텍스트로 데이터소스 라우팅
 * 데이터 소스가 필요한 시점에 determineCurrentLookupKey 메소드 호출
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getRoutingKey();
    }
}
