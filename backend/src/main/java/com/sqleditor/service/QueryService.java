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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.util.Map;
import java.util.UUID;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.HashMap;

@Service
public class QueryService {

    private final ConcurrentHashMap<String, Statement> activeStatements = new ConcurrentHashMap<>();
    
    public void cancelQuery(String queryId) {
        Statement st = activeStatements.remove(queryId);
        if (st != null) {
            try {
                st.cancel();
            } catch (SQLException ignored) {}
        }
    }


    private static final int MAX_ROWS = 1_000;
    private final JdbcTemplate db;

    public QueryService(JdbcTemplate db) {
        this.db = db;
    }

    
    public int deleteRow(java.sql.Connection connection, String role, com.sqleditor.model.QueryUpdateReq req) throws java.sql.SQLException {
        checkRolePermissions(role, "DELETE");
        if (req.getTableName() == null || req.getTableName().isEmpty()) {
            throw new java.sql.SQLException("Tablo ad tespit edilemedi.");
        }

        String fullTableName = req.getTableName();
        String catalog = connection.getCatalog();
        String schema = null;
        String pureTableName = fullTableName;
        if (fullTableName.contains(".")) {
            String[] parts = fullTableName.split("\\.");
            if (parts.length == 2) {
                catalog = parts[0];
                pureTableName = parts[1];
            } else if (parts.length == 3) {
                catalog = parts[0];
                schema = parts[1];
                pureTableName = parts[2];
            }
        }

        java.util.List<String> pkColumns = new java.util.ArrayList<>();
        try (java.sql.ResultSet pkRs = connection.getMetaData().getPrimaryKeys(catalog, schema, pureTableName)) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        java.util.List<String> whereCols = pkColumns.isEmpty() ? new java.util.ArrayList<>(req.getOldRowValues().keySet()) : pkColumns;
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        String[] parts = fullTableName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            sql.append("`").append(parts[i].replace("`", "``")).append("`");
            if (i < parts.length - 1) sql.append(".");
        }
        sql.append(" WHERE ");

