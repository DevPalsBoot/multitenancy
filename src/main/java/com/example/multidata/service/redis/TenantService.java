package com.example.multidata.service.redis;

import org.springframework.stereotype.Service;

import com.example.multidata.repository.redis.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantService {
    private static final String KEY_PREFIX = "email:";
    private static final String KEY_INFIX = ":tenantId";

    private final TenantRepository tenantRepository;

    /**
     * 태넌트 아이디 조회
     * @param email
     * @return
     */
    public String getTenantId(String email) {
        return tenantRepository.findTenantIdByUser(email);
    }

    /**
     * 사용자의 태넌트 아이디 저장
     * @param email
     * @param tenantId
     */
    public void saveUserTenantId(String email, String tenantId) {
        tenantRepository.save(generateKey(email), tenantId);
    }

    private String generateKey(String email) {
        return KEY_PREFIX + email + KEY_INFIX;
    }
}
