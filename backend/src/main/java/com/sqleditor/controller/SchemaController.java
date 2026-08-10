package com.sqleditor.controller;

import com.sqleditor.model.ColumnInfo;
import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.SchemaResponse;
import com.sqleditor.service.SchemaService;
import com.sqleditor.session.SessionStore;
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
    private final SessionStore sessionStore;

    public SchemaController(SchemaService schemaService, SessionStore sessionStore) {
        this.schemaService = schemaService;
        this.sessionStore = sessionStore;
    }

    /**
     * Sol paneldeki tablo ve view ağacını döner.
     * GET /api/schema?sessionId={sessionId}
     */
    @GetMapping
    public ResponseEntity<SchemaResponse> getSchema(@RequestParam String sessionId) {
        try {
            return ResponseEntity.ok(schemaService.getSchema(getSession(sessionId)));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /**
     * Seçilen tablo veya view'ın kolonlarını döner.
     * GET /api/schema/{tableName}/columns?sessionId={sessionId}
     */
    @GetMapping("/{tableName}/columns")
    public ResponseEntity<List<ColumnInfo>> getColumns(
            @RequestParam String sessionId,
            @PathVariable String tableName) {
        try {
            return ResponseEntity.ok(schemaService.getColumns(getSession(sessionId), tableName));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    private ConnectionRequest getSession(String sessionId) {
        ConnectionRequest request = sessionStore.get(sessionId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Oturum bulunamadı veya süresi doldu. Lütfen yeniden bağlanın.");
        }
        return request;
    }

    private ResponseStatusException databaseError(SQLException exception) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Veritabanı şema bilgisi alınamadı: " + exception.getMessage(), exception);
    }
}
