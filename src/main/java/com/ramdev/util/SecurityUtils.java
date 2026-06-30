package com.ramdev.util;

import org.springframework.web.util.HtmlUtils;
import java.util.regex.Pattern;

public class SecurityUtils {
    
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,()]+$");
    
    /**
     * Sanitize HTML input to prevent XSS attacks
     */
    public static String sanitizeHtml(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input.trim());
    }
    
    /**
     * Validate mobile number format
     */
    public static boolean isValidMobile(String mobile) {
        return mobile != null && MOBILE_PATTERN.matcher(mobile).matches();
    }
    
    /**
     * Validate safe string input (alphanumeric + basic punctuation)
     */
    public static boolean isSafeString(String input) {
        return input != null && SAFE_STRING_PATTERN.matcher(input).matches();
    }
    
    /**
     * Clean and validate file path to prevent directory traversal
     */
    public static String sanitizeFilePath(String path) {
        if (path == null) return null;
        
        // Remove any directory traversal attempts
        return path.replaceAll("\\.\\.[\\\\/]", "")
                  .replaceAll("[\\\\/]+", "/")
                  .trim();
    }
}