package com.sqleditor.controller;

import com.sqleditor.model.ColumnInfo;
import com.sqleditor.model.SchemaResponse;
import com.sqleditor.service.SchemaService;
import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.DumpService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
    private final DumpService dumpService;

    public SchemaController(SchemaService schemaService, ConnectionSessionService sessions, DumpService dumpService) {
        this.schemaService = schemaService;
        this.sessions = sessions;
        this.dumpService = dumpService;
    }

    /**
     * Bağlantıdaki tüm veritabanlarını listeler.
     * GET /api/schema/databases
     */
    @GetMapping("/databases")
    public ResponseEntity<List<String>> getDatabases(@org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token, org.springframework.security.core.Authentication auth) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            return ResponseEntity.ok(schemaService.getDatabases(c));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /**
     * Seçilen veritabanındaki (katalog) tablo ve view ağacını döner.
     * GET /api/schema?database={database}
     */
    @GetMapping
    public ResponseEntity<SchemaResponse> getSchema(
            @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token, 
            org.springframework.security.core.Authentication auth,
            @RequestParam(required = false) String database) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            String targetDb = (database != null && !database.isEmpty()) ? database : c.getCatalog();
            if (targetDb == null) targetDb = c.getSchema();
            return ResponseEntity.ok(schemaService.getSchema(c, targetDb));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /**
     * Seçilen tablo veya view'ın kolonlarını döner.
     * GET /api/schema/{tableName}/columns?database={database}
     */
    @GetMapping("/{tableName}/columns")
    public ResponseEntity<List<ColumnInfo>> getColumns(
            @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token,
            org.springframework.security.core.Authentication auth,
            @PathVariable String tableName,
            @RequestParam(required = false) String database) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            String targetDb = (database != null && !database.isEmpty()) ? database : c.getCatalog();
            if (targetDb == null) targetDb = c.getSchema();
            return ResponseEntity.ok(schemaService.getColumns(c, targetDb, tableName));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /**
     * Tablonun DDL kodunu döner.
     * GET /api/schema/{tableName}/ddl
     */
    @GetMapping("/{tableName}/ddl")
    public ResponseEntity<String> getDDL(
            @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token,
            org.springframework.security.core.Authentication auth,
            @PathVariable String tableName) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            return ResponseEntity.ok(schemaService.getDDL(c, tableName));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /**
     * Veritabanının tamamını .sql formatında indirir.
     * GET /api/schema/dump?database={database}
     */
    @GetMapping("/dump")
    public ResponseEntity<StreamingResponseBody> dumpDatabase(
            @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token,
            org.springframework.security.core.Authentication auth,
            @RequestParam(required = false) String database) {
        
        StreamingResponseBody stream = out -> {
            try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
                String targetDb = (database != null && !database.isEmpty()) ? database : c.getCatalog();
                if (targetDb == null) targetDb = c.getSchema();
                
                // Set catalog/schema if possible before dumping
                if (targetDb != null && !targetDb.isEmpty()) {
                    try {
                        if (c.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle")) {
                            c.setSchema(targetDb);
                        } else {
                            c.setCatalog(targetDb);
                        }
                    } catch (Exception ignored) {}
                }
                
                dumpService.dump(c, out);
            } catch (SQLException e) {
                throw new RuntimeException("Veritabanı yedeği alınamadı", e);
            }
        };

        String filename = (database != null && !database.isEmpty() ? database : "database") + "_dump.sql";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    /**
     * ER Diyagramı için JSON verisi döner.
     * GET /api/schema/erd?database={database}
     */
    @GetMapping("/erd")
    public ResponseEntity<com.sqleditor.model.ErdResponse> getErd(
            @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token,
            org.springframework.security.core.Authentication auth,
            @RequestParam(required = false) String database) {
        try (java.sql.Connection c = sessions.get(auth.getName(), token)) {
            String targetDb = (database != null && !database.isEmpty()) ? database : c.getCatalog();
            if (targetDb == null) targetDb = c.getSchema();
            return ResponseEntity.ok(schemaService.getErd(c, targetDb));
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    private ResponseStatusException databaseError(Exception exception) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Veritabanı şema bilgisi alınamadı: " + exception.getMessage(), exception);
    }
}
