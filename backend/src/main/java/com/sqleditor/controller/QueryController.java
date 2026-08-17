package com.sqleditor.controller;

import com.sqleditor.model.QueryRequest;
import com.sqleditor.model.QueryResponse;
import com.sqleditor.service.QueryService;
import com.sqleditor.service.ConnectionSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class QueryController {

    private final QueryService queryService;
    private final ConnectionSessionService sessions;

    public QueryController(QueryService queryService, ConnectionSessionService sessions) {
        this.queryService = queryService;
        this.sessions = sessions;
    }

    @PostMapping("/execute")
    public ResponseEntity<QueryResponse> execute(@Valid @RequestBody QueryRequest request, 
                                                 @RequestHeader("X-Connection-Token") String token, 
                                                 org.springframework.security.core.Authentication auth) {
        try (Connection connection = sessions.get(auth.getName(), token)) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            return ResponseEntity.ok(queryService.execute(connection, request.getSql(), role, auth.getName(), token));
        } catch (SQLException | SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorgu çalıştırılamadı: " + exception.getMessage(), exception);
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
        try (Connection connection = sessions.get(auth.getName(), token)) {
            return ResponseEntity.ok(queryService.explain(connection, request.getSql()));
        } catch (SQLException | SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explain alınamadı: " + exception.getMessage(), exception);
        }
    }

    @PostMapping("/transaction")
    public ResponseEntity<Void> transaction(@RequestParam String action, 
                                            @RequestHeader("X-Connection-Token") String token, 
                                            org.springframework.security.core.Authentication auth) {
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
        } catch (SQLException | SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "İşlem başarısız: " + exception.getMessage(), exception);
        }
    }
}
