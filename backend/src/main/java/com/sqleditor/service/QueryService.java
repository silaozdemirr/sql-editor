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

    public List<java.util.Map<String, Object>> getHistory(String userId, String connectionId) {
        try {
            return db.queryForList("SELECT query_text, execution_time_ms, status, CAST(created_at AS CHAR) as created_at FROM query_history WHERE user_id = ? AND connection_id = ? ORDER BY created_at DESC LIMIT 50", userId, connectionId);
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
}
