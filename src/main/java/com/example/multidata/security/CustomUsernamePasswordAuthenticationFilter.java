package com.example.multidata.security;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.multidata.entity.UserLogin;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CustomUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
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
        super.successfulAuthentication(request, response, chain, authResult);
    }
}
