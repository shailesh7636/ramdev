package com.ramdev.controller;

import com.ramdev.entity.User;
import com.ramdev.entity.Video;
import com.ramdev.repository.UserRepository;
import com.ramdev.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Handles all USER-role routes (/user/**).
 *
 * SecurityConfig already blocks non-authenticated requests to /user/**,
 * but @PreAuthorize("hasRole('USER')") adds a second layer so that
 * even an ADMIN who visits /user/home directly still gets redirected
 * to the access-denied page rather than seeing the user view.
 *
 * If you WANT admins to also be able to preview user pages,
 * change hasRole('USER') to isAuthenticated().
 */
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final VideoService   videoService;
    private final UserRepository userRepository;

    @GetMapping("/home")
    @PreAuthorize("hasRole('USER')")
    public String home(Model model, Principal principal) {
        User me = userRepository.findByMobile(principal.getName()).orElseThrow();
        model.addAttribute("me",     me);
        model.addAttribute("videos", videoService.getAllVideos());
        return "user/home";
    }

    @GetMapping("/watch/{id}")
    @PreAuthorize("hasRole('USER')")
    public String watch(@PathVariable Long id, Model model, Principal principal) {
        User  me    = userRepository.findByMobile(principal.getName()).orElseThrow();
        Video video = videoService.findById(id);
        model.addAttribute("me",    me);
        model.addAttribute("video", video);
        return "user/player";
    }

    @GetMapping("/contact")
    @PreAuthorize("hasRole('USER')")
    public String contact(Model model, Principal principal) {
        User me = userRepository.findByMobile(principal.getName()).orElseThrow();
        model.addAttribute("me", me);
        return "user/contact";
    }
}
