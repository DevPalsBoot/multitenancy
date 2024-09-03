package com.example.multidata.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.multidata.domain.UserInsert;
import com.example.multidata.entity.User;
import com.example.multidata.repository.UserRepository;
import com.example.multidata.service.redis.TenantService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;

    public User selectByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
    }

    public void saveUser(UserInsert userInsert) {
        User user = new User();
        user.setEmail(userInsert.getEmail());
        user.setUserName(userInsert.getName());
        user.setPwd(userInsert.getPwd());

        userRepository.save(user);
    }

    public Optional<User> selectByUserName(String username) {
        return userRepository.findByUserName(username);
    }

    public void saveUserTenantId(String email, String tenantId) {
        tenantService.saveUserTenantId(email, tenantId);
    }
}