        java.util.List<Object> params = new java.util.ArrayList<>();
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
            int updated = ps.executeUpdate();
            if (updated == 0) throw new java.sql.SQLException("Hibir kayt eletiirilemedi. Veri deimi olabilir veya key uyumsuzlugu.");
            return updated;
        }
    }

    public int updateCell(Connection connection, String role, com.sqleditor.model.QueryUpdateReq req)
            throws SQLException {
        checkRolePermissions(role, "UPDATE");
        if (req.getTableName() == null || req.getTableName().isEmpty()) {
            throw new SQLException("Tablo adı tespit edilemedi. Düzenleme yapılamaz.");
        }

        String fullTableName = req.getTableName();
        String catalog = connection.getCatalog();
        String schema = null;
        String pureTableName = fullTableName;

        if (fullTableName.contains(".")) {
            String[] parts = fullTableName.split("\\.");
            if (parts.length == 2) {
                catalog = parts[0];
                pureTableName = parts[1];
            } else if (parts.length == 3) {
                catalog = parts[0];
                schema = parts[1];
                pureTableName = parts[2];
            }
        }

        List<String> pkColumns = new ArrayList<>();
        try (ResultSet pkRs = connection.getMetaData().getPrimaryKeys(catalog, schema, pureTableName)) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        List<String> whereCols = pkColumns.isEmpty() ? new ArrayList<>(req.getOldRowValues().keySet()) : pkColumns;

        StringBuilder sql = new StringBuilder("UPDATE ");
        String[] parts = fullTableName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            sql.append("`").append(parts[i].replace("`", "``")).append("`");
            if (i < parts.length - 1)
                sql.append(".");
        }
        sql.append(" SET `").append(req.getUpdatedColumn().replace("`", "``")).append("` = ? WHERE ");

        List<Object> params = new ArrayList<>();
        Object newValue = req.getNewValue();
        if ("true".equalsIgnoreCase(req.getNewValue()))
            newValue = 1;
        else if ("false".equalsIgnoreCase(req.getNewValue()))
            newValue = 0;
        params.add(newValue);

        boolean first = true;
        for (String colName : whereCols) {
            String strVal = req.getOldRowValues().get(colName);
            if (!first)
                sql.append(" AND ");
            sql.append("`").append(colName.replace("`", "``")).append("` ");
            if (strVal == null) {
                sql.append("IS NULL");
            } else {
                sql.append("= ?");
                Object paramVal = strVal;
                // WHERE clause için eğer Primary Key yoksa ve boolean değerler geçiyorsa hata
                // vermemesi için
                if (pkColumns.isEmpty()) {
                    if ("true".equalsIgnoreCase(strVal))
                        paramVal = 1;
                    else if ("false".equalsIgnoreCase(strVal))
                        paramVal = 0;
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

    public StreamingResponseBody executeStream(
            java.sql.Connection connection, String dbType, String sql, String queryId, String role, String userId, 
            String connectionId, List<Map<String, String>> filters, List<Map<String, Object>> sorts, 
            Integer limit, Integer offset, Boolean includeCount) throws java.sql.SQLException {
        
        String wrappedSql = sql.trim();
        if (wrappedSql.endsWith(";")) {
            wrappedSql = wrappedSql.substring(0, wrappedSql.length() - 1);
        }
        
        String upperSql = wrappedSql.toUpperCase();
        boolean isSelect = upperSql.startsWith("SELECT") || upperSql.startsWith("WITH");
        
        boolean needsWrapper = isSelect && ((filters != null && !filters.isEmpty()) || (sorts != null && !sorts.isEmpty()) || limit != null || offset != null);
        if (needsWrapper) {
            StringBuilder sb = new StringBuilder("SELECT * FROM ( ");
            sb.append(wrappedSql).append(" ) AS t");
            
            if (filters != null && !filters.isEmpty()) {
                sb.append(" WHERE 1=1 ");
                for (Map<String, String> f : filters) {
                    String col = f.get("id");
                    String val = f.get("value");
                    String type = f.get("type");
                    if (col != null && val != null) {
                        String op = "LIKE";
                        String prefix = "%";
                        String suffix = "%";
                        
                        if ("equals".equalsIgnoreCase(type)) {
                            op = "="; prefix = ""; suffix = "";
                        } else if ("notEqual".equalsIgnoreCase(type)) {
                            op = "!="; prefix = ""; suffix = "";
                        } else if ("greaterThan".equalsIgnoreCase(type)) {
                            op = ">"; prefix = ""; suffix = "";
                        } else if ("greaterThanOrEqual".equalsIgnoreCase(type)) {
                            op = ">="; prefix = ""; suffix = "";
                        } else if ("lessThan".equalsIgnoreCase(type)) {
                            op = "<"; prefix = ""; suffix = "";
                        } else if ("lessThanOrEqual".equalsIgnoreCase(type)) {
                            op = "<="; prefix = ""; suffix = "";
                        } else if ("startsWith".equalsIgnoreCase(type)) {
                            op = "LIKE"; prefix = ""; suffix = "%";
                        } else if ("endsWith".equalsIgnoreCase(type)) {
                            op = "LIKE"; prefix = "%"; suffix = "";
                        }
                        
                        sb.append(" AND `").append(col.replace("`", "``")).append("` ").append(op)
                          .append(" '").append(prefix).append(val.replace("'", "''")).append(suffix).append("'");
                    }
                }
            }
            
            if (sorts != null && !sorts.isEmpty()) {
                sb.append(" ORDER BY ");
                boolean first = true;
                for (Map<String, Object> s : sorts) {
                    if (!first) sb.append(", ");
                    String col = (String) s.get("id");
                    boolean desc = Boolean.parseBoolean(String.valueOf(s.get("desc")));
                    sb.append("`").append(col.replace("`", "``")).append("` ").append(desc ? "DESC" : "ASC");
                    first = false;
                }
            }
            
            if (limit != null) {
                sb.append(" LIMIT ").append(limit);
            }
            if (offset != null) {
                sb.append(" OFFSET ").append(offset);
            }
            wrappedSql = sb.toString();
        }

        final String finalSql = wrappedSql;
        
        return outputStream -> {
            try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)) {
                if (queryId != null) activeStatements.put(queryId, statement);
                
                try {
                    statement.setFetchSize(1000);
                } catch (java.sql.SQLException e) {
                    // ignore
                }
                
                Long totalCount = null;
                if (Boolean.TRUE.equals(includeCount) && isSelect) {
                    String countSql = finalSql;
                    if (countSql.toUpperCase().contains(" LIMIT ")) {
                        countSql = countSql.substring(0, countSql.toUpperCase().lastIndexOf(" LIMIT "));
                    }
                    if (countSql.toUpperCase().contains(" ORDER BY ")) {
                        countSql = countSql.substring(0, countSql.toUpperCase().lastIndexOf(" ORDER BY "));
                    }
                    countSql = "SELECT COUNT(*) FROM (" + countSql + ") AS count_t";
                    try (Statement countStmt = connection.createStatement()) {
                        try (java.sql.ResultSet countRs = countStmt.executeQuery(countSql)) {
                            if (countRs.next()) {
                                totalCount = countRs.getLong(1);
                            }
                        }
                    } catch(Exception ignored) {
                        ignored.printStackTrace();
                    }
                }

                boolean hasResultSet = statement.execute(finalSql);
                if (!hasResultSet) {
                    int count = statement.getUpdateCount();
                    String msg = "{\"__meta\": [\"message\"]}\n{\"message\": \"" + count + " rows affected\"}\n";
                    outputStream.write(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    outputStream.flush();
                    return;
                }
                
                try (java.sql.ResultSet rs = statement.getResultSet()) {


                    java.sql.ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<String> cols = new ArrayList<>();
                    String detectedTable = null;
                    
                    for (int i = 1; i <= columnCount; i++) {
                        cols.add(metaData.getColumnLabel(i));
                        if (detectedTable == null || detectedTable.isEmpty()) {
                            String cat = metaData.getCatalogName(i);
                            String sch = metaData.getSchemaName(i);
                            String tab = metaData.getTableName(i);
                            if (tab != null && !tab.isEmpty()) {
                                if (sch != null && !sch.isEmpty()) detectedTable = sch + "." + tab;
                                else if (cat != null && !cat.isEmpty()) detectedTable = cat + "." + tab;
                                else detectedTable = tab;
                            }
                        }
                    }
                    
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> metaRow = new java.util.HashMap<>();
                    metaRow.put("__meta", cols);
                    if (detectedTable != null) metaRow.put("__tableName", detectedTable);
                    if (totalCount != null) metaRow.put("__totalCount", totalCount);
                    outputStream.write((mapper.writeValueAsString(metaRow) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    outputStream.flush();
                    
                    int count = 0;
                    while (rs.next()) {
                        java.util.Map<String, Object> row = new java.util.HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(cols.get(i - 1), formatValue(rs.getObject(i)));
                        }
                        outputStream.write((mapper.writeValueAsString(row) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        count++;
                        if (count % 5000 == 0) {
                            outputStream.flush();
                        }
                    }
                    outputStream.flush();
                }
            } catch (java.sql.SQLException e) {
                String err = "{\"__error\": \"" + e.getMessage().replace("\"", "\\\"").replace("\n", " ") + "\"}\n";
                try {
                    outputStream.write(err.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (java.io.IOException ioe) {}
            } finally {
                if (queryId != null) activeStatements.remove(queryId);
                // connection should not be closed as it's cached
            }
        };
    }


    public QueryResponse execute(Connection connection, String sql, String role, String userId, String connectionId)
            throws SQLException {
        checkRolePermissions(role, sql);
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMsg = null;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            // statement.setMaxRows(MAX_ROWS + 1);

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
                        String cat = metaData.getCatalogName(index);
                        String sch = metaData.getSchemaName(index);
                        String tab = metaData.getTableName(index);
                        if (tab != null && !tab.isEmpty()) {
                            if (sch != null && !sch.isEmpty())
                                detectedTable = sch + "." + tab;
                            else if (cat != null && !cat.isEmpty())
                                detectedTable = cat + "." + tab;
                            else
                                detectedTable = tab;
                        }
                    }
                }

                List<List<String>> rows = new ArrayList<>();
                boolean truncated = false;
                while (resultSet.next()) {
                    /*if (rows.size() == MAX_ROWS) {
                        truncated = true;
                        break;
                    }*/
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
                try (ResultSet resultSet = statement
                        .executeQuery("SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())")) {
                    List<String> columns = List.of("PLAN_TABLE_OUTPUT");
                    List<List<String>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(List.of(formatValue(resultSet.getObject(1))));
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    return new QueryResponse(columns, rows, null, false, elapsed,
                            "Açıklama (Explain Plan) oluşturuldu.", null);
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
                        return new QueryResponse(columns, rows, null, false, elapsed,
                                "Açıklama (Explain Plan) oluşturuldu.", null);
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
                    return new QueryResponse(columns, rows, null, false, elapsed,
                            "Açıklama (Explain Plan) oluşturuldu.", null);
                }
            }
        }
    }

        void checkRolePermissions(String role, String sql) {
        if (role != null && role.startsWith("ROLE_")) role = role.substring(5);
        String upper = sql.trim().toUpperCase();
        if ("READ_ONLY".equals(role)) {
            if (upper.startsWith("INSERT") || upper.startsWith("UPDATE") || upper.startsWith("DELETE") ||
                    upper.startsWith("DROP") || upper.startsWith("TRUNCATE") || upper.startsWith("CREATE") ||
                    upper.startsWith("ALTER") || upper.startsWith("GRANT") || upper.startsWith("REVOKE")) {
                throw new SecurityException(
                        "READ_ONLY yetkisine sahipsiniz. Sadece SELECT (okuma) sorguları çalıştırabilirsiniz.");
            }
        } else if ("EDITOR".equals(role)) {
            if (upper.startsWith("DROP") || upper.startsWith("TRUNCATE") || upper.startsWith("CREATE") ||
                    upper.startsWith("ALTER") || upper.startsWith("GRANT") || upper.startsWith("REVOKE")) {
                throw new SecurityException(
                        "EDITOR yetkisine sahipsiniz. Tablo oluşturma, silme veya kolon ismi değiştirme (CREATE, DROP, ALTER vb.) gibi şema değişiklikleri yapamazsınız.");
            }
        }
    }

    private void logHistory(String userId, String connectionId, String sql, long elapsed, String status, String error) {
        db.update(
                "INSERT INTO query_history (id, user_id, connection_id, query_text, execution_time_ms, status, error_message) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), userId, connectionId, sql, elapsed, status, error);
    }

    public QueryResponse executeMongo(MongoClient mongo, String commandJson, String role, String userId,
            String connectionId) throws Exception {
        long start = System.currentTimeMillis();
        Document cmd;
        try {
            cmd = Document.parse(commandJson);
        } catch (Exception e) {
            throw new Exception(
                    "Geçersiz JSON sorgusu. MongoDB için geçerli bir JSON objesi girin.\nÖrnek: {\"db\": \"veritabani\", \"find\": \"koleksiyon\"}");
        }

        String dbName = cmd.getString("db");
        if (dbName == null) {
            throw new Exception(
                    "Sorguda 'db' alanı eksik. Lütfen hedef veritabanını belirtin (Örn: {\"db\": \"test\"}).");
        }

        cmd.remove("db");
        MongoDatabase mdb = mongo.getDatabase(dbName);

        if (cmd.containsKey("find")) {
            String collection = cmd.getString("find");
            Document filter = cmd.get("filter", Document.class);
            if (filter == null)
                filter = new Document();

            List<Document> docs = new ArrayList<>();
            mdb.getCollection(collection).find(filter).into(docs);

            List<List<String>> rows = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            for (Document doc : docs) {
                for (String key : doc.keySet()) {
                    if (!columns.contains(key))
                        columns.add(key);
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
            // connection_id filter removed so history persists across different connection
            // sessions
            return db.queryForList(
                    "SELECT query_text, execution_time_ms, status, CAST(created_at AS CHAR) as created_at FROM query_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 50",
                    userId);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    String formatValue(Object value) throws SQLException {
        if (value == null)
            return null;
        if (value instanceof Blob)
            return "[BINARY]";
        if (value instanceof Clob clob)
            return clob.getSubString(1, (int) Math.min(clob.length(), 10_000));
        if (value instanceof byte[])
            return "[BINARY]";
        return String.valueOf(value);
    }

    public QueryResponse executeRedis(redis.clients.jedis.JedisPooled redisClient, String command, String role,
            String userId, String connectionId) {
        long start = System.currentTimeMillis();
        try {
            // Assume command is a simple GET key or raw command JSON. We'll do a simple raw
            // execute
            Object result;
            if (command.startsWith("{")) {
                // Not supported for this basic iteration
                result = "Redis JSON commands not fully supported. Send plain text like GET key";
            } else {
                String[] parts = command.split(" ");
                result = redisClient.sendCommand(
                        () -> redis.clients.jedis.util.SafeEncoder.encode(parts[0]),
                        java.util.Arrays.stream(parts).skip(1).map(redis.clients.jedis.util.SafeEncoder::encode)
                                .toArray(byte[][]::new));
            }
            long elapsed = System.currentTimeMillis() - start;
            List<String> columns = List.of("Result");
            List<List<String>> rows = List.of(List.of(result != null ? new String((byte[]) result) : "null"));
            logHistory(userId, connectionId, command, elapsed, "SUCCESS", null);
            return new QueryResponse(columns, rows, null, true, elapsed, "Komut çalıştırıldı.", null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, command, elapsed, "ERROR", e.getMessage());
            throw new RuntimeException("Redis hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeCassandra(com.datastax.oss.driver.api.core.CqlSession cassandra, String sql,
            String role, String userId, String connectionId) {
        long start = System.currentTimeMillis();
        checkRolePermissions(role, sql);
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

    public QueryResponse executeMemcached(net.spy.memcached.MemcachedClient memcached, String command, String role,
            String userId, String connectionId) {
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

    public QueryResponse executeNeo4j(org.neo4j.driver.Driver neo4j, String sql, String role, String userId,
            String connectionId) {
        long start = System.currentTimeMillis();
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

    public QueryResponse executeDynamoDb(software.amazon.awssdk.services.dynamodb.DynamoDbClient client, String sql,
            String role, String userId, String connectionId) {
        long start = System.currentTimeMillis();
        try {
            // Very simplified mock execution
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            return new QueryResponse(List.of("Result"), List.of(List.of("DynamoDB query simulated: " + sql)), null,
                    false, elapsed, null, null);
        } catch (Exception e) {
            logHistory(userId, connectionId, sql, System.currentTimeMillis() - start, "ERROR", e.getMessage());
            throw new RuntimeException("DynamoDB hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeArangoDb(com.arangodb.ArangoDB client, String sql, String role, String userId,
            String connectionId) {
        long start = System.currentTimeMillis();
        try {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            return new QueryResponse(List.of("Result"), List.of(List.of("ArangoDB query simulated: " + sql)), null,
                    false, elapsed, null, null);
        } catch (Exception e) {
            logHistory(userId, connectionId, sql, System.currentTimeMillis() - start, "ERROR", e.getMessage());
            throw new RuntimeException("ArangoDB hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeNeptune(org.apache.tinkerpop.gremlin.driver.Client client, String sql, String role,
            String userId, String connectionId) {
        long start = System.currentTimeMillis();
        try {
            List<org.apache.tinkerpop.gremlin.driver.Result> results = client.submit(sql).all().get();
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            List<List<String>> rows = new ArrayList<>();
            for (var r : results)
                rows.add(List.of(r.getString()));
            return new QueryResponse(List.of("Result"), rows, null, false, elapsed, null, null);
        } catch (Exception e) {
            logHistory(userId, connectionId, sql, System.currentTimeMillis() - start, "ERROR", e.getMessage());
            throw new RuntimeException("Neptune hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeHBase(org.apache.hadoop.hbase.client.Connection client, String sql, String role,
            String userId, String connectionId) {
        long start = System.currentTimeMillis();
        try {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            return new QueryResponse(List.of("Result"), List.of(List.of("HBase query simulated: " + sql)), null, false,
                    elapsed, null, null);
        } catch (Exception e) {
            logHistory(userId, connectionId, sql, System.currentTimeMillis() - start, "ERROR", e.getMessage());
            throw new RuntimeException("HBase hatası: " + e.getMessage());
        }
    }

    public QueryResponse executeCouchDb(String[] clientData, String sql, String role, String userId,
            String connectionId) {
        long start = System.currentTimeMillis();
        try {
            long elapsed = System.currentTimeMillis() - start;
            logHistory(userId, connectionId, sql, elapsed, "SUCCESS", null);
            return new QueryResponse(List.of("Result"), List.of(List.of("CouchDB query simulated: " + sql)), null,
                    false, elapsed, null, null);
        } catch (Exception e) {
            logHistory(userId, connectionId, sql, System.currentTimeMillis() - start, "ERROR", e.getMessage());
            throw new RuntimeException("CouchDB hatası: " + e.getMessage());
        }
    }
}
