package com.ramdev.service;

import com.ramdev.entity.User;
import com.ramdev.entity.Video;
import com.ramdev.repository.UserRepository;
import com.ramdev.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * Optimized method for mobile - loads only recent videos with limit
     */
    @Cacheable("recentVideos")
    public List<Video> getAllVideosOptimized() {
        Pageable topRecent = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        return videoRepository.findAll(topRecent).getContent();
    }

    public Video findById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found."));
    }

    /**
     * Saves video metadata after browser has uploaded video + thumbnail
     * directly to Cloudinary. Server never touches any file.
     */
    @Transactional
    public Video addVideoFromCloudinary(String title, String titleGuj, String description,
                                        String category, String videoUrl, String publicId,
                                        String thumbUrl, String uploaderMobile) {

        User uploader = userRepository.findByMobile(uploaderMobile).orElse(null);

        Video v = new Video();
        v.setTitle(title);
        v.setTitleGuj(titleGuj);
        v.setDescription(description);
        v.setCategory(category);
        v.setFilePath(videoUrl);
        v.setCloudinaryPublicId(publicId);
        v.setThumbnail((thumbUrl != null && !thumbUrl.isBlank()) ? thumbUrl : null);
        v.setUploadedBy(uploader);

        try {
            Video saved = videoRepository.save(v);
            log.info("Video '{}' saved (Cloudinary id: {})", title, publicId);
            return saved;
        } catch (DataAccessException dbEx) {
            log.error("DB insert failed for '{}', rolling back Cloudinary asset: {}", title, dbEx.getMessage(), dbEx);
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
