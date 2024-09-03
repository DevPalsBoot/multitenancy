package com.example.multidata.domain;

import java.util.List;

import lombok.Data;

@Data
public class UserCreate {
    private String tenantId;
    private List<UserInsert> users;
}
