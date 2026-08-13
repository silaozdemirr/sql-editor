package com.sqleditor.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {
    private final byte[] secret;
    private final long accessSeconds;
    private final ObjectMapper mapper;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-token-minutes}") long minutes, ObjectMapper mapper) {
        if (secret == null || secret.length() < 32 || secret.startsWith("replace-this")) throw new IllegalStateException("JWT_SECRET en az 32 karakter olmalıdır.");
        this.secret = secret.getBytes(StandardCharsets.UTF_8); this.accessSeconds = minutes * 60; this.mapper = mapper;
    }
    public String createAccessToken(String userId, String email) {
        try {
            String head = enc(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String body = enc(mapper.writeValueAsBytes(Map.of("sub", userId, "email", email, "iat", Instant.now().getEpochSecond(), "exp", Instant.now().plusSeconds(accessSeconds).getEpochSecond())));
            return head + "." + body + "." + enc(sign(head + "." + body));
        } catch (Exception e) { throw new IllegalStateException("Token oluşturulamadı", e); }
    }
    public Map<String, Object> verify(String token) {
        try {
            String[] p = token.split("\\."); if (p.length != 3 || !constantEquals(sign(p[0] + "." + p[1]), dec(p[2]))) throw new IllegalArgumentException("Geçersiz token");
            Map<String,Object> claims = mapper.readValue(dec(p[1]), new TypeReference<>() {});
            if (((Number) claims.get("exp")).longValue() <= Instant.now().getEpochSecond()) throw new IllegalArgumentException("Süresi dolmuş token");
            return claims;
        } catch (Exception e) { throw new IllegalArgumentException("Geçersiz erişim tokenı", e); }
    }
    private byte[] sign(String v) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret, "HmacSHA256")); return mac.doFinal(v.getBytes(StandardCharsets.UTF_8)); }
    private String enc(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private byte[] dec(String s) { return Base64.getUrlDecoder().decode(s); }
    private boolean constantEquals(byte[] a, byte[] b) { return java.security.MessageDigest.isEqual(a,b); }
}
