package com.example.todo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
    }

    public final long accessTokenValidityMs = 15L * 60L * 1000L;
    public final long refreshTokenValidityMs = 30L * 24 * 60 * 60 * 1000;

    private String generateToken(Long userId, String type, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateAccessToken(Long userId) { return generateToken(userId, "access", accessTokenValidityMs); }

    public String generateRefreshToken(Long userId) { return generateToken(userId, "refresh", refreshTokenValidityMs); }

    private Claims parseAllClaim(String token) {
        String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(rawToken)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateAccessToken(String token) {
        Claims claims = parseAllClaim(token);
        if (claims == null) return false;
        String tokenType = claims.get("type", String.class);
        return tokenType.equals("access");
    }

    public boolean validateRefreshToken(String token) {
        Claims claims = parseAllClaim(token);
        if (claims == null) return false;
        String tokenType = claims.get("type", String.class);
        return tokenType.equals("refresh");
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseAllClaim(token);
        if (claims == null) throw new ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid token");
        return Long.parseLong(claims.getSubject());
    }

}
