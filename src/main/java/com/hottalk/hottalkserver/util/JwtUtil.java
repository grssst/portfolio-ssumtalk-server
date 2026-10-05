package com.hottalk.hottalkserver.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret-key}")
    private String secretKey; // 실제 비밀 키를 사용
    private static String SECRET_KEY; // `static` 변수
    @PostConstruct
    public void init() {
        SECRET_KEY = secretKey; // `static` 변수에 값 할당
    }
    private static final long EXPIRATION_TIME = 3153600000000L;  // 1일 (밀리초 단위)

    public static String generateToken(String userId) {
        return JWT.create()
                .withSubject(userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    public static String validateToken(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token)
                .getSubject();
    }

    public static Claims validateTokenAndGetClaims(String token) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String generateAdminToken(String adminId) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + 106000000; // 1시간 유효
        Date exp = new Date(expMillis);
        return Jwts.builder()
                .setSubject(adminId)
                .claim("role", "admin")
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

}
