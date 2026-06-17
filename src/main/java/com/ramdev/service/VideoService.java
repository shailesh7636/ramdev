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
     * Saves video metadata after browser has uploaded the video directly to Cloudinary.
     * Server never touches the video file — only saves title, url, public_id to DB.
     * Thumbnail is still uploaded via server (small file, fast).
     */
    @Transactional
    public Video addVideoFromCloudinary(String title, String titleGuj, String description,
                                        String category, String videoUrl, String publicId,
                                        MultipartFile thumbnail, String uploaderMobile) throws IOException {

        // Upload thumbnail via server (small image, fast)
        String thumbUrl = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                Map<String, Object> thumbResult = cloudinaryService.uploadThumbnail(thumbnail);
                thumbUrl = (String) thumbResult.get("secure_url");
            } catch (IOException e) {
                log.warn("Thumbnail upload failed (video will still be saved): {}", e.getMessage());
            }
        }

        User uploader = userRepository.findByMobile(uploaderMobile).orElse(null);

        Video v = new Video();
        v.setTitle(title);
        v.setTitleGuj(titleGuj);
        v.setDescription(description);
        v.setCategory(category);
        v.setFilePath(videoUrl);
        v.setCloudinaryPublicId(publicId);
        v.setThumbnail(thumbUrl);
        v.setUploadedBy(uploader);

        try {
            Video saved = videoRepository.save(v);
            log.info("Video '{}' saved (Cloudinary id: {})", title, publicId);
            return saved;
        } catch (DataAccessException dbEx) {
            log.error("DB insert failed for '{}', rolling back Cloudinary asset: {}", title, dbEx.getMessage());
            cloudinaryService.deleteVideo(publicId);
            throw new RuntimeException("Failed to save video metadata. Cloudinary upload rolled back.", dbEx);
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
