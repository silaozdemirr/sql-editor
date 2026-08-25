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
import com.mongodb.client.MongoClient;

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
        String dbType = sessions.getDbType(auth.getName(), token);
        if ("MONGODB".equalsIgnoreCase(dbType)) {
            MongoClient mongo = sessions.getMongo(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getMongoDatabases(mongo));
        } else if ("REDIS".equalsIgnoreCase(dbType)) {
            redis.clients.jedis.JedisPooled redis = sessions.getRedis(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getRedisDatabases(redis));
        } else if ("CASSANDRA".equalsIgnoreCase(dbType) || "SCYLLADB".equalsIgnoreCase(dbType)) {
            com.datastax.oss.driver.api.core.CqlSession cass = sessions.getCassandra(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getCassandraDatabases(cass));
        } else if ("MEMCACHED".equalsIgnoreCase(dbType)) {
            net.spy.memcached.MemcachedClient memcached = sessions.getMemcached(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getMemcachedDatabases(memcached));
        } else if ("NEO4J".equalsIgnoreCase(dbType)) {
            org.neo4j.driver.Driver neo4j = sessions.getNeo4j(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getNeo4jDatabases(neo4j));
        }
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
        String dbType = sessions.getDbType(auth.getName(), token);
        if ("MONGODB".equalsIgnoreCase(dbType)) {
            MongoClient mongo = sessions.getMongo(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getMongoSchema(mongo, database));
        } else if ("REDIS".equalsIgnoreCase(dbType)) {
            redis.clients.jedis.JedisPooled redis = sessions.getRedis(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getRedisSchema(redis, database));
        } else if ("CASSANDRA".equalsIgnoreCase(dbType) || "SCYLLADB".equalsIgnoreCase(dbType)) {
            com.datastax.oss.driver.api.core.CqlSession cass = sessions.getCassandra(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getCassandraSchema(cass, database));
        } else if ("MEMCACHED".equalsIgnoreCase(dbType)) {
            net.spy.memcached.MemcachedClient memcached = sessions.getMemcached(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getMemcachedSchema(memcached, database));
        } else if ("NEO4J".equalsIgnoreCase(dbType)) {
            org.neo4j.driver.Driver neo4j = sessions.getNeo4j(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getNeo4jSchema(neo4j, database));
        }
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
        String dbType = sessions.getDbType(auth.getName(), token);
        if ("MONGODB".equalsIgnoreCase(dbType)) {
            MongoClient mongo = sessions.getMongo(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getMongoColumns(mongo, database, tableName));
        } else if ("REDIS".equalsIgnoreCase(dbType)) {
            redis.clients.jedis.JedisPooled redis = sessions.getRedis(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getRedisColumns(redis, database, tableName));
        } else if ("CASSANDRA".equalsIgnoreCase(dbType) || "SCYLLADB".equalsIgnoreCase(dbType)) {
            com.datastax.oss.driver.api.core.CqlSession cass = sessions.getCassandra(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getCassandraColumns(cass, database, tableName));
        } else if ("MEMCACHED".equalsIgnoreCase(dbType)) {
            net.spy.memcached.MemcachedClient memcached = sessions.getMemcached(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getMemcachedColumns(memcached, database, tableName));
        } else if ("NEO4J".equalsIgnoreCase(dbType)) {
            org.neo4j.driver.Driver neo4j = sessions.getNeo4j(auth.getName(), token);
            return ResponseEntity.ok(schemaService.getNeo4jColumns(neo4j, database, tableName));
        }
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
