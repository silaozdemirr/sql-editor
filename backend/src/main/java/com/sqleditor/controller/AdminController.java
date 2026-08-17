package com.sqleditor.controller;

import com.sqleditor.model.AppRole;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate db;

    public AdminController(JdbcTemplate db) {
        this.db = db;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getUsers() {
        List<UserDTO> users = db.query("SELECT id, email, display_name, role_name FROM app_users ORDER BY email ASC",
                (rs, rowNum) -> new UserDTO(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("role_name")
                ));
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("Rol belirtilmedi.");
        }
        try {
            AppRole.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Geçersiz rol: " + newRole);
        }

        int updated = db.update("UPDATE app_users SET role_name = ? WHERE id = ?", newRole, id);
        if (updated == 0) {
            throw new IllegalArgumentException("Kullanıcı bulunamadı.");
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        int deleted = db.update("DELETE FROM app_users WHERE id = ?", id);
        if (deleted == 0) {
            throw new IllegalArgumentException("Kullanıcı bulunamadı.");
        }
        return ResponseEntity.ok().build();
    }

    public record UserDTO(String id, String email, String displayName, String roleName) {}
}
