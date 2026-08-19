package com.sqleditor.service;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.SavedConnectionResponse;
import com.sqleditor.security.CredentialCipher;
import com.sqleditor.security.TokenHasher;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class ConnectionSessionService {
    private final JdbcTemplate db;
    private final CredentialCipher cipher;
    private final TokenHasher hasher;
    private final int minutes;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, SingleConnectionDataSource> pools = new ConcurrentHashMap<>();

    public ConnectionSessionService(JdbcTemplate db, CredentialCipher cipher, TokenHasher hasher,
            @Value("${app.connection.session-minutes}") int minutes) {
        this.db = db;
        this.cipher = cipher;
        this.hasher = hasher;
        this.minutes = minutes;
    }

    public String create(String userId, ConnectionRequest request) {
        String token = token();
        CredentialCipher.Encrypted encrypted = cipher.encrypt(request.getPassword());
        db.update(
                """
                        insert into connection_sessions(id,user_id,token_hash,db_type,host,port,database_name,db_username,password_ciphertext,password_iv,expires_at)
                        values(?,?,?,?,?,?,?,?,?,?,?)
                        """,
                UUID.randomUUID().toString(), userId, hasher.hash(token), request.getDbType().toUpperCase(),
                request.getHost(), request.getPort(), request.getDatabase(), request.getUsername(),
                encrypted.ciphertext(), encrypted.iv(),
                Timestamp.from(Instant.now().plus(Duration.ofMinutes(minutes))));
        return token;
    }

    public Connection get(String userId, String token) throws SQLException {
        String tokenHash = hasher.hash(token);
        List<Stored> rows = db.query("""
                select db_type,host,port,database_name,db_username,password_ciphertext,password_iv
                from connection_sessions where user_id=? and token_hash=?
                """, (rs, rowNum) -> new Stored(rs.getString(1), rs.getString(2), rs.getInt(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)), userId, tokenHash);
        if (rows.isEmpty())
            throw new SecurityException("Bağlantı oturumu bulunamadı veya süresi doldu.");
        SingleConnectionDataSource pool = pools.computeIfAbsent(tokenHash, ignored -> pool(rows.get(0)));
        return pool.getConnection();
    }

    public void close(String userId, String token) {
        String tokenHash = hasher.hash(token);
        db.update("delete from connection_sessions where user_id=? and token_hash=?", userId, tokenHash);
        closePool(tokenHash);
    }

    public void closeAll(String userId) {
        List<String> hashes = db.query("select token_hash from connection_sessions where user_id=?", 
                (rs, rowNum) -> rs.getString(1), userId);
        db.update("delete from connection_sessions where user_id=?", userId);
        hashes.forEach(this::closePool);
    }

    @Scheduled(fixedRate = 60000)
    public void evictExpired() {
        Timestamp now = Timestamp.from(Instant.now());
        List<String> hashes = db.query("select token_hash from connection_sessions where expires_at <= ?", 
                (rs, rowNum) -> rs.getString(1), now);
        if (!hashes.isEmpty()) {
            db.update("delete from connection_sessions where expires_at <= ?", now);
            hashes.forEach(this::closePool);
        }
    }

    /** Başarılı bağlantıları, parolayı içermeden kullanıcı geçmişine ekler. */
    public void history(String userId, ConnectionRequest request) {
        db.update("""
                insert into connection_history(id,user_id,connection_name,db_type,host,port,database_name,db_username)
                values(?,?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), userId, blank(request.getConnectionName()),
                request.getDbType().toUpperCase(), request.getHost(), request.getPort(),
                request.getDatabase(), request.getUsername());
    }

    public void save(String userId, ConnectionRequest request) {
        String name = requiredName(request.getConnectionName());
        CredentialCipher.Encrypted encrypted = cipher.encrypt(request.getPassword());
        db.update(
                """
                        insert into saved_connections(id,user_id,connection_name,db_type,host,port,database_name,db_username,password_ciphertext,password_iv)
                        values(?,?,?,?,?,?,?,?,?,?)
                        """,
                UUID.randomUUID().toString(), userId, name, request.getDbType().toUpperCase(), request.getHost(),
                request.getPort(), request.getDatabase(), request.getUsername(), encrypted.ciphertext(),
                encrypted.iv());
    }

    public List<SavedConnectionResponse> saved(String userId) {
        return db.query("""
                select id,connection_name,db_type,host,port,database_name,db_username,updated_at
                from saved_connections where user_id=? order by updated_at desc
                """, (rs, rowNum) -> new SavedConnectionResponse(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getTimestamp(8).toInstant()),
                userId);
    }

    public ConnectionRequest loadSaved(String userId, String savedConnectionId) {
        List<StoredSaved> rows = db.query("""
                select connection_name,db_type,host,port,database_name,db_username,password_ciphertext,password_iv
                from saved_connections where id=? and user_id=?
                """, (rs, rowNum) -> new StoredSaved(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)), savedConnectionId, userId);
        if (rows.isEmpty())
            throw new IllegalArgumentException("Kayıtlı bağlantı bulunamadı.");
        StoredSaved saved = rows.get(0);
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionName(saved.name());
        request.setDbType(saved.type());
        request.setHost(saved.host());
        request.setPort(saved.port());
        request.setDatabase(saved.database());
        request.setUsername(saved.username());
        request.setPassword(cipher.decrypt(saved.ciphertext(), saved.iv()));
        return request;
    }

    public void deleteSaved(String userId, String savedConnectionId) {
        if (db.update("delete from saved_connections where id=? and user_id=?", savedConnectionId, userId) == 0) {
            throw new IllegalArgumentException("Kayıtlı bağlantı bulunamadı.");
        }
    }

    private void closePool(String tokenHash) {
        SingleConnectionDataSource pool = pools.remove(tokenHash);
        if (pool != null)
            pool.destroy();
    }

    private SingleConnectionDataSource pool(Stored stored) {
        String pass = cipher.decrypt(stored.ciphertext(), stored.iv());
        SingleConnectionDataSource ds = new SingleConnectionDataSource(url(stored), stored.username(), pass, true);
        ds.setAutoCommit(true);
        return ds;
    }

    private String url(Stored s) {
        return switch (s.type().toUpperCase()) {
            case "MYSQL" -> "jdbc:mysql://" + s.host() + ":" + s.port() + "/" + s.database()
                    + "?useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8&sessionVariables=time_zone='%2B03:00'";
            case "POSTGRESQL" -> "jdbc:postgresql://" + s.host() + ":" + s.port() + "/" + s.database();
            case "MSSQL" -> "jdbc:sqlserver://" + s.host() + ":" + s.port() + ";databaseName=" + s.database() + ";encrypt=false";
            case "ORACLE" -> "jdbc:oracle:thin:@//" + s.host() + ":" + s.port() + "/" + s.database();
            case "MARIADB" -> "jdbc:mariadb://" + s.host() + ":" + s.port() + "/" + s.database() 
                    + "?useSSL=false&characterEncoding=UTF-8";
            case "SQLITE" -> "jdbc:sqlite:" + s.database();
            default -> throw new IllegalArgumentException("Desteklenmeyen veritabanı tipi: " + s.type());
        };
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String requiredName(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Bağlantı adı boş olamaz.");
        return value.trim();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Stored(String type, String host, int port, String database, String username, String ciphertext,
            String iv) {
    }

    private record StoredSaved(String name, String type, String host, int port, String database, String username,
            String ciphertext, String iv) {
    }
}
