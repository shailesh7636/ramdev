package com.ramdev.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

/**
 * Streams video files through the application.
 * Direct disk paths are never exposed to the browser.
 * No download headers are ever set.
 */
@RestController
@RequiredArgsConstructor
public class VideoStreamController {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.thumbnail.dir}")
    private String thumbnailDir;

    @GetMapping("/stream/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> stream(@PathVariable String filename,
                                           HttpServletRequest request) throws MalformedURLException {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = "video/mp4";
        try {
            String detected = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (detected != null) contentType = detected;
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            // NEVER set Content-Disposition: attachment
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            // Prevent caching of video URLs
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(resource);
    }

    @GetMapping("/thumbnails/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> thumbnail(@PathVariable String filename)
            throws MalformedURLException {
        Path filePath = Paths.get(thumbnailDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(resource);
    }
}
