package com.example.multidata.controller;

import com.example.multidata.domain.FileDownload;
import com.example.multidata.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "file", description = "File API")
public class FileRestController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<?> downloadFile(@RequestPart MultipartFile file) {
        return new ResponseEntity<>(storageService.putObject(file), HttpStatus.OK);
    }

    @PostMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestBody FileDownload fileDownload) {
        String fileName = fileDownload.getFileName();
        byte[] fileContent = storageService.getObject(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("filename",  fileName);
        headers.setContentLength(fileContent.length);
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

}
