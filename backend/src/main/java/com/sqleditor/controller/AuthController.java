package com.sqleditor.controller;
import com.sqleditor.model.*;
import com.sqleditor.service.AuthService;
import com.sqleditor.service.ConnectionSessionService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final ConnectionSessionService sessions;

    public AuthController(AuthService a, ConnectionSessionService sessions) {
        auth = a;
        this.sessions = sessions;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest r, HttpServletResponse res) {
        AuthResponse a = auth.register(r);
        cookie(res, auth.issueRefresh(a.email()));
        return a;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest r, HttpServletResponse res) {
        AuthResponse a = auth.login(r);
        cookie(res, auth.issueRefresh(a.email()));
        return a;
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(value = "refresh_token", required = false) String raw, HttpServletResponse res) {
        if (raw == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Oturum bulunamadı.");
        try {
            AuthResponse a = auth.refresh(raw);
            cookie(res, auth.issueRefresh(a.email()));
            return a;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String raw, HttpServletResponse res) {
        if (raw != null) {
            String userId = auth.revoke(raw);
            if (userId != null) {
                sessions.closeAll(userId);
            }
        }
        ResponseCookie c = ResponseCookie.from("refresh_token", "").httpOnly(true).sameSite("Strict").path("/api/auth").maxAge(0).build();
        res.addHeader(HttpHeaders.SET_COOKIE, c.toString());
        return ResponseEntity.noContent().build();
    }

    private void cookie(HttpServletResponse res, String token) {
        ResponseCookie c = ResponseCookie.from("refresh_token", token).httpOnly(true).sameSite("Strict").path("/api/auth").maxAge(java.time.Duration.ofDays(7)).build();
        res.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }
}
