package com.ramdev.controller;

import com.ramdev.entity.User;
import com.ramdev.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardRedirectController {

    private final UserRepository userRepository;

    /**
     * Smart dashboard redirect - sends users to appropriate dashboard based on their role
     */
    @GetMapping("/dashboard")
    public String redirectToDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        String mobile = auth.getName();
        User user = userRepository.findByMobileWithRoles(mobile).orElse(null);
        
        if (user == null) {
            return "redirect:/login";
        }

        if (user.hasRole("SUPER_ADMIN")) {
            return "redirect:/admin/super/dashboard";
        } else if (user.hasRole("ADMIN")) {
            return "redirect:/admin/dashboard";  
        } else {
            return "redirect:/user/home";
        }
    }
}