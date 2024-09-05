package com.example.multidata.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_TYPE = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        String userEmail = null;
        String requestURI = request.getRequestURI();
        try {
            if (token != null && tokenProvider.verifyToken(token)) {
                userEmail = tokenProvider.getEmailFromToken(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 토큰이 유효하지 않은 경우
            response.getWriter().write("The token is invalid.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        if (request.getRequestURI().equals("/api/auth/token")) {
            // 토큰 재발급인 경우 인증 정보 확인하지 않음
            return null;
        }
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        String value = null;
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_TYPE)) {
            value = bearerToken.substring(TOKEN_TYPE.length());
            if ("null".equals(value)) {
                value = null;
            }
        }
        return value;
    }
}

