package com.example.multidata.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.multidata.entity.User;
import com.example.multidata.domain.UserTenant;
import com.example.multidata.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserRestController {

    private final UserService userService;

    /**
     * 사용자 조회
     * @param userName
     * @return
     */
    @GetMapping("/{userName}")
    public ResponseEntity<?> selectUser(@PathVariable String userName) {
        Optional<User> user = userService.selectByUserName(userName);
        if (user.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(user, HttpStatus.OK);
        }
    }

    /**
     * 사용자 테넌트 아이디 조회
     * @param userTenant
     * @return
     */
    @PostMapping("/tenant")
    public ResponseEntity<?> saveUserTenantId(@RequestBody UserTenant userTenant) {
        if (userTenant == null || userTenant.getEmail().isEmpty() || userTenant.getTenantId().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        userService.saveUserTenantId(userTenant.getEmail(), userTenant.getTenantId());
        return new ResponseEntity<>(userTenant.getEmail(), HttpStatus.OK);
    }
}
