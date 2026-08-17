package com.sqleditor.controller;

import com.sqleditor.model.ColumnInfo;
import com.sqleditor.model.SchemaResponse;
import com.sqleditor.service.SchemaService;
import com.sqleditor.service.ConnectionSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;

/**
 * Schema Explorer REST API.
 * Bağlantı bilgileri istemciden yeniden alınmaz; connect çağrısının döndürdüğü
 * sessionId ile bellekteki bağlantı tanımı kullanılır.
 */
@RestController
@RequestMapping("/api/schema")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SchemaController {

    private final SchemaService schemaService;
    private final ConnectionSessionService sessions;

    public SchemaController(SchemaService schemaService, ConnectionSessionService sessions) {
        this.schemaService = schemaService;
        this.sessions = sessions;
    }

    /**
     * Sol paneldeki tablo ve view ağacını döner.
     * GET /api/schema?sessionId={sessionId}
     */
    @GetMapping
    public ResponseEntity<SchemaResponse> getSchema(@org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token, org.springframework.security.core.Authentication auth) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            return ResponseEntity.ok(schemaService.getSchema(c, c.getCatalog()));
        } catch (SQLException | SecurityException e) {
            throw databaseError(e);
        }
    }

    /**
     * Seçilen tablo veya view'ın kolonlarını döner.
     * GET /api/schema/{tableName}/columns?sessionId={sessionId}
     */
    @GetMapping("/{tableName}/columns")
    public ResponseEntity<List<ColumnInfo>> getColumns(
            @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token,
            org.springframework.security.core.Authentication auth,
            @PathVariable String tableName) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            return ResponseEntity.ok(schemaService.getColumns(c, c.getCatalog(), tableName));
        } catch (SQLException | SecurityException e) {
            throw databaseError(e);
        }
    }

    private ResponseStatusException databaseError(Exception exception) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Veritabanı şema bilgisi alınamadı: " + exception.getMessage(), exception);
    }
}
