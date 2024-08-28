package com.example.multidata.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.multidata.entity.User;
import com.example.multidata.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User selectByEmail(String email) {
        return userRepository.findByUserName(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
    }

    public Optional<User> selectByUserName(String username) {
        return userRepository.findByUserName(username);
    }
}
