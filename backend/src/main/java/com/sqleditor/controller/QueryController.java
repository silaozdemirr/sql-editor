package com.sqleditor.controller;

import com.sqleditor.model.QueryRequest;
import com.sqleditor.model.QueryResponse;
import com.sqleditor.service.QueryService;
import com.sqleditor.service.ConnectionSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<QueryResponse> execute(@Valid @RequestBody QueryRequest request, @org.springframework.web.bind.annotation.RequestHeader("X-Connection-Token") String token, org.springframework.security.core.Authentication auth) {
        try {
            return ResponseEntity.ok(queryService.execute(sessions.get(auth.getName(), token), request.getSql()));
        } catch (SQLException | SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sorgu çalıştırılamadı: " + exception.getMessage(), exception);
        }
    }
}
