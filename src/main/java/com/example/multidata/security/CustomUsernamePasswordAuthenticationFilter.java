package com.example.multidata.security;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.multidata.entity.User;
import com.example.multidata.domain.UserLogin;
import com.example.multidata.domain.UserTenant;
import com.example.multidata.service.UserService;
import com.example.multidata.service.redis.TenantService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final UserService userService;
    private final TenantService tenantService;
    private final TokenProvider tokenProvider;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CustomUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager,
                                                      UserService userService,
                                                      TenantService tenantService,
                                                      TokenProvider tokenProvider) {
        super(authenticationManager);
        this.userService = userService;
        this.tenantService = tenantService;
        this.tokenProvider = tokenProvider;
        setFilterProcessesUrl("/api/user/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            UserLogin userLogin = objectMapper.readValue(request.getInputStream(), UserLogin.class);
            if (userLogin == null) {
                throw new AuthenticationCredentialsNotFoundException("bad request login");
            }
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(userLogin.getEmail(),
                            userLogin.getPassword(),
                            new ArrayList<>())
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("attempt login failed");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        User user = userService.selectByEmail(authResult.getName());
        UserTenant userTenant = new UserTenant();
        userTenant.setEmail(user.getEmail());
        userTenant.setTenantId(tenantService.getTenantId(user.getEmail()));
        response.getWriter().write(tokenProvider.generateToken(userTenant));    // user-tenantId 값으로 token 생성
        // refresh token 생략
    }
}
