package com.sqleditor.service;

import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.QueryResponse;
import org.springframework.stereotype.Service;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryService {

    private static final int MAX_ROWS = 1_000;

    /** Bağlı veritabanında tek SQL ifadesi çalıştırır. Sonuç satırları 1000 ile sınırlıdır. */
    public QueryResponse execute(ConnectionRequest request, String sql) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection connection = DriverManager.getConnection(buildJdbcUrl(request), request.getUsername(), request.getPassword());
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.setMaxRows(MAX_ROWS + 1);

            boolean hasResultSet = statement.execute(sql);
            long elapsed = System.currentTimeMillis() - start;
            if (!hasResultSet) {
                int updateCount = statement.getUpdateCount();
                return new QueryResponse(List.of(), List.of(), updateCount, false, elapsed,
                        updateCount + " kayıt etkilendi.");
            }

            try (ResultSet resultSet = statement.getResultSet()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int index = 1; index <= columnCount; index++) {
                    columns.add(metaData.getColumnLabel(index));
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
                return new QueryResponse(columns, rows, null, truncated,
                        System.currentTimeMillis() - start, rows.size() + " satır döndü.");
            }
        }
    }

    private String formatValue(Object value) throws SQLException {
        if (value == null) return null;
        if (value instanceof Blob) return "[BINARY]";
        if (value instanceof Clob clob) return clob.getSubString(1, (int) Math.min(clob.length(), 10_000));
        if (value instanceof byte[]) return "[BINARY]";
        return String.valueOf(value);
    }

    private String buildJdbcUrl(ConnectionRequest request) {
        return switch (request.getDbType().toUpperCase()) {
            case "MYSQL" -> String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8",
                    request.getHost(), request.getPort(), request.getDatabase());
            case "POSTGRESQL" -> String.format("jdbc:postgresql://%s:%d/%s",
                    request.getHost(), request.getPort(), request.getDatabase());
            default -> throw new IllegalArgumentException("Desteklenmeyen tip: " + request.getDbType());
        };
    }
}
