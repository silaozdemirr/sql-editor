package com.sqleditor.service;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.ConnectionResponse;
import com.sqleditor.session.SessionStore;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Veritabanı bağlantı servisi.
 * JDBC kullanarak gerçek bağlantı testi ve kurulumu yapar.
 */
@Service
public class ConnectionService {

    private final SessionStore sessionStore;

    public ConnectionService(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * Bağlantıyı test eder: bağlanıp hemen kapar.
     * "Test Connection" butonuna karşılık gelir.
     */
    public ConnectionResponse testConnection(ConnectionRequest request) {
        long startTime = System.currentTimeMillis();
        String jdbcUrl = buildJdbcUrl(request);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, request.getUsername(), request.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            long elapsed = System.currentTimeMillis() - startTime;

            return ConnectionResponse.builder()
                    .success(true)
                    .serverVersion(meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion())
                    .databaseName(request.getDatabase())
                    .host(request.getHost())
                    .port(request.getPort())
                    .dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!")
                    .responseTimeMs(elapsed)
                    .build();

        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + getFriendlyErrorMessage(e))
                    .errorDetail(e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    /**
     * Tam bağlantı kurar ve session ID döndürür.
     * "Connect" butonuna karşılık gelir.
     * (Şu an test ile aynı, ileride connection pool eklenecek)
     */
    public ConnectionResponse connect(ConnectionRequest request) {
        long startTime = System.currentTimeMillis();
        String jdbcUrl = buildJdbcUrl(request);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, request.getUsername(), request.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            long elapsed = System.currentTimeMillis() - startTime;

            String sessionId = UUID.randomUUID().toString();
            sessionStore.save(sessionId, request);

            return ConnectionResponse.builder()
                    .success(true)
                    .sessionId(sessionId)
                    .serverVersion(meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion())
                    .databaseName(conn.getCatalog())
                    .host(request.getHost())
                    .port(request.getPort())
                    .dbType(request.getDbType())
                    .message("Bağlantı başarıyla kuruldu!")
                    .responseTimeMs(elapsed)
                    .build();

        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + getFriendlyErrorMessage(e))
                    .errorDetail(e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    /**
     * Veritabanı tipine göre JDBC URL oluşturur.
     */
    private String buildJdbcUrl(ConnectionRequest request) {
        return switch (request.getDbType().toUpperCase()) {
            case "MYSQL" -> String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            case "POSTGRESQL" -> String.format(
                    "jdbc:postgresql://%s:%d/%s",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            case "MSSQL" -> String.format(
                    "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            default -> throw new IllegalArgumentException("Desteklenmeyen veritabanı tipi: " + request.getDbType());
        };
    }

    /**
     * SQL hata mesajını kullanıcı dostu hale getirir.
     */
    private String getFriendlyErrorMessage(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return "Bilinmeyen hata";

        if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            return "Sunucuya ulaşılamıyor. Host veya port bilgisini kontrol edin.";
        }
        if (msg.contains("Access denied")) {
            return "Erişim reddedildi. Kullanıcı adı veya şifrenizi kontrol edin.";
        }
        if (msg.contains("Unknown database")) {
            return "Veritabanı bulunamadı. Veritabanı adını kontrol edin.";
        }
        if (msg.contains("SSL")) {
            return "SSL bağlantı hatası.";
        }
        return msg;
    }
}
