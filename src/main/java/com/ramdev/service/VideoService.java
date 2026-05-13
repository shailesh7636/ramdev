package com.ramdev.service;

import com.ramdev.entity.User;
import com.ramdev.entity.Video;
import com.ramdev.repository.UserRepository;
import com.ramdev.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository   videoRepository;
    private final UserRepository    userRepository;
    private final CloudinaryService cloudinaryService;

    public List<Video> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc();
    }

    public Video findById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found."));
    }

    /**
     * Uploads the video (and optional thumbnail) to Cloudinary, then
     * persists the metadata — including the Cloudinary public_id — to
     * Supabase (PostgreSQL).
     *
     * If the Supabase INSERT fails, the already-uploaded Cloudinary asset
     * is deleted to avoid orphaned media files.
     */
    @Transactional
    public Video addVideo(String title, String titleGuj, String description,
                          String category, MultipartFile videoFile,
                          MultipartFile thumbnail, String uploaderMobile) throws IOException {

        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("A video file must be selected.");
        }

        // ── Upload video to Cloudinary ──────────────────────────────
        Map<String, Object> videoResult;
        try {
            videoResult = cloudinaryService.uploadVideo(videoFile);
        } catch (IOException e) {
            log.error("Cloudinary video upload failed: {}", e.getMessage());
            throw new IOException("Video upload to Cloudinary failed: " + e.getMessage(), e);
        }

        String videoUrl      = (String) videoResult.get("secure_url");
        String videoPublicId = (String) videoResult.get("public_id");

        // ── Upload thumbnail (optional) ─────────────────────────────
        String thumbUrl = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                Map<String, Object> thumbResult = cloudinaryService.uploadThumbnail(thumbnail);
                thumbUrl = (String) thumbResult.get("secure_url");
            } catch (IOException e) {
                // Non-fatal: log and continue without thumbnail
                log.warn("Thumbnail upload failed (video will still be saved): {}", e.getMessage());
            }
        }

        // ── Resolve uploader ────────────────────────────────────────
        User uploader = userRepository.findByMobile(uploaderMobile).orElse(null);

        // ── Persist to Supabase ─────────────────────────────────────
        Video v = new Video();
        v.setTitle(title);
        v.setTitleGuj(titleGuj);
        v.setDescription(description);
        v.setCategory(category);
        v.setFilePath(videoUrl);
        v.setCloudinaryPublicId(videoPublicId);
        v.setThumbnail(thumbUrl);
        v.setUploadedBy(uploader);

        try {
            Video saved = videoRepository.save(v);
            log.info("Video '{}' saved to Supabase (Cloudinary id: {})", title, videoPublicId);
            return saved;
        } catch (DataAccessException dbEx) {
            // DB insert failed — roll back the Cloudinary upload to avoid orphans
            log.error("Supabase insert failed for '{}', rolling back Cloudinary asset: {}",
                      title, dbEx.getMessage());
            cloudinaryService.deleteVideo(videoPublicId);
            throw new RuntimeException(
                "Failed to save video metadata to the database. " +
                "The Cloudinary upload has been rolled back. Please try again.", dbEx);
        }
    }

    /**
     * Deletes the Cloudinary asset first, then removes the DB record.
     */
    @Transactional
    public void deleteVideo(Long id) {
        Video v = findById(id);
        cloudinaryService.deleteVideo(v.getCloudinaryPublicId());
        videoRepository.delete(v);
        log.info("Video id={} deleted from Supabase and Cloudinary.", id);
    }
}
