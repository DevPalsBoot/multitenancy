package com.example.multidata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.example.multidata.security.CustomUsernamePasswordAuthenticationFilter;
import com.example.multidata.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationManager authenticationManager;

    private static final String[] AUTH_WHITELIST = {
            "/",
            "/resources/**",
            "/index.html",
            "/web-resources/**",
            "/error",
            "/webjars/**",
            "/favicon.ico",
            "/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/webhooks/**",
            "swagger-resources/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                        .requestMatchers(AUTH_WHITELIST)
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .logout(l -> l
                        .logoutSuccessUrl("/").permitAll()
                )
                .csrf(c -> c
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/**")
                )
                .addFilter(new CustomUsernamePasswordAuthenticationFilter(authenticationManager))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .getOrBuild();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}
