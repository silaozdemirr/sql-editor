package com.sqleditor.controller;

import com.sqleditor.model.AiRequest;
import com.sqleditor.model.AiResponse;
import com.sqleditor.service.AiService;
import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.SchemaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AiController {

    private final AiService aiService;
    private final SchemaService schemaService;
    private final ConnectionSessionService sessions;

    public AiController(AiService aiService, SchemaService schemaService, ConnectionSessionService sessions) {
        this.aiService = aiService;
        this.schemaService = schemaService;
        this.sessions = sessions;
    }

    @PostMapping("/generate")
    public ResponseEntity<AiResponse> generateSql(@Valid @RequestBody AiRequest request,
                                                  @RequestHeader("X-Connection-Token") String token,
                                                  @RequestHeader(value = "X-Gemini-Api-Key", required = false) String apiKey,
                                                  org.springframework.security.core.Authentication auth) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Gemini API Anahtarı eksik.");
        }

        try (Connection connection = sessions.get(auth.getName(), token)) {
            // Fetch schema context
            String schemaContext = schemaService.getSchemaSummary(connection, request.getDbType(), null);
            
            // Generate SQL via Gemini
            String sql = aiService.generateSql(request.getPrompt(), schemaContext, request.getDbType(), apiKey);
            
            return ResponseEntity.ok(new AiResponse(sql));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yapay zeka SQL üretemedi: " + exception.getMessage(), exception);
        }
    }
}
