package com.example.multidata.controller;

import com.example.multidata.domain.BucketCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.multidata.domain.TenantCreate;
import com.example.multidata.domain.UserCreate;
import com.example.multidata.service.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminRestController {

    private final AdminService adminService;

    @PostMapping("/tenant")
    public ResponseEntity<?> createDbTenant(@RequestBody TenantCreate tenantCreate) {
        if (tenantCreate == null || tenantCreate.getTenantId().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        adminService.createDbTenant(tenantCreate.getTenantId());
        return new ResponseEntity<>(tenantCreate.getTenantId(), HttpStatus.OK);
    }

    @PostMapping("/tenant/users")
    public ResponseEntity<?> createUsers(@RequestBody UserCreate userCreate) {
        if (userCreate == null || userCreate.getTenantId().isEmpty() || userCreate.getUsers().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        adminService.insertUsers(userCreate.getTenantId(), userCreate.getUsers());
        return new ResponseEntity<>(userCreate.getTenantId(), HttpStatus.OK);
    }

    @PostMapping("/tenant/s3")
    public ResponseEntity<?> createS3(@RequestBody BucketCreate bucketCreate) {
        if (bucketCreate == null || bucketCreate.getTenantId().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        adminService.createBucket(bucketCreate.getTenantId());
        return new ResponseEntity<>(bucketCreate.getTenantId(), HttpStatus.OK);
    }
}
