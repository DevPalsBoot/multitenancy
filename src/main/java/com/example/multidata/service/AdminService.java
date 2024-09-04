package com.example.multidata.service;

import com.example.multidata.domain.UserInsert;
import com.example.multidata.util.datasource.DataSourceContextHolder;
import com.example.multidata.util.datasource.DataSourceManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DataSourceManager dataSourceManager;
    private final UserService userService;
    private final StorageService storageService;

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

    public void createBucket(String tenantId) {
        // bucket 존재 여부 확인
        boolean existBucket = storageService.isExistBucket(tenantId);
        // 없다면 생성
        if (!existBucket) {
            storageService.createBucket(tenantId);
        }
    }
}
