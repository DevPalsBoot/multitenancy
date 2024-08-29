package com.example.multidata.repository.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
public class TenantRepository {

    private final RedisTemplate<String, String> userTenantIdRedisTemplate;

    public TenantRepository(@Qualifier("userTenantIdRedisTemplate") RedisTemplate<String, String> userTenantIdRedisTemplate) {
        this.userTenantIdRedisTemplate = userTenantIdRedisTemplate;
    }

    public ValueOperations<String, String> opsForValue() {
        return userTenantIdRedisTemplate.opsForValue();
    }

    public String findTenantIdByUser(String key) {
        return opsForValue().get(key);
    }

    public void save(String key, String tenantId) {
        opsForValue().set(key, tenantId);
    }
}
