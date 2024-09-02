package com.example.multidata.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.multidata.service.redis.TenantService;
import com.example.multidata.util.datasource.DataSourceContextHolder;
import com.example.multidata.util.datasource.DataSourceManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationManager implements AuthenticationManager {

    private final CustomUserDetailsService customUserDetailsService;
    private final DataSourceManager dataSourceManager;
    private final TenantService tenantService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 최초 로그인 시 data source routing
        String tenantId = tenantService.getTenantId(authentication.getName());
        dataSourceManager.setCurrent(tenantId);
        DataSourceContextHolder.setRoutingKey(tenantId);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(authentication.getName());
        if (!authentication.getCredentials().equals(userDetails.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        return new UsernamePasswordAuthenticationToken(userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
    }
}
