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

    public record LoginResult(String token, Authentication auth) {}

    /**
     * Authenticates with mobile number (used as username) + password.
     * Returns both the JWT token and the Authentication so the caller
     * can read roles without an extra DB call.
     * Throws BadCredentialsException if credentials are wrong.
     */
    public LoginResult login(LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getMobile(), req.getPassword())
        );
        return new LoginResult(jwtUtils.generateToken(auth), auth);
    }
}
