package com.sqleditor.service;

import com.sqleditor.model.QueryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.HashMap;

@Service
public class QueryService {

    private static final int MAX_ROWS = 1_000;
    private final JdbcTemplate db;

    public QueryService(JdbcTemplate db) {
        this.db = db;
    }

    public int updateCell(Connection connection, String role, com.sqleditor.model.QueryUpdateReq req) throws SQLException {
        checkReadOnly(role, "UPDATE");
        if (req.getTableName() == null || req.getTableName().isEmpty()) {
            throw new SQLException("Tablo adı tespit edilemedi. Düzenleme yapılamaz.");
        }

        List<String> pkColumns = new ArrayList<>();
        try (ResultSet pkRs = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), null, req.getTableName())) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        List<String> whereCols = pkColumns.isEmpty() ? new ArrayList<>(req.getOldRowValues().keySet()) : pkColumns;

        StringBuilder sql = new StringBuilder("UPDATE `")
                .append(req.getTableName().replace("`", "``")).append("` SET `")
                .append(req.getUpdatedColumn().replace("`", "``")).append("` = ? WHERE ");

        List<Object> params = new ArrayList<>();
        Object newValue = req.getNewValue();
        if ("true".equalsIgnoreCase(req.getNewValue())) newValue = 1;
        else if ("false".equalsIgnoreCase(req.getNewValue())) newValue = 0;
        params.add(newValue);

        boolean first = true;
        for (String colName : whereCols) {
            String strVal = req.getOldRowValues().get(colName);
            if (!first) sql.append(" AND ");
            sql.append("`").append(colName.replace("`", "``")).append("` ");
            if (strVal == null) {
                sql.append("IS NULL");
            } else {
                sql.append("= ?");
                Object paramVal = strVal;
                // WHERE clause için eğer Primary Key yoksa ve boolean değerler geçiyorsa hata vermemesi için
                if (pkColumns.isEmpty()) {
                    if ("true".equalsIgnoreCase(strVal)) paramVal = 1;
                    else if ("false".equalsIgnoreCase(strVal)) paramVal = 0;
                }
                params.add(paramVal);
            }
            first = false;
        }

        sql.append(" LIMIT 1");

        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        }
    }

    public QueryResponse execute(Connection connection, String sql, String role, String userId, String connectionId) throws SQLException {
        checkReadOnly(role, sql);
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMsg = null;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.setMaxRows(MAX_ROWS + 1);

            boolean hasResultSet = statement.execute(sql);
            long elapsed = System.currentTimeMillis() - start;
            if (!hasResultSet) {
                int updateCount = statement.getUpdateCount();
                logHistory(userId, connectionId, sql, elapsed, status, null);
                return new QueryResponse(List.of(), List.of(), updateCount, false, elapsed,
                        updateCount + " kayıt etkilendi.", null);
            }

            try (ResultSet resultSet = statement.getResultSet()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columns = new ArrayList<>();
                String detectedTable = null;
                for (int index = 1; index <= columnCount; index++) {
                    columns.add(metaData.getColumnLabel(index));
                    if (detectedTable == null || detectedTable.isEmpty()) {
                        detectedTable = metaData.getTableName(index);
                    }
                }

                List<List<String>> rows = new ArrayList<>();
                boolean truncated = false;
                while (resultSet.next()) {
                    if (rows.size() == MAX_ROWS) {
                        truncated = true;
                        break;
                    }
                    List<String> row = new ArrayList<>();
                    for (int index = 1; index <= columnCount; index++) {
                        row.add(formatValue(resultSet.getObject(index)));
                    }
                    rows.add(row);
                }
                logHistory(userId, connectionId, sql, elapsed, status, null);
                return new QueryResponse(columns, rows, null, truncated,
                        System.currentTimeMillis() - start, rows.size() + " satır döndü.", detectedTable);
            }
        } catch (SQLException e) {
            status = "ERROR";
            errorMsg = e.getMessage();
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, status, errorMsg);
            throw e;
        }
    }

    public QueryResponse explain(Connection connection, String sql) throws SQLException {
        String dbProductName = connection.getMetaData().getDatabaseProductName().toLowerCase();
        boolean isOracle = dbProductName.contains("oracle");
        boolean isSqlServer = dbProductName.contains("microsoft") || dbProductName.contains("sql server");

        long start = System.currentTimeMillis();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);

            if (isOracle) {
                // Oracle: Önce EXPLAIN PLAN FOR çalıştırılır
                statement.execute("EXPLAIN PLAN FOR " + sql);
                // Ardından DBMS_XPLAN ile sonuç okunur
                try (ResultSet resultSet = statement.executeQuery("SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())")) {
                    List<String> columns = List.of("PLAN_TABLE_OUTPUT");
                    List<List<String>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(List.of(formatValue(resultSet.getObject(1))));
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    return new QueryResponse(columns, rows, null, false, elapsed, "Açıklama (Explain Plan) oluşturuldu.", null);
                }
            } else if (isSqlServer) {
                // MSSQL: SET SHOWPLAN_ALL ON çalıştırılır
                statement.execute("SET SHOWPLAN_ALL ON");
                try {
                    try (ResultSet resultSet = statement.executeQuery(sql)) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        List<String> columns = new ArrayList<>();
                        for (int index = 1; index <= columnCount; index++) {
                            columns.add(metaData.getColumnLabel(index));
                        }

                        List<List<String>> rows = new ArrayList<>();
                        while (resultSet.next()) {
                            List<String> row = new ArrayList<>();
                            for (int index = 1; index <= columnCount; index++) {
                                row.add(formatValue(resultSet.getObject(index)));
                            }
                            rows.add(row);
                        }
                        long elapsed = System.currentTimeMillis() - start;
                        return new QueryResponse(columns, rows, null, false, elapsed, "Açıklama (Explain Plan) oluşturuldu.", null);
                    }
                } finally {
                    statement.execute("SET SHOWPLAN_ALL OFF");
                }
            } else {
                // MySQL / PostgreSQL default behavior
                String explainSql = "EXPLAIN " + sql;
                try (ResultSet resultSet = statement.executeQuery(explainSql)) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<String> columns = new ArrayList<>();
                    for (int index = 1; index <= columnCount; index++) {
                        columns.add(metaData.getColumnLabel(index));
                    }

                    List<List<String>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        List<String> row = new ArrayList<>();
                        for (int index = 1; index <= columnCount; index++) {
                            row.add(formatValue(resultSet.getObject(index)));
                        }
                        rows.add(row);
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    return new QueryResponse(columns, rows, null, false, elapsed, "Açıklama (Explain Plan) oluşturuldu.", null);
                }
            }
        }
    }

    void checkReadOnly(String role, String sql) {
        if ("READ_ONLY".equals(role)) {
            String upper = sql.trim().toUpperCase();
            if (upper.startsWith("INSERT") || upper.startsWith("UPDATE") || upper.startsWith("DELETE") || 
                upper.startsWith("DROP") || upper.startsWith("TRUNCATE") || upper.startsWith("CREATE") || 
                upper.startsWith("ALTER") || upper.startsWith("GRANT") || upper.startsWith("REVOKE")) {
                throw new SecurityException("Read-Only yetkisine sahipsiniz. Sadece SELECT sorguları çalıştırabilirsiniz.");
            }
        }
    }

    private void logHistory(String userId, String connectionId, String sql, long elapsed, String status, String error) {
        db.update("INSERT INTO query_history (id, user_id, connection_id, query_text, execution_time_ms, status, error_message) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), userId, connectionId, sql, elapsed, status, error);
    }

    public QueryResponse executeMongo(MongoClient mongo, String commandJson, String role, String userId, String connectionId) throws Exception {
        long start = System.currentTimeMillis();
        Document cmd;
        try {
            cmd = Document.parse(commandJson);
        } catch (Exception e) {
            throw new Exception("Geçersiz JSON sorgusu. MongoDB için geçerli bir JSON objesi girin.\nÖrnek: {\"db\": \"veritabani\", \"find\": \"koleksiyon\"}");
        }
        
        String dbName = cmd.getString("db");
        if (dbName == null) {
            throw new Exception("Sorguda 'db' alanı eksik. Lütfen hedef veritabanını belirtin (Örn: {\"db\": \"test\"}).");
        }
        
        cmd.remove("db");
        MongoDatabase mdb = mongo.getDatabase(dbName);
        
        if (cmd.containsKey("find")) {
            String collection = cmd.getString("find");
            Document filter = cmd.get("filter", Document.class);
            if (filter == null) filter = new Document();
            
            List<Document> docs = new ArrayList<>();
            mdb.getCollection(collection).find(filter).limit(MAX_ROWS).into(docs);
            
            List<List<String>> rows = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            
            for (Document doc : docs) {
                for (String key : doc.keySet()) {
                    if (!columns.contains(key)) columns.add(key);
                }
            }
            for (Document doc : docs) {
                List<String> row = new ArrayList<>();
                for (String col : columns) {
                    row.add(doc.get(col) != null ? doc.get(col).toString() : null);
                }
                rows.add(row);
            }
            
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, commandJson, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, true, elapsed, null, null);
        } else {
            Document result = mdb.runCommand(cmd);
            long elapsed = System.currentTimeMillis() - start;
            List<String> columns = List.of("Result");
            List<List<String>> rows = List.of(List.of(result.toJson()));
            
            logHistory(userId, connectionId, commandJson, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, true, elapsed, "Komut başarıyla çalıştırıldı.", null);
        }
    }

    public List<java.util.Map<String, Object>> getHistory(String userId, String connectionId) {
        try {
            // connection_id filter removed so history persists across different connection sessions
            return db.queryForList("SELECT query_text, execution_time_ms, status, CAST(created_at AS CHAR) as created_at FROM query_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 50", userId);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    String formatValue(Object value) throws SQLException {
        if (value == null) return null;
        if (value instanceof Blob) return "[BINARY]";
        if (value instanceof Clob clob) return clob.getSubString(1, (int) Math.min(clob.length(), 10_000));
        if (value instanceof byte[]) return "[BINARY]";
        return String.valueOf(value);
    }

    public QueryResponse executeRedis(redis.clients.jedis.JedisPooled redisClient, String command, String role, String userId, String connectionId) {
        long start = System.currentTimeMillis();
        try {
            // Assume command is a simple GET key or raw command JSON. We'll do a simple raw execute
            Object result;
            if (command.startsWith("{")) {
                // Not supported for this basic iteration
                result = "Redis JSON commands not fully supported. Send plain text like GET key";
            } else {
                String[] parts = command.split(" ");
                result = redisClient.sendCommand(
                    () -> redis.clients.jedis.util.SafeEncoder.encode(parts[0]),
                    java.util.Arrays.stream(parts).skip(1).map(redis.clients.jedis.util.SafeEncoder::encode).toArray(byte[][]::new)
                );
            }
            long elapsed = System.currentTimeMillis() - start;
            List<String> columns = List.of("Result");
            List<List<String>> rows = List.of(List.of(result != null ? new String((byte[])result) : "null"));
            logHistory(userId, connectionId, command, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, true, elapsed, "Komut çalıştırıldı.", null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, command, elapsed, "ERROR", e.getMessage());
            throw new RuntimeException("Redis hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeCassandra(com.datastax.oss.driver.api.core.CqlSession cassandra, String sql, String role, String userId, String connectionId) {
        long start = System.currentTimeMillis();
        checkReadOnly(role, sql);
        try {
            com.datastax.oss.driver.api.core.cql.ResultSet rs = cassandra.execute(sql);
            long elapsed = System.currentTimeMillis() - start;
            
            List<String> columns = new ArrayList<>();
            rs.getColumnDefinitions().forEach(col -> columns.add(col.getName().toString()));
            
            List<List<String>> rows = new ArrayList<>();
            rs.forEach(row -> {
                List<String> r = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    Object val = row.getObject(i);
                    r.add(val != null ? val.toString() : null);
                }
                rows.add(r);
            });
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, false, elapsed, null, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "ERROR", e.getMessage());
            throw new RuntimeException("Cassandra hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeMemcached(net.spy.memcached.MemcachedClient memcached, String command, String role, String userId, String connectionId) {
        long start = System.currentTimeMillis();
        try {
            Object result = null;
            if (command.toUpperCase().startsWith("GET ")) {
                String key = command.substring(4).trim();
                result = memcached.get(key);
            } else if (command.toUpperCase().startsWith("SET ")) {
                String[] parts = command.substring(4).trim().split(" ", 2);
                if (parts.length == 2) {
                    memcached.set(parts[0], 3600, parts[1]);
                    result = "OK";
                }
            } else {
                result = "Sadece basit GET ve SET (örn: GET mykey, SET mykey myval) desteklenmektedir.";
            }
            long elapsed = System.currentTimeMillis() - start;
            List<String> columns = List.of("Result");
            List<List<String>> rows = List.of(List.of(result != null ? result.toString() : "null"));
            logHistory(userId, connectionId, command, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, true, elapsed, "Memcached komutu çalıştırıldı.", null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, command, elapsed, "ERROR", e.getMessage());
            throw new RuntimeException("Memcached hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeNeo4j(org.neo4j.driver.Driver neo4j, String sql, String role, String userId, String connectionId) {
        long start = System.currentTimeMillis();
        // checkPermissions logic skipped since Cypher isn't SQL, but read-only enforcement can be done similarly if needed
        try (org.neo4j.driver.Session session = neo4j.session()) {
            org.neo4j.driver.Result result = session.run(sql);
            long elapsed = System.currentTimeMillis() - start;
            
            List<String> columns = result.keys();
            List<List<String>> rows = new ArrayList<>();
            result.list().forEach(record -> {
                List<String> r = new ArrayList<>();
                for (String key : columns) {
                    org.neo4j.driver.Value val = record.get(key);
                    r.add(val != null && !val.isNull() ? val.toString() : null);
                }
                rows.add(r);
            });
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, false, elapsed, null, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "ERROR", e.getMessage());
            throw new RuntimeException("Neo4j hatası: " + e.getMessage());
        }
    }
}
