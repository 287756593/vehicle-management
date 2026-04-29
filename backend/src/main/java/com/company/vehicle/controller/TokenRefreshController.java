package com.company.vehicle.controller;

import com.company.vehicle.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class TokenRefreshController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);

            // 验证 token（包括过期检查）
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            // 检查 token 是否即将过期（剩余时间少于1小时）
            Date expirationDate = jwtUtil.getExpirationDateFromToken(token);
            if (expirationDate == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Cannot determine token expiration"));
            }

            long timeUntilExpiration = expirationDate.getTime() - System.currentTimeMillis();
            long oneHourInMillis = 60 * 60 * 1000;

            if (timeUntilExpiration > oneHourInMillis) {
                // Token 还有超过1小时有效期，不需要刷新
                return ResponseEntity.ok(Map.of(
                    "message", "Token is still valid",
                    "expiresIn", timeUntilExpiration
                ));
            }

            // 生成新 token
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            String newToken = jwtUtil.generateToken(username, role);

            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("message", "Token refreshed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }
}
