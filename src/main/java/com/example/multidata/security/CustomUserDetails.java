package com.example.multidata.security;

import java.util.Collection;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.multidata.entity.User;

@Getter
@RequiredArgsConstructor
@Setter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private String tenantId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return user.getPwd();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }
}
