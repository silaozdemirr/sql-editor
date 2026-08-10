package com.sqleditor.service;

import com.sqleditor.model.ColumnInfo;
import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.SchemaResponse;
import com.sqleditor.model.SchemaResponse.TableInfo;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Veritabanı schema bilgisini çeken servis.
 * information_schema'yı sorgular.
 */
@Service
public class SchemaService {

    /**
     * Veritabanındaki tüm tabloları ve view'ları döner.
     */
    public SchemaResponse getSchema(ConnectionRequest request) throws SQLException {
        String jdbcUrl = buildJdbcUrl(request);

        List<TableInfo> tables = new ArrayList<>();
        List<TableInfo> views  = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(
                jdbcUrl, request.getUsername(), request.getPassword())) {

            String sql = """
                SELECT
                    t.TABLE_NAME,
                    t.TABLE_TYPE,
                    COALESCE(t.TABLE_ROWS, 0) AS TABLE_ROWS
                FROM information_schema.TABLES t
                WHERE t.TABLE_SCHEMA = ?
                ORDER BY t.TABLE_TYPE DESC, t.TABLE_NAME ASC
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, request.getDatabase());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("TABLE_NAME");
                        String type = rs.getString("TABLE_TYPE");
                        int    rows = rs.getInt("TABLE_ROWS");

                        TableInfo info = new TableInfo(name, type, rows);

                        if ("VIEW".equals(type)) {
                            views.add(info);
                        } else {
                            tables.add(info);
                        }
                    }
                }
            }
        }

        return SchemaResponse.builder()
                .databaseName(request.getDatabase())
                .tables(tables)
                .views(views)
                .build();
    }

    /**
     * Belirli bir tablonun kolon bilgilerini döner.
     */
    public List<ColumnInfo> getColumns(ConnectionRequest request, String tableName) throws SQLException {
        String jdbcUrl = buildJdbcUrl(request);
        List<ColumnInfo> columns = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(
                jdbcUrl, request.getUsername(), request.getPassword())) {

            String sql = """
                SELECT
                    c.COLUMN_NAME,
                    c.DATA_TYPE,
                    c.COLUMN_TYPE,
                    c.IS_NULLABLE,
                    c.COLUMN_KEY,
                    c.COLUMN_DEFAULT,
                    c.EXTRA
                FROM information_schema.COLUMNS c
                WHERE c.TABLE_SCHEMA = ?
                  AND c.TABLE_NAME   = ?
                ORDER BY c.ORDINAL_POSITION
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, request.getDatabase());
                ps.setString(2, tableName);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String key = rs.getString("COLUMN_KEY");

                        ColumnInfo col = ColumnInfo.builder()
                                .name(rs.getString("COLUMN_NAME"))
                                .dataType(rs.getString("DATA_TYPE"))
                                .fullType(rs.getString("COLUMN_TYPE"))
                                .nullable("YES".equals(rs.getString("IS_NULLABLE")))
                                .primaryKey("PRI".equals(key))
                                .foreignKey("MUL".equals(key))
                                .unique("UNI".equals(key))
                                .defaultValue(rs.getString("COLUMN_DEFAULT"))
                                .extra(rs.getString("EXTRA"))
                                .build();

                        columns.add(col);
                    }
                }
            }
        }

        return columns;
    }

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
            default -> throw new IllegalArgumentException("Desteklenmeyen tip: " + request.getDbType());
        };
    }
}
