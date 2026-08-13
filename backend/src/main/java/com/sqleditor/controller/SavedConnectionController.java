package com.sqleditor.controller;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.ConnectionResponse;
import com.sqleditor.model.SavedConnectionResponse;
import com.sqleditor.service.ConnectionService;
import com.sqleditor.service.ConnectionSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/connections/saved")
public class SavedConnectionController {
    private final ConnectionSessionService sessions;
    private final ConnectionService connections;

    public SavedConnectionController(ConnectionSessionService sessions, ConnectionService connections) {
        this.sessions = sessions;
        this.connections = connections;
    }

    @GetMapping
    public List<SavedConnectionResponse> list(Authentication authentication) {
        return sessions.saved(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody ConnectionRequest request, Authentication authentication) {
        try {
            sessions.save(authentication.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping("/{savedConnectionId}/connect")
    public ConnectionResponse connect(@PathVariable String savedConnectionId, Authentication authentication) {
        try {
            return connections.connect(authentication.getName(), sessions.loadSaved(authentication.getName(), savedConnectionId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    @DeleteMapping("/{savedConnectionId}")
    public ResponseEntity<Void> delete(@PathVariable String savedConnectionId, Authentication authentication) {
        try {
            sessions.deleteSaved(authentication.getName(), savedConnectionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }
}
