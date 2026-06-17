package com.ramdev.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    /**
     * Generates a signed upload signature for direct browser-to-Cloudinary upload.
     * The secret never leaves the server.
     */
    public Map<String, Object> generateSignature(String folder) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        Map<String, Object> params = new HashMap<>();
        params.put("timestamp", timestamp);
        params.put("folder", folder);
        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);
        Map<String, Object> result = new HashMap<>();
        result.put("signature",  signature);
        result.put("timestamp",  timestamp);
        result.put("api_key",    apiKey);
        result.put("cloud_name", cloudName);
        result.put("folder",     folder);
        return result;
    }

    public Map<String, Object> uploadThumbnail(MultipartFile file) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getInputStream(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder",        "ramdev/thumbnails"
                )
        );
        log.info("Cloudinary thumbnail upload success — public_id: {}", result.get("public_id"));
        return result;
    }

    public void deleteVideo(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("deleteVideo called with blank public_id — skipping Cloudinary call.");
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "video")
            );
            log.info("Cloudinary delete result for '{}': {}", publicId, result.get("result"));
        } catch (Exception e) {
            log.error("Failed to delete Cloudinary asset '{}': {}", publicId, e.getMessage());
        }
    }

    public void deleteThumbnail(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            log.info("Cloudinary thumbnail deleted: {}", publicId);
        } catch (Exception e) {
            log.error("Failed to delete Cloudinary thumbnail '{}': {}", publicId, e.getMessage());
        }
    }
}
