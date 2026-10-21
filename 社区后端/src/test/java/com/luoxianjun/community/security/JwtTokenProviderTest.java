package com.luoxianjun.community.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具纯单测：不依赖 Spring 容器，验证签发/解析/校验与失效场景。
 */
class JwtTokenProviderTest {

    // HS256 要求密钥至少 256 bit（32 字节），这里给一个足够长的测试密钥
    private final JwtTokenProvider provider =
            new JwtTokenProvider("test-secret-key-which-is-long-enough-for-hs256-aaaaaaa", 30);

    @Test
    void createAndParseOk() {
        String token = provider.createToken(42L, "ADMIN");
        assertTrue(provider.validate(token));
        assertEquals(42L, provider.getUserId(token));
        assertEquals("ADMIN", provider.getRole(token));
    }

    @Test
    void expiredTokenIsInvalid() {
        // 过期时间设为 -1 分钟，签发即过期
        JwtTokenProvider expired = new JwtTokenProvider(
                "test-secret-key-which-is-long-enough-for-hs256-aaaaaaa", -1);
        String token = expired.createToken(1L, "USER");
        assertFalse(expired.validate(token));
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = provider.createToken(1L, "USER");
        // 篡改 payload 段，使签名必然失效
        String[] parts = token.split("\\.");
        parts[1] = parts[1] + "x";
        String tampered = String.join(".", parts);
        assertFalse(provider.validate(tampered));
        assertFalse(provider.validate("not-a-jwt"));
    }
}
