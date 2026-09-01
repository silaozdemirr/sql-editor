package com.sqleditor.controller;

import com.sqleditor.model.QueryRequest;
import com.sqleditor.model.QueryResponse;
import com.sqleditor.service.QueryService;
import com.sqleditor.service.ConnectionSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.sqleditor.model.QueryUpdateReq;

import java.sql.Connection;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class QueryController {
    @PostMapping("/executeStream")
    public org.springframework.http.ResponseEntity<StreamingResponseBody> executeStream(
            @RequestHeader("X-Connection-Token") String token,
            @RequestBody QueryRequest request,
            org.springframework.security.core.Authentication auth) throws java.sql.SQLException {
        
        String role = "READ_ONLY";
        if (auth != null && auth.getAuthorities() != null) {
            role = auth.getAuthorities().stream().findFirst().map(org.springframework.security.core.GrantedAuthority::getAuthority).orElse("ROLE_READ_ONLY").replace("ROLE_", "");
        }
        
        String userId = auth != null ? auth.getName() : "anonymous";
        String dbType = sessions.getDbType(userId, token);
        java.sql.Connection connection = sessions.get(userId, token);
        
        StreamingResponseBody stream = queryService.executeStream(
                connection, dbType, request.getSql(), request.getQueryId(), role, userId, token,
                request.getFilters(), request.getSorts(), request.getLimit(), request.getOffset(), request.getIncludeCount());
                
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_NDJSON)
                .body(stream);
    }

    @PostMapping("/cancel/{queryId}")
    public void cancelQuery(@PathVariable String queryId) {
        queryService.cancelQuery(queryId);
    }


    private final QueryService queryService;
    private final ConnectionSessionService sessions;

    public QueryController(QueryService queryService, ConnectionSessionService sessions) {
        this.queryService = queryService;
        this.sessions = sessions;
    }

    private boolean isNoSql(String dbType) {
        if (dbType == null) return false;
        String t = dbType.toUpperCase();
        return t.equals("MONGODB") || t.equals("REDIS") || t.equals("CASSANDRA") || 
               t.equals("SCYLLADB") || t.equals("MEMCACHED") || t.equals("NEO4J") ||
               t.equals("DYNAMODB") || t.equals("ARANGODB") || t.equals("NEPTUNE") ||
               t.equals("HBASE") || t.equals("COUCHDB");
    }

    @PostMapping("/execute")
    public ResponseEntity<QueryResponse> execute(@Valid @RequestBody QueryRequest request, 
                                                 @RequestHeader("X-Connection-Token") String token, 
                                                 org.springframework.security.core.Authentication auth) {
        try {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            String dbType = sessions.getDbType(auth.getName(), token);

            if (isNoSql(dbType)) {
                QueryResponse res;
                switch (dbType.toUpperCase()) {
                    case "MONGODB" -> res = queryService.executeMongo(sessions.getMongo(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "REDIS" -> res = queryService.executeRedis(sessions.getRedis(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "CASSANDRA", "SCYLLADB" -> res = queryService.executeCassandra(sessions.getCassandra(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "MEMCACHED" -> res = queryService.executeMemcached(sessions.getMemcached(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "NEO4J" -> res = queryService.executeNeo4j(sessions.getNeo4j(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "DYNAMODB" -> res = queryService.executeDynamoDb(sessions.getDynamoDb(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "ARANGODB" -> res = queryService.executeArangoDb(sessions.getArangoDb(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "NEPTUNE" -> res = queryService.executeNeptune(sessions.getNeptune(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "HBASE" -> res = queryService.executeHBase(sessions.getHBase(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    case "COUCHDB" -> res = queryService.executeCouchDb(sessions.getCouchDb(auth.getName(), token), request.getSql(), role, auth.getName(), token);
                    default -> throw new IllegalArgumentException("Desteklenmeyen NoSQL tipi: " + dbType);
                }
                return ResponseEntity.ok(res);
            }

            try (Connection connection = sessions.get(auth.getName(), token)) {
                return ResponseEntity.ok(queryService.execute(connection, request.getSql(), role, auth.getName(), token));
            }
        } catch (Exception exception) {
            QueryResponse errResp = new QueryResponse(
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    null,
                    false,
                    0,
                    "Sorgu hatası: " + exception.getMessage(),
                    null
            );
            return ResponseEntity.ok(errResp);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<java.util.Map<String, String>> deleteRow(@RequestBody com.sqleditor.model.QueryUpdateReq req,
                                                                    @RequestHeader("X-Connection-Token") String token,
                                                                    org.springframework.security.core.Authentication auth) {
        try {
            String dbType = sessions.getDbType(auth.getName(), token);
            if (isNoSql(dbType)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Silme ilemi NoSQL veritabanlar iin desteklenmemektedir.");
            }
            try (java.sql.Connection connection = sessions.get(auth.getName(), token)) {
                String role = auth.getAuthorities().iterator().next().getAuthority();
                int deleted = queryService.deleteRow(connection, role, req);
                return ResponseEntity.ok(java.util.Map.of("message", deleted + " kayt silindi."));
            }
        } catch (java.sql.SQLException exception) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Silme baarsz: " + exception.getMessage(), exception);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<java.util.Map<String, String>> updateCell(@RequestBody QueryUpdateReq req,
                                                                    @RequestHeader("X-Connection-Token") String token,
                                                                    org.springframework.security.core.Authentication auth) {
        try {
            String dbType = sessions.getDbType(auth.getName(), token);
            if (isNoSql(dbType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hücre güncelleme işlemi henüz NoSQL veritabanları için desteklenmemektedir.");
            }

            try (Connection connection = sessions.get(auth.getName(), token)) {
                String role = auth.getAuthorities().iterator().next().getAuthority();
                int updated = queryService.updateCell(connection, role, req);
                return ResponseEntity.ok(java.util.Map.of("message", updated + " kayıt güncellendi."));
            }
        } catch (java.sql.SQLException exception) { exception.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Düzenleme başarısız: " + exception.getMessage(), exception);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getHistory(
            @RequestHeader("X-Connection-Token") String token, 
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(queryService.getHistory(auth.getName(), token));
    }

    @PostMapping("/explain")
    public ResponseEntity<QueryResponse> explain(@Valid @RequestBody QueryRequest request, 
                                                 @RequestHeader("X-Connection-Token") String token, 
                                                 org.springframework.security.core.Authentication auth) {
        try {
            String dbType = sessions.getDbType(auth.getName(), token);
            if (isNoSql(dbType)) {
                QueryResponse errResp = new QueryResponse(
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    null,
                    false,
                    0,
                    "Explain işlemi NoSQL veritabanları için desteklenmiyor.",
                    null
                );
                return ResponseEntity.ok(errResp);
            }

            try (Connection connection = sessions.get(auth.getName(), token)) {
                return ResponseEntity.ok(queryService.explain(connection, request.getSql()));
            }
        } catch (SQLException | SecurityException exception) {
            QueryResponse errResp = new QueryResponse(
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    null,
                    false,
                    0,
                    "Açıklama alınamadı: " + exception.getMessage(),
                    null
            );
            return ResponseEntity.ok(errResp);
        }
    }

    @PostMapping("/transaction")
    public ResponseEntity<Void> transaction(@RequestParam String action, 
                                            @RequestHeader("X-Connection-Token") String token, 
                                            org.springframework.security.core.Authentication auth) {
        try {
            String dbType = sessions.getDbType(auth.getName(), token);
            if (isNoSql(dbType)) {
                // NoSQL veritabanları bu transaction işlemlerini desteklemez. Arayüz hata vermesin diye sessizce 200 dönüyoruz.
                return ResponseEntity.ok().build();
            }

            try (Connection connection = sessions.get(auth.getName(), token)) {
                if ("commit".equalsIgnoreCase(action)) {
                    connection.commit();
                } else if ("rollback".equalsIgnoreCase(action)) {
                    connection.rollback();
                } else if ("autocommit_on".equalsIgnoreCase(action)) {
                    connection.setAutoCommit(true);
                } else if ("autocommit_off".equalsIgnoreCase(action)) {
                    connection.setAutoCommit(false);
                }
                return ResponseEntity.ok().build();
            }
        } catch (java.sql.SQLException exception) { exception.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "İşlem başarısız: " + exception.getMessage(), exception);
        }
    }
}
