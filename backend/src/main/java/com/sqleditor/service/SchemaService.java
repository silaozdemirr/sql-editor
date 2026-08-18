package com.sqleditor.service;

import com.sqleditor.model.ColumnInfo;
import com.sqleditor.model.ConnectionRequest;
import com.sqleditor.model.SchemaResponse;
import com.sqleditor.model.SchemaResponse.TableInfo;
import com.sqleditor.model.ErdResponse;
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
    public SchemaResponse getSchema(Connection conn, String databaseName) throws SQLException {

        List<TableInfo> tables = new ArrayList<>();
        List<TableInfo> views  = new ArrayList<>();

        try (conn) {

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
                ps.setString(1, databaseName);

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
                .databaseName(databaseName)
                .tables(tables)
                .views(views)
                .build();
    }

    /**
     * Belirli bir tablonun kolon bilgilerini döner.
     */
    public List<ColumnInfo> getColumns(Connection conn, String databaseName, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();

        try (conn) {

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
                ps.setString(1, databaseName);
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

    /**
     * Belirli bir tablonun DDL (CREATE TABLE) kodunu döner.
     */
    public String getDDL(Connection conn, String tableName) throws SQLException {
        // SQL Injection'ı engellemek için tablo adındaki backtick'leri kaçırıyoruz
        String sql = "SHOW CREATE TABLE `" + tableName.replace("`", "``") + "`";
        try (conn) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(2);
                }
            }
        }
        return "-- DDL bulunamadı";
    }

    /**
     * Veritabanının ERD (Entity Relationship Diagram) verisini döner.
     */
    public ErdResponse getErd(Connection conn, String databaseName) throws SQLException {
        List<ErdResponse.ErdTable> erdTables = new ArrayList<>();
        List<ErdResponse.ErdEdge> erdEdges = new ArrayList<>();

        try (conn) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            List<String> tableNames = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }

            for (String tableName : tableNames) {
                List<ColumnInfo> columns = new ArrayList<>();
                String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, EXTRA " +
                             "FROM information_schema.COLUMNS " +
                             "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, databaseName);
                    ps.setString(2, tableName);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String key = rs.getString("COLUMN_KEY");
                            columns.add(ColumnInfo.builder()
                                    .name(rs.getString("COLUMN_NAME"))
                                    .dataType(rs.getString("DATA_TYPE"))
                                    .fullType(rs.getString("COLUMN_TYPE"))
                                    .nullable("YES".equals(rs.getString("IS_NULLABLE")))
                                    .primaryKey("PRI".equals(key))
                                    .foreignKey("MUL".equals(key))
                                    .unique("UNI".equals(key))
                                    .defaultValue(rs.getString("COLUMN_DEFAULT"))
                                    .extra(rs.getString("EXTRA"))
                                    .build());
                        }
                    }
                }
                erdTables.add(new ErdResponse.ErdTable(tableName, columns));
                
                try (ResultSet rs = metaData.getImportedKeys(databaseName, null, tableName)) {
                    while (rs.next()) {
                        String pkTableName = rs.getString("PKTABLE_NAME");
                        String pkColumnName = rs.getString("PKCOLUMN_NAME");
                        String fkTableName = rs.getString("FKTABLE_NAME");
                        String fkColumnName = rs.getString("FKCOLUMN_NAME");
                        erdEdges.add(new ErdResponse.ErdEdge(fkTableName, fkColumnName, pkTableName, pkColumnName));
                    }
                }
            }
        }
        return new ErdResponse(erdTables, erdEdges);
    }
}
