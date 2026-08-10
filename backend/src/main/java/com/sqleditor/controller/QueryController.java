package com.sqleditor.controller;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.QueryRequest;
import com.sqleditor.model.QueryResponse;
import com.sqleditor.service.QueryService;
import com.sqleditor.session.SessionStore;
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
    private final SessionStore sessionStore;

    public QueryController(QueryService queryService, SessionStore sessionStore) {
        this.queryService = queryService;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/execute")
    public ResponseEntity<QueryResponse> execute(@Valid @RequestBody QueryRequest request) {
        ConnectionRequest connection = sessionStore.get(request.getSessionId());
        if (connection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Oturum bulunamadı veya süresi doldu. Lütfen yeniden bağlanın.");
        }
        try {
            return ResponseEntity.ok(queryService.execute(connection, request.getSql()));
        } catch (SQLException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sorgu çalıştırılamadı: " + exception.getMessage(), exception);
        }
    }
}
