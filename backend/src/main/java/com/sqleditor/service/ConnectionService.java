package com.sqleditor.service;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.ConnectionResponse;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import redis.clients.jedis.JedisPooled;
import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;
import net.spy.memcached.MemcachedClient;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.AuthTokens;

/**
 * Veritabanı bağlantı servisi.
 * JDBC ve NoSQL kullanarak gerçek bağlantı testi ve kurulumu yapar.
 */
@Service
public class ConnectionService {

    private final ConnectionSessionService sessions;

    public ConnectionService(ConnectionSessionService sessions) {
        this.sessions = sessions;
    }

    public ConnectionResponse testConnection(ConnectionRequest request) {
        long startTime = System.currentTimeMillis();
        
        if ("MONGODB".equalsIgnoreCase(request.getDbType())) {
            return testMongoConnection(request, startTime);
        } else if ("REDIS".equalsIgnoreCase(request.getDbType())) {
            return testRedisConnection(request, startTime);
        } else if ("CASSANDRA".equalsIgnoreCase(request.getDbType()) || "SCYLLADB".equalsIgnoreCase(request.getDbType())) {
            return testCassandraConnection(request, startTime);
        } else if ("MEMCACHED".equalsIgnoreCase(request.getDbType())) {
            return testMemcachedConnection(request, startTime);
        } else if ("NEO4J".equalsIgnoreCase(request.getDbType())) {
            return testNeo4jConnection(request, startTime);
        } else if ("DYNAMODB".equalsIgnoreCase(request.getDbType())) {
            return testDynamoDbConnection(request, startTime);
        } else if ("ARANGODB".equalsIgnoreCase(request.getDbType())) {
            return testArangoDbConnection(request, startTime);
        } else if ("NEPTUNE".equalsIgnoreCase(request.getDbType())) {
            return testNeptuneConnection(request, startTime);
        } else if ("HBASE".equalsIgnoreCase(request.getDbType())) {
            return testHBaseConnection(request, startTime);
        } else if ("COUCHDB".equalsIgnoreCase(request.getDbType())) {
            return testCouchDbConnection(request, startTime);
        }

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
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private ConnectionResponse testMongoConnection(ConnectionRequest request, long startTime) {
        String uri = buildMongoUrl(request);
        try (MongoClient client = MongoClients.create(uri)) {
            Document ping = new Document("ping", 1);
            Document result = client.getDatabase("admin").runCommand(ping);
            long elapsed = System.currentTimeMillis() - startTime;
            
            return ConnectionResponse.builder()
                    .success(result.getDouble("ok") == 1.0)
                    .serverVersion("MongoDB")
                    .databaseName(request.getDatabase())
                    .host(request.getHost())
                    .port(request.getPort())
                    .dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!")
                    .responseTimeMs(elapsed)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private ConnectionResponse testRedisConnection(ConnectionRequest request, long startTime) {
        try {
            JedisPooled pool;
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                // Her zaman "default" kullan, frontend'den gelen yanl veya otomatik doldurulmu username'i yoksay.
                pool = new JedisPooled(request.getHost(), request.getPort(), "default", request.getPassword());
            } else {
                pool = new JedisPooled(request.getHost(), request.getPort());
            }
            String ping = pool.ping();
            pool.close();
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success("PONG".equalsIgnoreCase(ping))
                    .serverVersion("Redis")
                    .databaseName(request.getDatabase())
                    .host(request.getHost())
                    .port(request.getPort())
                    .dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!")
                    .responseTimeMs(elapsed)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private ConnectionResponse testCassandraConnection(ConnectionRequest request, long startTime) {
        try {
            var builder = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(request.getHost(), request.getPort()))
                    .withLocalDatacenter("datacenter1");
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                builder.withAuthCredentials(request.getUsername(), request.getPassword() != null ? request.getPassword() : "");
            }
            if (request.getDatabase() != null && !request.getDatabase().isBlank()) {
                builder.withKeyspace(request.getDatabase());
            }
            try (CqlSession session = builder.build()) {
                session.execute("SELECT release_version FROM system.local");
                long elapsed = System.currentTimeMillis() - startTime;
                return ConnectionResponse.builder()
                        .success(true)
                        .serverVersion("Cassandra")
                        .databaseName(request.getDatabase())
                        .host(request.getHost())
                        .port(request.getPort())
                        .dbType(request.getDbType())
                        .message("Bağlantı testi başarılı!")
                        .responseTimeMs(elapsed)
                        .build();
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private ConnectionResponse testMemcachedConnection(ConnectionRequest request, long startTime) {
        try {
            MemcachedClient client = new MemcachedClient(new InetSocketAddress(request.getHost(), request.getPort()));
            client.getVersions(); // just to test connection
            client.shutdown();
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(true)
                    .serverVersion("Memcached")
                    .databaseName(request.getDatabase())
                    .host(request.getHost())
                    .port(request.getPort())
                    .dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!")
                    .responseTimeMs(elapsed)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private ConnectionResponse testNeo4jConnection(ConnectionRequest request, long startTime) {
        try {
            String uri = "bolt://" + request.getHost() + ":" + request.getPort();
            Driver driver;
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                driver = GraphDatabase.driver(uri, AuthTokens.basic(request.getUsername(), request.getPassword() != null ? request.getPassword() : ""));
            } else {
                driver = GraphDatabase.driver(uri, AuthTokens.none());
            }
            driver.verifyConnectivity();
            driver.close();
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(true)
                    .serverVersion("Neo4j")
                    .databaseName(request.getDatabase())
                    .host(request.getHost())
                    .port(request.getPort())
                    .dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!")
                    .responseTimeMs(elapsed)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return ConnectionResponse.builder()
                    .success(false)
                    .message("Bağlantı hatası: " + e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    public ConnectionResponse connect(String userId, ConnectionRequest request) {
        long startTime = System.currentTimeMillis();
        
        if ("MONGODB".equalsIgnoreCase(request.getDbType()) || "REDIS".equalsIgnoreCase(request.getDbType()) || 
            "CASSANDRA".equalsIgnoreCase(request.getDbType()) || "SCYLLADB".equalsIgnoreCase(request.getDbType()) ||
            "MEMCACHED".equalsIgnoreCase(request.getDbType()) || "NEO4J".equalsIgnoreCase(request.getDbType()) ||
            "DYNAMODB".equalsIgnoreCase(request.getDbType()) || "ARANGODB".equalsIgnoreCase(request.getDbType()) ||
            "NEPTUNE".equalsIgnoreCase(request.getDbType()) || "HBASE".equalsIgnoreCase(request.getDbType()) ||
            "COUCHDB".equalsIgnoreCase(request.getDbType())) {
            
            ConnectionResponse testRes;
            if ("MONGODB".equalsIgnoreCase(request.getDbType())) testRes = testMongoConnection(request, startTime);
            else if ("REDIS".equalsIgnoreCase(request.getDbType())) testRes = testRedisConnection(request, startTime);
            else if ("MEMCACHED".equalsIgnoreCase(request.getDbType())) testRes = testMemcachedConnection(request, startTime);
            else if ("NEO4J".equalsIgnoreCase(request.getDbType())) testRes = testNeo4jConnection(request, startTime);
            else if ("DYNAMODB".equalsIgnoreCase(request.getDbType())) testRes = testDynamoDbConnection(request, startTime);
            else if ("ARANGODB".equalsIgnoreCase(request.getDbType())) testRes = testArangoDbConnection(request, startTime);
            else if ("NEPTUNE".equalsIgnoreCase(request.getDbType())) testRes = testNeptuneConnection(request, startTime);
            else if ("HBASE".equalsIgnoreCase(request.getDbType())) testRes = testHBaseConnection(request, startTime);
            else if ("COUCHDB".equalsIgnoreCase(request.getDbType())) testRes = testCouchDbConnection(request, startTime);
            else testRes = testCassandraConnection(request, startTime);

            if (!testRes.isSuccess()) {
                return testRes;
            }
            String connectionToken = sessions.create(userId, request);
            sessions.history(userId, request);
            return ConnectionResponse.builder()
                    .success(true)
                    .connectionToken(connectionToken)
                    .serverVersion(testRes.getServerVersion())
                    .databaseName(testRes.getDatabaseName())
                    .host(testRes.getHost())
                    .port(testRes.getPort())
                    .dbType(testRes.getDbType())
                    .message("Bağlantı başarıyla kuruldu!")
                    .responseTimeMs(testRes.getResponseTimeMs())
                    .build();
        }

        String jdbcUrl = buildJdbcUrl(request);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, request.getUsername(), request.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            long elapsed = System.currentTimeMillis() - startTime;

            String connectionToken = sessions.create(userId, request);
            sessions.history(userId, request);

            return ConnectionResponse.builder()
                    .success(true)
                    .connectionToken(connectionToken)
                    .serverVersion(meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion())
                    .databaseName(request.getDatabase())
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
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private ConnectionResponse testDynamoDbConnection(ConnectionRequest request, long startTime) {
        try (var client = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
            .endpointOverride(java.net.URI.create("http://" + request.getHost() + ":" + request.getPort()))
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                    request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : "dummy",
                    request.getPassword() != null && !request.getPassword().isBlank() ? request.getPassword() : "dummy"
                )
            )).build()) {
            client.listTables();
            return ConnectionResponse.builder().success(true).serverVersion("DynamoDB").dbType(request.getDbType())
                .message("Bağlantı testi başarılı!").responseTimeMs(System.currentTimeMillis() - startTime).build();
        } catch (Exception e) {
            return ConnectionResponse.builder().success(false).message("Bağlantı hatası: " + e.getMessage())
                .responseTimeMs(System.currentTimeMillis() - startTime).build();
        }
    }

    private ConnectionResponse testArangoDbConnection(ConnectionRequest request, long startTime) {
        var builder = new com.arangodb.ArangoDB.Builder()
            .host(request.getHost(), request.getPort())
            .user(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : "root")
            .password(request.getPassword() != null ? request.getPassword() : "");
        com.arangodb.ArangoDB client = null;
        try {
            client = builder.build();
            client.getVersion();
            return ConnectionResponse.builder().success(true).serverVersion("ArangoDB").dbType(request.getDbType())
                .message("Bağlantı testi başarılı!").responseTimeMs(System.currentTimeMillis() - startTime).build();
        } catch (Exception e) {
            return ConnectionResponse.builder().success(false).message("Bağlantı hatası: " + e.getMessage())
                .responseTimeMs(System.currentTimeMillis() - startTime).build();
        } finally {
            if (client != null) client.shutdown();
        }
    }

    private ConnectionResponse testNeptuneConnection(ConnectionRequest request, long startTime) {
        org.apache.tinkerpop.gremlin.driver.Cluster cluster = null;
        org.apache.tinkerpop.gremlin.driver.Client client = null;
        try {
            cluster = org.apache.tinkerpop.gremlin.driver.Cluster.build()
                .addContactPoint(request.getHost())
                .port(request.getPort())
                .create();
            client = cluster.connect();
            client.submit("g.V().limit(1)").all().get();
            return ConnectionResponse.builder().success(true).serverVersion("Neptune/Gremlin").dbType(request.getDbType())
                .message("Bağlantı testi başarılı!").responseTimeMs(System.currentTimeMillis() - startTime).build();
        } catch (Exception e) {
            return ConnectionResponse.builder().success(false).message("Bağlantı hatası: " + e.getMessage())
                .responseTimeMs(System.currentTimeMillis() - startTime).build();
        } finally {
            if (client != null) client.close();
            if (cluster != null) cluster.close();
        }
    }

    private ConnectionResponse testHBaseConnection(ConnectionRequest request, long startTime) {
        try {
            org.apache.hadoop.conf.Configuration config = org.apache.hadoop.hbase.HBaseConfiguration.create();
            config.set("hbase.zookeeper.quorum", request.getHost());
            config.set("hbase.zookeeper.property.clientPort", "2181"); // Default ZK port for HBase in Docker
            try (var conn = org.apache.hadoop.hbase.client.ConnectionFactory.createConnection(config)) {
                conn.getAdmin().listTableNames();
                return ConnectionResponse.builder().success(true).serverVersion("HBase").dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!").responseTimeMs(System.currentTimeMillis() - startTime).build();
            }
        } catch (Exception e) {
            return ConnectionResponse.builder().success(false).message("Bağlantı hatası: " + e.getMessage())
                .responseTimeMs(System.currentTimeMillis() - startTime).build();
        }
    }

    private ConnectionResponse testCouchDbConnection(ConnectionRequest request, long startTime) {
        try {
            String url = "http://" + request.getHost() + ":" + request.getPort() + "/";
            var builder = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET();
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                String auth = request.getUsername() + ":" + request.getPassword();
                builder.header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes()));
            }
            var resp = java.net.http.HttpClient.newHttpClient().send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 400) {
                return ConnectionResponse.builder().success(true).serverVersion("CouchDB").dbType(request.getDbType())
                    .message("Bağlantı testi başarılı!").responseTimeMs(System.currentTimeMillis() - startTime).build();
            } else {
                throw new Exception("HTTP " + resp.statusCode());
            }
        } catch (Exception e) {
            return ConnectionResponse.builder().success(false).message("Bağlantı hatası: " + e.getMessage())
                .responseTimeMs(System.currentTimeMillis() - startTime).build();
        }
    }

    public void disconnect(String userId, String connectionToken) {
        sessions.close(userId, connectionToken);
    }

    private String buildMongoUrl(ConnectionRequest request) {
        String uri = "mongodb://";
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            uri += request.getUsername() + ":" + request.getPassword() + "@";
        }
        uri += request.getHost() + ":" + request.getPort() + "/";
        if (request.getDatabase() != null && !request.getDatabase().isBlank()) {
            uri += "?authSource=" + request.getDatabase();
        }
        return uri;
    }

    /**
     * Veritabanı tipine göre JDBC URL oluşturur.
     */
    private String buildJdbcUrl(ConnectionRequest request) {
        return switch (request.getDbType().toUpperCase()) {
            case "MYSQL" -> String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8&allowMultiQueries=true&rewriteBatchedStatements=true",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            case "POSTGRESQL" -> String.format(
                    "jdbc:postgresql://%s:%d/%s",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            case "MARIADB" -> String.format(
                    "jdbc:mariadb://%s:%d/%s?useSSL=false&characterEncoding=UTF-8&allowMultiQueries=true",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            case "SQLITE" -> "jdbc:sqlite:" + request.getDatabase();
            case "MSSQL" -> String.format(
                    "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false",
                    request.getHost(), request.getPort(), request.getDatabase()
            );
            case "ORACLE" -> String.format(
                    "jdbc:oracle:thin:@//%s:%d/%s",
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
        if (msg.contains("SSL error") || msg.contains("SSLHandshakeException") || msg.contains("PKIX path building failed")) {
            return "SSL bağlantı hatası.";
        }
        return msg;
    }
}

