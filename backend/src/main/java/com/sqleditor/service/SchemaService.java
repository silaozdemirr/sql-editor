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
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName().toLowerCase();

            // Oracle ve PostgreSQL gibi sistemlerde catalog null dönebilir, schema/kullanıcı adını kullan
            String actualDbName = databaseName;
            if (actualDbName == null || actualDbName.isEmpty()) {
                actualDbName = conn.getSchema();
            }
            if (actualDbName == null || actualDbName.isEmpty()) {
                actualDbName = metaData.getUserName();
            }

            if (dbProductName.contains("oracle")) {
                // Oracle Database
                try (ResultSet rs = metaData.getTables(null, actualDbName.toUpperCase(), "%", new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        String name = rs.getString("TABLE_NAME");
                        String type = rs.getString("TABLE_TYPE");
                        TableInfo info = new TableInfo(name, type, 0);
                        if ("VIEW".equalsIgnoreCase(type)) {
                            views.add(info);
                        } else {
                            tables.add(info);
                        }
                    }
                }
            } else if (dbProductName.contains("microsoft") || dbProductName.contains("sql server")) {
                // SQL Server
                String sql = """
                    SELECT
                        t.TABLE_NAME,
                        t.TABLE_TYPE,
                        0 AS TABLE_ROWS
                    FROM information_schema.TABLES t
                    WHERE t.TABLE_CATALOG = ?
                    ORDER BY t.TABLE_TYPE DESC, t.TABLE_NAME ASC
                    """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, actualDbName);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("TABLE_NAME");
                            String type = rs.getString("TABLE_TYPE");

                            TableInfo info = new TableInfo(name, type, 0);
                            if ("VIEW".equals(type)) {
                                views.add(info);
                            } else {
                                tables.add(info);
                            }
                        }
                    }
                }
            } else if (dbProductName.contains("postgresql")) {
                // PostgreSQL
                String sql = """
                    SELECT
                        table_name AS TABLE_NAME,
                        table_type AS TABLE_TYPE,
                        0 AS TABLE_ROWS
                    FROM information_schema.tables
                    WHERE table_catalog = ? AND table_schema NOT IN ('information_schema', 'pg_catalog')
                    ORDER BY table_type DESC, table_name ASC
                    """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, databaseName);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("TABLE_NAME");
                            String type = rs.getString("TABLE_TYPE");
                            TableInfo info = new TableInfo(name, type, 0);
                            if ("VIEW".equalsIgnoreCase(type)) {
                                views.add(info);
                            } else {
                                tables.add(info);
                            }
                        }
                    }
                }
            } else {
                // Default / MySQL
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
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName().toLowerCase();

            String actualDbName = databaseName;
            if (actualDbName == null || actualDbName.isEmpty()) {
                actualDbName = conn.getSchema();
            }
            if (actualDbName == null || actualDbName.isEmpty()) {
                actualDbName = metaData.getUserName();
            }

            if (dbProductName.contains("oracle")) {
                String sql = """
                    SELECT
                        c.COLUMN_NAME,
                        c.DATA_TYPE,
                        c.DATA_TYPE AS COLUMN_TYPE,
                        c.NULLABLE,
                        '' AS COLUMN_KEY,
                        c.DATA_DEFAULT AS COLUMN_DEFAULT,
                        '' AS EXTRA
                    FROM ALL_TAB_COLUMNS c
                    WHERE c.OWNER = ?
                      AND c.TABLE_NAME = ?
                    ORDER BY c.COLUMN_ID
                    """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, actualDbName.toUpperCase());
                    ps.setString(2, tableName.toUpperCase());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ColumnInfo col = ColumnInfo.builder()
                                    .name(rs.getString("COLUMN_NAME"))
                                    .dataType(rs.getString("DATA_TYPE"))
                                    .fullType(rs.getString("COLUMN_TYPE"))
                                    .nullable("Y".equals(rs.getString("NULLABLE")))
                                    .primaryKey(false) // Tam doğruluk için constraint tablolarına bakılmalı
                                    .foreignKey(false)
                                    .unique(false)
                                    .defaultValue(rs.getString("COLUMN_DEFAULT"))
                                    .extra(rs.getString("EXTRA"))
                                    .build();
                            columns.add(col);
                        }
                    }
                }
            } else if (dbProductName.contains("microsoft") || dbProductName.contains("sql server")) {
                // SQL Server
                String sql = """
                    SELECT
                        c.COLUMN_NAME,
                        c.DATA_TYPE,
                        c.DATA_TYPE AS COLUMN_TYPE,
                        c.IS_NULLABLE,
                        '' AS COLUMN_KEY,
                        c.COLUMN_DEFAULT,
                        '' AS EXTRA
                    FROM information_schema.COLUMNS c
                    WHERE c.TABLE_CATALOG = ?
                      AND c.TABLE_NAME   = ?
                    ORDER BY c.ORDINAL_POSITION
                    """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, actualDbName);
                    ps.setString(2, tableName);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ColumnInfo col = ColumnInfo.builder()
                                    .name(rs.getString("COLUMN_NAME"))
                                    .dataType(rs.getString("DATA_TYPE"))
                                    .fullType(rs.getString("COLUMN_TYPE"))
                                    .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                                    .primaryKey(false)
                                    .defaultValue(rs.getString("COLUMN_DEFAULT"))
                                    .extra(rs.getString("EXTRA"))
                                    .build();
                            columns.add(col);
                        }
                    }
                }
            } else if (dbProductName.contains("postgresql")) {
                String sql = """
                    SELECT
                        column_name AS COLUMN_NAME,
                        data_type AS DATA_TYPE,
                        data_type AS COLUMN_TYPE,
                        is_nullable AS IS_NULLABLE,
                        '' AS COLUMN_KEY,
                        column_default AS COLUMN_DEFAULT,
                        '' AS EXTRA
                    FROM information_schema.columns
                    WHERE table_catalog = ? AND table_name = ?
                    ORDER BY ordinal_position
                    """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, databaseName);
                    ps.setString(2, tableName);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ColumnInfo col = ColumnInfo.builder()
                                    .name(rs.getString("COLUMN_NAME"))
                                    .dataType(rs.getString("DATA_TYPE"))
                                    .fullType(rs.getString("COLUMN_TYPE"))
                                    .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                                    .primaryKey(false)
                                    .defaultValue(rs.getString("COLUMN_DEFAULT"))
                                    .extra(rs.getString("EXTRA"))
                                    .build();
                            columns.add(col);
                        }
                    }
                }
            } else {
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
            // Oracle için catalog genelde null, schema ise kullanıcı adıdır.
            // Fakat burada genel geçerlilik için hem catalog hem schema olarak databaseName verebiliriz,
            // veya JDBC driver'ın varsayılan davranışını kullanması için null, null verebiliriz.
            // En güvenlisi MySQL için catalog, Oracle için schema'ya denk gelecek şekilde arama yapmaktır.
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String tableCat = rs.getString("TABLE_CAT");
                    String tableSchem = rs.getString("TABLE_SCHEM");
                    String tableName = rs.getString("TABLE_NAME");
                    String tableType = rs.getString("TABLE_TYPE");
                    
                    // Eğer databaseName belirtilmişse, ve cat/schema ile uyuşmuyorsa filtrele
                    if (databaseName != null && !databaseName.isEmpty()) {
                        if ((tableCat != null && !tableCat.equalsIgnoreCase(databaseName)) &&
                            (tableSchem != null && !tableSchem.equalsIgnoreCase(databaseName))) {
                            // Oracle sistem tablolarını veya diğer schemaları atla
                            if (!databaseName.equalsIgnoreCase(tableSchem)) {
                                continue;
                            }
                        }
                    }
                    
                    if ("TABLE".equalsIgnoreCase(tableType)) {
                        tableNames.add(tableName);
                    }
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
