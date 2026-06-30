package com.ramdev.config;

import com.ramdev.security.JwtAuthFilter;
import com.ramdev.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.cache.concurrent.ConcurrentMapCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter          jwtAuthFilter;

    @Bean
    public UserCache userCache() throws Exception {
        return new SpringCacheBasedUserCache(new ConcurrentMapCache("userCache"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(6);
    }

    @Bean
    public DaoAuthenticationProvider authProvider() throws Exception {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        p.setUserCache(userCache());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
            throws Exception {
        return cfg.getAuthenticationManager();
    }

    /**
     * Custom 403 handler: instead of a blank Spring error page,
     * redirect the user to their own dashboard with an "Access Denied" flash.
     * A plain USER who types /admin/dashboard in the URL gets sent back to /user/home.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) ->
            response.sendRedirect(request.getContextPath() + "/access-denied");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CSRF protection for production security
            .csrf(csrf -> csrf
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/stream/**", "/api/**") // Only ignore for API endpoints
            )
            .headers(h -> h
                .frameOptions(f -> f.sameOrigin())
                .contentTypeOptions(c -> c.disable()) // Prevent MIME type sniffing
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            )
            // Stateless — JWT cookie carries all auth state
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Public ──────────────────────────────────────────
                .requestMatchers(
                    "/", "/login", "/logout", "/health",
                    "/access-denied", "/privacy-policy", "/delete-account",
                    "/css/**", "/js/**", "/images/**",
                    "/favicon.ico", "/error"
                ).permitAll()

                // ── Super-Admin ONLY ─────────────────────────────────
                // e.g. add/delete other admins
                .requestMatchers("/admin/super/**").hasRole("SUPER_ADMIN")

                // ── Admin + Super-Admin ──────────────────────────────
                // dashboard, user management, video management, video watch
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ── Any logged-in user ───────────────────────────────
                .requestMatchers("/user/**", "/stream/**", "/thumbnails/**")
                    .authenticated()

                // Catch-all — must be authenticated
                .anyRequest().authenticated()
            )
            // Redirect to /login when JWT is missing/expired (401)
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler())
                .authenticationEntryPoint((req, res, authEx) ->
                    res.sendRedirect(req.getContextPath() + "/login"))
            )
            // JWT filter runs before Spring's own username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
