package com.ramdev.controller;

import com.ramdev.dto.CreateUserRequest;
import com.ramdev.entity.User;
import com.ramdev.repository.UserRepository;
import com.ramdev.service.CloudinaryService;
import com.ramdev.service.UserService;
import com.ramdev.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserService      userService;
    private final VideoService     videoService;
    private final UserRepository   userRepository;
    private final CloudinaryService cloudinaryService;

    // ════════════════════════════════════════════════════════════
    //  SUPER-ADMIN DASHBOARD  (/admin/super/dashboard)
    //  Only SUPER_ADMIN can access this path.
    // ════════════════════════════════════════════════════════════

    @GetMapping("/admin/super/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String superDashboard(Model model, Principal principal) {
        User me = currentUser(principal);
        model.addAttribute("me", me);
        model.addAttribute("videos", videoService.getAllVideos());
        model.addAttribute("users",  userService.getAllUsers());
        model.addAttribute("admins", userService.getUsersByRole("ADMIN"));
        return "admin/super-dashboard";
    }

    // ── Admin Management (Super-Admin only) ─────────────────────

    @GetMapping("/admin/super/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String listAdmins(Model model, Principal principal) {
        model.addAttribute("me",        currentUser(principal));
        model.addAttribute("admins",    userService.getUsersByRole("ADMIN"));
        model.addAttribute("createReq", new CreateUserRequest());
        return "admin/admins";
    }

    @PostMapping("/admin/super/admins/add")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String addAdmin(@ModelAttribute CreateUserRequest req,
                           RedirectAttributes ra) {
        req.setRole("ADMIN");
        try {
            userService.createUser(req);
            ra.addFlashAttribute("success", "Admin '" + req.getName() + "' added. Password: 12345");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/super/admins";
    }

    @PostMapping("/admin/super/admins/delete/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteAdmin(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "Admin removed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/super/admins";
    }

    // ════════════════════════════════════════════════════════════
    //  SHARED ADMIN DASHBOARD  (/admin/dashboard)
    //  Both ADMIN and SUPER_ADMIN land here after login.
    //  (SUPER_ADMIN lands on /admin/super/dashboard instead.)
    // ════════════════════════════════════════════════════════════

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String dashboard(Model model, Principal principal) {
        User me = currentUser(principal);
        // Super admins have their own richer dashboard — redirect them
        if (me.hasRole("SUPER_ADMIN")) {
            return "redirect:/admin/super/dashboard";
        }
        model.addAttribute("me",     me);
        model.addAttribute("videos", videoService.getAllVideos());
        model.addAttribute("users",  userService.getAllUsers());
        return "admin/dashboard";
    }

    // ── User Management ─────────────────────────────────────────

    @GetMapping("/admin/users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String listUsers(Model model, Principal principal) {
        model.addAttribute("me",        currentUser(principal));
        model.addAttribute("users",     userService.getAllUsers());
        model.addAttribute("createReq", new CreateUserRequest());
        return "admin/users";
    }

    @PostMapping("/admin/users/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String addUser(@Valid @ModelAttribute("createReq") CreateUserRequest req,
                          RedirectAttributes ra) {
        req.setRole("USER");  // users page always creates USER role
        try {
            userService.createUser(req);
            ra.addFlashAttribute("success",
                "User '" + req.getName() + "' added. Default password: 12345");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Video Management ─────────────────────────────────────────

    @GetMapping("/admin/videos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String listVideos(Model model, Principal principal) {
        model.addAttribute("me",     currentUser(principal));
        model.addAttribute("videos", videoService.getAllVideos());
        return "admin/videos";
    }

    /** Returns Cloudinary signed upload params for direct browser upload */
    @GetMapping("/admin/videos/upload-signature")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadSignature() {
        try {
            return ResponseEntity.ok(cloudinaryService.generateSignature("ramdev/videos"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/admin/videos/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String addVideo(@RequestParam String title,
                           @RequestParam(required = false) String titleGuj,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) String category,
                           @RequestParam("videoUrl")   String videoUrl,
                           @RequestParam("publicId")   String publicId,
                           @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                           Principal principal,
                           RedirectAttributes ra) {
        try {
            videoService.addVideoFromCloudinary(title, titleGuj, description, category,
                                                videoUrl, publicId, thumbnail, principal.getName());
            ra.addFlashAttribute("success", "Video uploaded successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/videos";
    }

    @PostMapping("/admin/videos/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String deleteVideo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            videoService.deleteVideo(id);
            ra.addFlashAttribute("success", "Video deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/videos";
    }

    @GetMapping("/admin/videos/watch/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String watchVideo(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("me",    currentUser(principal));
        model.addAttribute("video", videoService.findById(id));
        return "admin/video-player";
    }

    // ── helper ───────────────────────────────────────────────────
    private User currentUser(Principal principal) {
        return userRepository.findByMobile(principal.getName()).orElseThrow();
    }
}
