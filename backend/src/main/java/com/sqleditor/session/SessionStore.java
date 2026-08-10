package com.sqleditor.session;

import com.sqleditor.model.ConnectionRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Aktif bağlantı oturumlarını bellekte tutan basit session store.
 * sessionId → ConnectionRequest eşlemesi.
 *
 * Aşama 5'te connection pool ile değiştirilecek.
 */
@Component
public class SessionStore {

    private final ConcurrentHashMap<String, ConnectionRequest> sessions = new ConcurrentHashMap<>();

    public void save(String sessionId, ConnectionRequest request) {
        sessions.put(sessionId, request);
    }

    public ConnectionRequest get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean exists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
