package com.example.multidata.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.multidata.domain.UserInsert;
import com.example.multidata.util.datasource.DataSourceContextHolder;
import com.example.multidata.util.datasource.DataSourceManager;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DataSourceManager dataSourceManager;
    private final UserService userService;

    public void createDbTenant(String tenantId) {
        // 새로운 디비 테넌트 생성
        dataSourceManager.addDataSource(tenantId);
        // 생성한 디비에 migration 수행
        dataSourceManager.migrateDataSource(tenantId);
    }

    public void insertUsers(String tenantId, List<UserInsert> users) {
        DataSourceContextHolder.setRoutingKey(tenantId);
        for (UserInsert user : users) {
            userService.saveUserTenantId(user.getEmail(), tenantId);
            userService.saveUser(user);
        }
    }
}
