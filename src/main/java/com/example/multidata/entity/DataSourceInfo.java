package com.example.multidata.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Entity
@Builder
@Getter
@Table(name = "datasource_info")
public class DataSourceInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "datasource_map_id")
    private Long id;

    @Column(name = "tenant_id", unique = true)
    private String tenantId; // unique 제약 추가

    @Column(name = "company")
    private String company;

    @Column(name = "url")
    private String url;

    @Column(name = "driver")
    private String driver;

    // TODO database, schema 값 따로 받을 지 결정
}

