package com.example.multidata.config;

import com.example.multidata.exception.BaseException;
import com.example.multidata.exception.ErrorCode;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class StorageClientConfig {

    @Value("${storage.url}")
    private String url;
    @Value("${storage.port}")
    private String port;
    @Value("${storage.accessKey}")
    private String accessKey;
    @Value("${storage.secretKey}")
    private String secretKey;
    @Value("${storage.region:#{null}}")
    private String region;


    @Bean
    public MinioClient cloudStorageClient() {
        if (url == null || url.isEmpty()) {
            throw new BaseException(ErrorCode.FAIL_TO_CREATE_STORAGE_CLIENT);
        }
        // timeout 설정 추가
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
        MinioClient.Builder minioBuilder = MinioClient.builder()
                .endpoint(url, Integer.parseInt(port), false)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient);
        if (region != null && !region.isBlank()) {
            minioBuilder.region(region);
        }
        return minioBuilder.build();
    }
}
