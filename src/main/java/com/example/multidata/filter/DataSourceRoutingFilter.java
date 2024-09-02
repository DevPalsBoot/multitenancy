package com.example.multidata.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.multidata.security.TokenProvider;
import com.example.multidata.service.redis.TenantService;
import com.example.multidata.util.datasource.DataSourceContextHolder;
import com.example.multidata.util.datasource.DataSourceManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class DataSourceRoutingFilter extends OncePerRequestFilter {

    private static final Pattern LOGIN_URI_PATTERN = Pattern.compile("^/api/user/login$");

    @Autowired
    private DataSourceManager dataSourceManager;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private TenantService tenantService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getBearerToken(request);
        if (token == null) {
            log.error("Error: token not found in request header.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "token not found");
            throw new RuntimeException("Error: token not found in request header.");
        }
        String tenantId = tokenProvider.getTenantIdFromToken(token);
        dataSourceManager.setCurrent(tenantId);
        DataSourceContextHolder.setRoutingKey(tenantId);
        filterChain.doFilter(request, response);
    }

    /**
     * token 에서 tenant id 값 추출
     * @param request
     * @return
     */
    private String getBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
