package com.company.vehicle.controller;

import com.company.vehicle.service.UploadStorageService;
import com.company.vehicle.service.UploadStorageService.StoredImageResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/uploads")
public class UploadPreviewController {

    private final UploadStorageService uploadStorageService;

    public UploadPreviewController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @GetMapping("/preview")
    public ResponseEntity<FileSystemResource> preview(@RequestParam("path") String path) {
        StoredImageResource imageResource = uploadStorageService.resolveDriverPreview(path);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(imageResource.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .contentType(mediaType)
                .contentLength(imageResource.length())
                .body(new FileSystemResource(imageResource.path()));
    }
}
