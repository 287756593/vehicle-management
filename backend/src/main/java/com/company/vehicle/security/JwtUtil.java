package com.company.vehicle.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        logger.debug("Generating token for user: {}, role: {}, expires at: {}", username, role, expiryDate);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("role", String.class);
        } catch (JwtException e) {
            logger.error("Failed to extract role from token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 明确检查过期时间
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                logger.warn("Token has expired at: {}", expiration);
                return false;
            }

            logger.debug("Token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("Token validation failed - expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.error("Token validation failed - invalid signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.error("Token validation failed - malformed token: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.error("Token validation failed - unsupported token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.error("Token validation failed - illegal argument: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            logger.error("Token validation failed - general error: {}", e.getMessage());
            return false;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (JwtException e) {
            logger.error("Failed to extract expiration date from token: {}", e.getMessage());
            return null;
        }
    }
}
