package com.example.multidata.entity;

import lombok.Data;

@Data
public class UserTenant {
    private String email;
    private String tenantId;
    private Integer roleCode;
}
