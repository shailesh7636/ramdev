package com.ramdev.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        
        try {
            // Test database connection
            try (Connection conn = dataSource.getConnection()) {
                status.put("database", "UP");
            }
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
        }
        
        status.put("application", "UP");
        status.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(status);
    }
}