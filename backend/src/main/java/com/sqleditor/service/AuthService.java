package com.sqleditor.service;

import com.sqleditor.model.AppRole;
import com.sqleditor.model.AuthRequest;
import com.sqleditor.model.AuthResponse;
import com.sqleditor.security.JwtService;
import com.sqleditor.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final TokenHasher hash;
    private final int refreshDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(JdbcTemplate db, PasswordEncoder passwords, JwtService jwt, TokenHasher hash,
                       @Value("${app.jwt.refresh-token-days}") int refreshDays) {
        this.db = db;
        this.passwords = passwords;
        this.jwt = jwt;
        this.hash = hash;
        this.refreshDays = refreshDays;
    }

    public AuthResponse register(AuthRequest request) {
        String email = email(request.getEmail());
        String name = displayName(request);
        String id = UUID.randomUUID().toString();
        try {
            db.update("insert into app_users(id,email,display_name,password_hash,role_name) values(?,?,?,?,?)",
                    id, email, name, passwords.encode(request.getPassword()), AppRole.EDITOR.name());
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Bu e-posta zaten kayıtlı.");
        }
        return response(new User(id, email, name, AppRole.EDITOR, null));
    }

    public AuthResponse login(AuthRequest request) {
        User user = find(email(request.getEmail()));
        if (user == null || !passwords.matches(request.getPassword(), user.passwordHash())) {
            throw new IllegalArgumentException("E-posta veya parola hatalı.");
        }
        return response(user);
    }

    public String issueRefresh(String email) {
        User user = find(email(email));
        String raw = rawToken();
        db.update("insert into refresh_tokens(id,user_id,token_hash,expires_at) values(?,?,?,?)",
                UUID.randomUUID().toString(), user.id(), hash.hash(raw),
                Timestamp.from(Instant.now().plus(Duration.ofDays(refreshDays))));
        return raw;
    }

    public AuthResponse refresh(String raw) {
        List<User> users = db.query("""
                select u.id,u.email,u.display_name,u.role_name,u.password_hash
                from refresh_tokens t join app_users u on u.id=t.user_id
                where t.token_hash=? and t.revoked_at is null and t.expires_at > current_timestamp
                """, (rs, rowNum) -> new User(rs.getString(1), rs.getString(2), rs.getString(3),
                AppRole.valueOf(rs.getString(4)), rs.getString(5)), hash.hash(raw));
        if (users.isEmpty()) throw new IllegalArgumentException("Oturum yenileme tokenı geçersiz.");
        db.update("update refresh_tokens set revoked_at=current_timestamp where token_hash=?", hash.hash(raw));
        return response(users.get(0));
    }

    public String revoke(String raw) {
        String tokenHash = hash.hash(raw);
        List<String> userIds = db.query("select user_id from refresh_tokens where token_hash=?", 
                (rs, rowNum) -> rs.getString(1), tokenHash);
        db.update("update refresh_tokens set revoked_at=current_timestamp where token_hash=?", tokenHash);
        return userIds.isEmpty() ? null : userIds.get(0);
    }

    private User find(String email) {
        List<User> users = db.query("select id,email,display_name,role_name,password_hash from app_users where email=?",
                (rs, rowNum) -> new User(rs.getString(1), rs.getString(2), rs.getString(3),
                        AppRole.valueOf(rs.getString(4)), rs.getString(5)), email);
        return users.isEmpty() ? null : users.get(0);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.createAccessToken(user.id(), user.email(), user.role().name()),
                user.email(), user.name(), user.role().name());
    }

    private String email(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private String displayName(AuthRequest request) {
        return request.getDisplayName() == null || request.getDisplayName().isBlank()
                ? request.getEmail().trim() : request.getDisplayName().trim();
    }
    private String rawToken() {
        byte[] bytes = new byte[48]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private record User(String id, String email, String name, AppRole role, String passwordHash) { }
}
