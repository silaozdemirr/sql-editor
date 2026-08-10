package com.sqleditor.controller;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.ConnectionResponse;
import com.sqleditor.service.ConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Veritabanı bağlantı REST API controller'ı.
 *
 * Endpoints:
 *   POST /api/connection/test    → Bağlantıyı test et (bağlanıp kopar)
 *   POST /api/connection/connect → Bağlantı kur, session ID döndür
 */
@RestController
@RequestMapping("/api/connection")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Bağlantı testi - "Test Connection" butonu için.
     * Bağlanır ve hemen kapar, sonucu döndürür.
     */
    @PostMapping("/test")
    public ResponseEntity<ConnectionResponse> testConnection(@Valid @RequestBody ConnectionRequest request) {
        ConnectionResponse response = connectionService.testConnection(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Tam bağlantı kurma - "Connect" butonu için.
     * Başarıyla bağlanırsa session ID döner.
     */
    @PostMapping("/connect")
    public ResponseEntity<ConnectionResponse> connect(@Valid @RequestBody ConnectionRequest request) {
        ConnectionResponse response = connectionService.connect(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("SQL Editor Backend is running! 🚀");
    }
}
