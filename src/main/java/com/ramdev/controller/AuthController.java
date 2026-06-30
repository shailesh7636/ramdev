package com.ramdev.controller;

import com.ramdev.dto.LoginRequest;
import com.ramdev.entity.User;
import com.ramdev.repository.UserRepository;
import com.ramdev.service.AuthService;
import com.ramdev.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService    authService;
    private final UserRepository userRepository;
    private final UserService    userService;
    private final CookieConfig   cookieConfig;

    /** Root → login page */
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    /** Show login form */
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    /**
     * Process login.
     *
     * On success  → set JWT HttpOnly cookie, then redirect by role:
     *   SUPER_ADMIN → /admin/super/dashboard
     *   ADMIN       → /admin/dashboard
     *   USER        → /user/home
     *
     * On failure  → return to login with "Invalid mobile number and password."
     */
    @PostMapping("/login")
    public String doLogin(@Valid @ModelAttribute("loginRequest") LoginRequest req,
                          BindingResult bindingResult,
                          HttpServletResponse response,
                          Model model) {

        // Bean-validation errors (e.g. non-numeric mobile)
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Invalid mobile number and password.");
            return "auth/login";
        }

        try {
            AuthService.LoginResult result = authService.login(req);

            // Secure, HttpOnly cookie with configurable settings
            Cookie cookie = new Cookie("JWT", result.token());
            cookie.setHttpOnly(true);
            cookie.setSecure(cookieConfig.isSecure());
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 3600); // 7 days
            cookie.setAttribute("SameSite", cookieConfig.getSameSite());
            response.addCookie(cookie);

            // Set SecurityContext immediately for redirect
            SecurityContextHolder.getContext().setAuthentication(result.auth());

            // Redirect to generic dashboard - let DashboardRedirectController handle role-based routing
            return "redirect:/dashboard";

        } catch (BadCredentialsException ex) {
            model.addAttribute("error", "Invalid mobile number and password.");
            model.addAttribute("loginRequest", req);
            return "auth/login";
        }
    }

    /** Clears the JWT cookie and returns to login */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("JWT", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        SecurityContextHolder.clearContext();
        return "redirect:/login?logout";
    }

    /** Privacy Policy — public, no login required */
    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy-policy";
    }

    /** Delete Account page — public */
    @GetMapping("/delete-account")
    public String deleteAccountPage() {
        return "delete-account";
    }

    /** Process account deletion by mobile number */
    @PostMapping("/delete-account")
    public String deleteAccount(@RequestParam String mobile, RedirectAttributes ra) {
        mobile = mobile.trim();
        if (!mobile.matches("\\d{10}")) {
            ra.addFlashAttribute("error", "Please enter a valid 10-digit mobile number.");
            return "redirect:/delete-account";
        }
        User user = userRepository.findByMobile(mobile).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "No account found with this mobile number.");
            return "redirect:/delete-account";
        }
        if (user.hasRole("SUPER_ADMIN")) {
            ra.addFlashAttribute("error", "This account cannot be deleted.");
            return "redirect:/delete-account";
        }
        userService.deleteUser(user.getId());
        ra.addFlashAttribute("success", "Your account has been deleted successfully.");
        return "redirect:/delete-account";
    }

    /**
     * Access-denied page.
     * Spring Security calls this when a USER tries to visit /admin/**
     * or any other route they don't have authority for.
     */
    @GetMapping("/access-denied")
    public String accessDenied(HttpServletRequest request, Model model) {
        // Try to figure out where to send them back
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String home = "/login";
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));
            home = isAdmin ? "/admin/dashboard" : "/user/home";
        }
        model.addAttribute("homeUrl", home);
        return "auth/access-denied";
    }
}
