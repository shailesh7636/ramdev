package com.ramdev.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Wraps all Cloudinary upload / delete operations so the rest of the
 * application never touches the SDK directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a video file to Cloudinary under the "ramdev/videos" folder.
     *
     * @param file the MultipartFile received from the HTML form
     * @return a Map with at least "secure_url" and "public_id" keys
     * @throws IOException if the upload fails
     */
    public Map<String, Object> uploadVideo(MultipartFile file) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder",        "ramdev/videos",
                        "overwrite",     false
                )
        );
        log.info("Cloudinary upload success — public_id: {}", result.get("public_id"));
        return result;
    }

    /**
     * Uploads a thumbnail image to Cloudinary under the "ramdev/thumbnails" folder.
     *
     * @param file the MultipartFile received from the HTML form
     * @return a Map with at least "secure_url" and "public_id" keys
     * @throws IOException if the upload fails
     */
    public Map<String, Object> uploadThumbnail(MultipartFile file) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder",        "ramdev/thumbnails"
                )
        );
        log.info("Cloudinary thumbnail upload success — public_id: {}", result.get("public_id"));
        return result;
    }

    /**
     * Deletes a video from Cloudinary by its public_id.
     *
     * @param publicId the Cloudinary public_id stored in the DB
     */
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
            // Log but don't re-throw — let the DB record be removed even if Cloudinary fails.
            log.error("Failed to delete Cloudinary asset '{}': {}", publicId, e.getMessage());
        }
    }

    /**
     * Deletes a thumbnail image from Cloudinary by its public_id.
     *
     * @param publicId the Cloudinary public_id of the thumbnail
     */
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
