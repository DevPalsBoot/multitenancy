package com.example.multidata.service;

import com.example.multidata.exception.BaseException;
import com.example.multidata.exception.ErrorCode;
import com.example.multidata.security.CustomUserDetailsService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient storageClient;
    private final CustomUserDetailsService customUserDetailsService;

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

    public String putObject(MultipartFile file) {
        String uploadPath = "";
        try {
            uploadPath = file.getName() + ".zip";
            storageClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(getBucket())
                            .object(uploadPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (ServerException | InsufficientDataException | ErrorResponseException
                 | IOException | NoSuchAlgorithmException | InvalidKeyException
                 | InvalidResponseException | XmlParserException | InternalException e) {
            log.error(e.getMessage(), e);
            throw new BaseException(ErrorCode.FAIL_TO_UPLOAD_FILE);
        }
        return uploadPath;
    }

    public byte[] getObject(String objectPath) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(getBucket())
                .object(objectPath)
                .build();
        try (InputStream inputStream = storageClient.getObject(getObjectArgs);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | ServerException | XmlParserException |
                 NoSuchAlgorithmException e) {
            log.error("Failed to download file from path: " + objectPath, e);
            throw new BaseException(ErrorCode.FAIL_TO_DOWNLOAD_FILE);
        }
    }

    private String getBucket() {
        return customUserDetailsService.getUserDetails().getTenantId();
    }
}
