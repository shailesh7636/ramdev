package com.ramdev.service;

import com.ramdev.dto.LoginRequest;
import com.ramdev.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    /**
     * Authenticates with mobile number (used as username) + password.
     * Throws BadCredentialsException if credentials are wrong.
     */
    public String login(LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getMobile(), req.getPassword())
        );
        return jwtUtils.generateToken(auth);
    }
}
