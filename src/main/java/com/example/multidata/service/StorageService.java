package com.example.multidata.service;

import com.example.multidata.exception.BaseException;
import com.example.multidata.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient storageClient;

    public boolean isExistBucket(String tenantId) {
        boolean isExist = false;
        try {
            isExist = storageClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(tenantId)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException| XmlParserException e) {
            throw new BaseException(ErrorCode.FAILED_TO_CHECK_IF_BUCKET_EXISTS);
        }
        return isExist;
    }

    public void createBucket(String tenantId) {
        try {
            storageClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(tenantId)
                            .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException| InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new BaseException(ErrorCode.FAIL_TO_CREATE_BUCKET);
        }
    }
}
