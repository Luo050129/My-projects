package com.luoxianjun.community.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发 / 解析 / 校验。HS256，密钥来自配置（生产用环境变量注入）。
 * token 中携带 userId 与 role，供鉴权过滤器与业务侧取当前用户。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtTokenProvider(@Value("${community.jwt.secret}") String secret,
                            @Value("${community.jwt.expiration-minutes:120}") long expirationMinutes) {
        // 启动期 fail-fast：prod 默认空密钥原本会在 JJWT 内部抛 WeakKeyException，
        // 报错看不出原因。这里给出可直接执行的修复提示，而不是放任弱密钥上线。
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置：请通过环境变量 JWT_SECRET 注入至少 32 字节的随机串");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥长度不足：HS256 要求至少 32 字节，当前 " + secretBytes.length + " 字节，请检查 JWT_SECRET");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = expirationMinutes * 60 * 1000L;
    }

    public String createToken(Long userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
