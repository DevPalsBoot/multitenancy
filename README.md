# Multi-Tenancy Database, S3 with Spring Boot

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

### 1. Database Multi-Tenancy

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

### 서버 패치 전략

서버 패치 시 데이터소스 초기화 및 마이그레이션 방안

서버 패치 시 datasource map 은 리셋되므로 기존에 생성된 테넌트를 재등록하는 과정이 필요함. 이때, 테이블 구조 등 디비 변경사항이 모든 테넌트에 일괄로 적용되어야 하기 때문에 각 데이터 소스 별 마이그레이션을 수행해야함

**Q. 각 테넌트의 유저가 실제 connection 하는 타이밍에 최초 커넥션이면 map 에 등록 후 마이그레이션 하면 되지 않은가?**

A. 마이그레이션의 소요시간은 일정하지 않고 request 에 대해 수 초 안에 응답을 줘야 하기 때문에 마이그레이션을 별도로 일괄 수행하는 것이 안정적일것이라는 판단. 또한, 마이그레이션 실패 시 디비 형상이 롤백하면 사용자에게 잘못된 데이터를 사용해 응답을 줄 수 있는 여지가 있음. 따라서 서버 패치 시 일괄 적용하고 그 결과를 모니터링 하는 과정을 별로도 만듦.

**생성된 데이터소스들을 map 에 일괄 추가**

Redis 에 tenantId 를 저장하는 set 을 만들어 관리한다. User-TenantId 가 Redis 에 저장될 때 set 에도 추가를 시도함. 데이터 소스 초기화 시 해당 set 을 조회하여 datasource map 을 초기화 해줌. 초기화가 된 map 을 순회하며 migration 도 진행.

### Default Schema 변경

- DP-04 : public schema 사용 제한

[CSAP SaaS 인증을 위한 보안 설정](https://guide-gov.ncloud-docs.com/docs/clouddbforpostgresql-csap)

> PostgreSQL에서 DB를 생성할 경우 Default로 public schema가 생성된다. 다른 schema를 생성하지 않고 table을 생성할 경우 기본적으로 public schema 안에 생성이 되며 public schema는 모든 개체에서 접근이 가능하므로 정보유출, 자원고갈 등의 위험성이 있음
>

이러한 보안 기준에 따라 default schema 를 public 이 아닌 값으로 설정한다.

설정 대상은 postgresql, jpa, flyway, datasource config 이다. jpa, flyway 설정은 application.yml 설정 값 변경을 통해 쉽게 바꿀 수 있으나, DataSource 의 경우 bean 초기화 시에 config 값에 search path 설정을 추가해야 한다.

또한, 이 프로젝트에서는 여러 데이터소스를 생성하고 관리하는 만큼 각 데이터소스가 생성될 때 search path 를 설정해주는 configuration 을 추가 했다.

```java
private HikariDataSource createDataSource(String url) {
	HikariDataSource dataSource = new HikariDataSource();
  dataSource.setJdbcUrl(url);
  dataSource.setUsername(defaultUsername);
  dataSource.setPassword(defaultPwd);
  dataSource.setDriverClassName(defaultDriver);
  dataSource.setMaximumPoolSize(10);
  dataSource.setConnectionInitSql("SET search_path TO " + defaultSchema);
  return dataSource;
}
```


### 2. S3 Multi-Tenancy
하나의 애플리케이션에서 테넌트별로 S3 버킷을 나누어 사용하는 구조

### 구조도
![s3-architecture.png](src%2Fmain%2Fresources%2Fs3-architecture.png)

### 실행 방법
application.yml에 S3 설정 정보 수정 후 boot start

```yaml
storage:
  url: 127.0.0.1
  port: 9000
  accessKey: 
  secretKey: 
```

### 사전 조건
1. S3 서버가 정상적으로 동작한다.
2. TenantId에 매핑되는 S3 버킷이 있다. (API 제공)
3. TenantId가 담긴 JWT 토큰이 있다.

```
curl --location 'http://127.0.0.1:8080/api/admin/tenant/s3' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqaXN1bmEzMTRAZ21haWwuY29tIiwiZXhwIjoxNzI2MDM2MDc3fQ.OSEbjYljohTgDrV3hoOKkIkyM_Tvgk-I6CNbHFUPMt_72cJSlfvVWcDlpuIoeZrIU9gajZQLN4sCU55HVeAc8Q' \
--header 'Cookie: refresh_token=eyJhbGciOiJIUzUxMiJ9.eyJlbWFpbCI6ImFzc2V0QHNwYXJyb3dmYXNvby5jb20iLCJleHAiOjE3MzA3MDI1ODN9.mrr5L-s3yLByifjeEt_IuEgaMTTThAeqWdSfrpdjoUU7a4jiWY3rk3zHbv271I8nGFXHIMpMw3ZMSsymx0SWxA' \
--data '{
"tenantId" : "TestTenant"
}'
```

### 로컬 테스트
1. S3(minio) 서버 띄우기
```shell
$ docker run -p 9000:9000 -p 9001:9001 --name multi-minio -v D:/minio/data:/data -e "MINIO_ROOT_USER=ROOTUSER" -e "MINIO_ROOT_PASSWORD=CHANGEME123" 
quay.io/minio/minio:latest server /data --console-address ":9001"
```
2. S3(minio) AccessKey 생성
콘솔 접속하여 AccessKey 생성
3. TenantId에 매핑되는 S3 버킷 생성 API 요청
4. tenantId가 담긴 JWT 토큰을 넣어서 다운로드 요청

