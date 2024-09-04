# Multi-Tenancy Database with Spring Boot

생성 일시: 2024년 9월 4일 오전 9:49
생성자: 박지원 (since1909)

## 기술 구성

- Java 17
- Spring Boot 3.3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- JDBC
- Flyway

## 프로젝트 소개

### Multi-Tenancy

하나의 애플리케이션에서 테넌트를 분리하여 고객별로 테이터를 나누어 저장한다.

멀티 테넌시 구축 방안은 데이터 격리 방법에 따라 달라지는데, 여기서는 DB 분리를 통한 아키텍처를 설계한다.

### 구조도

![%EC%8A%AC%EB%9D%BC%EC%9D%B4%EB%93%9C1](https://github.com/user-attachments/assets/821bfdc4-c2b8-4a7c-bfb7-4275efbb18e1)


![%EC%8A%AC%EB%9D%BC%EC%9D%B4%EB%93%9C2](https://github.com/user-attachments/assets/8900581f-1882-4585-baa8-45ebe1dc8196)


![%EC%8A%AC%EB%9D%BC%EC%9D%B4%EB%93%9C3](https://github.com/user-attachments/assets/d8aa04ad-5b49-4fb9-b76a-873463e81030)

### 실행 방법
application.yml 에 DB, Redis connection 설정 정보 수정 후 boot start

### 사전 조건

1. Redis에 User-TenantId 값이 저장되어 있다.
2. TenantId 에 대응되는 DB가 생성되어 있다.
3. 각 테넌트 별로 사용자 정보가 저장되어 있다.

두 사전 조건을 만족하기 위한 API 제공

- **테넌트 생성**

```bash
curl --location --request POST 'http://localhost:8080/api/admin/tenant' \
--header 'Content-Type: application/json' \
--data-raw '{
    "tenantId" : "xhznshdbdntl"
}'
```

**/api/admin/tenant**

테넌트 아이디를 사용해 **CREATE DATABASE** 쿼리를 실행합니다.
DB 생성 후에는 **flyway** 가 migration 을 자동으로 수행합니다.

- **테넌트에 사용자 추가**

```bash
curl --location --request POST 'http://localhost:8080/api/admin/tenant/users' \
--header 'Content-Type: application/json' \
--data-raw '{
    "tenantId" : "xhznshdbdntl",
    "users" : [
        {
            "email" : "yushi45@gmail.com",
            "name" : "yushi",
            "pwd" : "asdf1234!"
        }
    ]
}'
```

**/api/admin/tenant/users**

테넌트 아이디를 통해 저장하고자 하는 DB 를 선택하고, 해당하는 테넌트에 **사용자를 저장**합니다.
**Redis** 에 User - TenantId 값을 저장합니다.


## 구현 방식

spring jdbc 의 AbstractRoutingDataSource 의 `determineCurrentLookupKey` 를 구현하여 데이터 소스가 동적으로 라우팅 될 수 있도록 한다. ThreadLocal 변수에 현재 사용할 LookupKey 를 저장하고 context 흐름에 따라 그 변수 값을 확인하여 현재의 LookupKey 를 결정하는 구조를 만든다.

### LookupKey

각 데이터 소스를 식별할 수 있는 값

Tenant Id 를 Unique 하게 발급한다는 조건 하에 TenantId 를 LookupKey 로 사용한다.

### DataSourceContextHolder

ThreadLocal 변수를 가지고 있는 클래스

context 흐름에 따라 LookupKey 를 set/get 할 수 있다.

## 데이터소스 관리

스프링 부트가 뜨면서 데이터 소스를 초기화 할 때 접속할 디폴트 디비 설정값이 필요하다. 이 디폴트 디비 설정에 **라우터로 관리할 데이터 소스 타겟**들이 저장되어야 한다.  또한, 저장된 데이터 소스들은 동적으로 관리가 가능해야 한다.

### DataSourceManager

데이터 소스의 초기화, 추가 등 라우팅 대상이 되는 데이터 소스들을 한데 모아 관리한다.