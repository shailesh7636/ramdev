package com.ramdev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieConfig {
    
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;
    
    @Value("${app.cookie.samesite:Lax}")
    private String cookieSameSite;
    
    public boolean isSecure() {
        return cookieSecure;
    }
    
    public String getSameSite() {
        return cookieSameSite;
    }
}