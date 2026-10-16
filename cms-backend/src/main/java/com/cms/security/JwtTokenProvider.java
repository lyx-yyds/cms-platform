package com.cms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;


/**
 * JWT 双令牌工具（security/JwtTokenProvider.java，文档 7.1）
 *
 * AccessToken：有效期 2 小时，无状态   用于用户日常请求
 * RefreshToken：有效期 7 天，携带 jti（UUID），签发时写入 Redis，刷新时 GETDEL 原子取用  用于用户登录后刷新 AccessToken
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** 生成 AccessToken */
    public String generateAccessToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /** 生成 RefreshToken（含 jti，用于 Redis 存储与 GETDEL 防重放） */
    /** jti 是刷新令牌的唯一标识，用于在 Redis 中作为键名，防止重复刷新 */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /** 解析并校验 Token，失败抛 JwtException */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 校验 Token 是否有效（签名 + 过期） */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Object uid = parseToken(token).get("uid");
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public String getTokenType(String token) {
        Object type = parseToken(token).get("type");
        return type == null ? null : type.toString();
    }

    /** 提取 RefreshToken 的 jti（Redis key 后缀） */
    public String getJti(String token) {
        return parseToken(token).getId();
    }
}
