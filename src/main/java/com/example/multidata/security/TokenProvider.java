package com.example.multidata.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.multidata.domain.UserTenant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String TENANT_KEY = "tid";

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.token-period-sec}")
    private Long tokenPeriodSec;

    public String generateToken(UserTenant userTenant) {
        if (userTenant == null || userTenant.getEmail() == null || userTenant.getTenantId() == null) {
            return "";
        }
        return Jwts.builder()
                .claim(AUTHORITIES_KEY, userTenant.getRoleCode())
                .claim(TENANT_KEY, userTenant.getTenantId())
                .subject(userTenant.getEmail().toLowerCase())
                .expiration(getExpirationDate())
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public boolean verifyToken(String token) {
        return getClaims(token)
                .getPayload()
                .getExpiration()
                .after(new Date());
    }

    public String getEmailFromToken(String token) {
        return getClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getTenantIdFromToken(String token) {
        return (String) getClaims(token)
                .getPayload()
                .get(TENANT_KEY);
    }

    public Claims getClaimsBody(String token) {
        return getClaims(token)
                .getBody();
    }

    private Jws<Claims> getClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);
    }

    private Date getExpirationDate() {
        long currentTimeMillis = System.currentTimeMillis();
        long expirationTimeMillis = tokenPeriodSec * 1000; // 초를 밀리초로 변환
        return new Date(currentTimeMillis + expirationTimeMillis);
    }

}
