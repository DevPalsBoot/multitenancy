package com.example.multidata.repository.redis;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
public class TenantRepository {

    private static final String TENANTID_SET = "tenantIdList";

    private final RedisTemplate<String, String> userTenantIdRedisTemplate;

    public TenantRepository(@Qualifier("userTenantIdRedisTemplate") RedisTemplate<String, String> userTenantIdRedisTemplate) {
        this.userTenantIdRedisTemplate = userTenantIdRedisTemplate;
    }

    public ValueOperations<String, String> opsForValue() {
        return userTenantIdRedisTemplate.opsForValue();
    }

    public SetOperations<String, String> opsForSet() {
        return userTenantIdRedisTemplate.opsForSet();
    }

    public String findTenantIdByUser(String key) {
        return opsForValue().get(key);
    }

    public Set<String> findAllTenantIds(){
        return opsForSet().members(TENANTID_SET);
    }

    public Boolean isTenantIdExists(String tenantId) {
        return opsForSet().isMember(TENANTID_SET, tenantId);
    }

    public void save(String key, String tenantId) {
        opsForValue().set(key, tenantId);
        addTenantIdToSet(tenantId);
    }

    private void addTenantIdToSet(String tenantId) {
        opsForSet().add(TENANTID_SET, tenantId);
    }
}
