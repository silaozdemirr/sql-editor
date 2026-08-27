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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import redis.clients.jedis.JedisPooled;
import com.datastax.oss.driver.api.core.CqlSession;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import net.spy.memcached.MemcachedClient;

/**
 * Veritabanı schema bilgisini çeken servis.
 * information_schema'yı sorgular.
 */
@Service
public class SchemaService {

    /**
     * Bağlantıdaki mevcut veritabanlarını (katalogları/şemaları) listeler.
     */
    public List<String> getDatabases(Connection conn) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (conn) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName().toLowerCase();

            if (dbProductName.contains("oracle")) {
                // Oracle: Şemalar genellikle user'lardır
                try (ResultSet rs = conn.createStatement().executeQuery("SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME")) {
                    while (rs.next()) {
                        databases.add(rs.getString(1));
                    }
                } catch (Exception e) {
                    try (ResultSet rs = metaData.getSchemas()) {
                        while (rs.next()) {
                            databases.add(rs.getString("TABLE_SCHEM"));
                        }
                    }
                }
            } else if (dbProductName.contains("postgresql")) {
                // PostgreSQL: pg_database tablosundan çek
                try (ResultSet rs = conn.createStatement().executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname")) {
                    while (rs.next()) {
                        databases.add(rs.getString(1));
                    }
                }
            } else if (dbProductName.contains("sqlite")) {
                databases.add("main");
            } else {
                // MySQL, SQL Server vb.: getCatalogs() standarttır
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        databases.add(rs.getString("TABLE_CAT"));
                    }
                }
            }
        }
        return databases;
    }

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
            } else if (dbProductName.contains("sqlite")) {
                String sql = "SELECT name AS TABLE_NAME, type AS TABLE_TYPE, 0 AS TABLE_ROWS FROM sqlite_master WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%' ORDER BY type DESC, name ASC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("TABLE_NAME");
                            String type = rs.getString("TABLE_TYPE");
                            TableInfo info = new TableInfo(name, type, 0);
                            if ("view".equalsIgnoreCase(type)) {
                                views.add(info);
                            } else {
                                tables.add(info);
                            }
                        }
                    }
                }
            } else {
                // Default / MySQL / MariaDB
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
            } else if (dbProductName.contains("sqlite")) {
                String sql = "PRAGMA table_info('" + tableName.replace("'", "''") + "')";
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        String dataType = rs.getString("type");
                        boolean notNull = rs.getInt("notnull") == 1;
                        boolean pk = rs.getInt("pk") == 1;
                        String dflt_value = rs.getString("dflt_value");

                        ColumnInfo col = ColumnInfo.builder()
                                .name(name)
                                .dataType(dataType)
                                .fullType(dataType)
                                .nullable(!notNull)
                                .primaryKey(pk)
                                .defaultValue(dflt_value)
                                .build();
                        columns.add(col);
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
    public String getDDL(Connection conn, String database, String tableName) throws SQLException {
        String safeTable = tableName.replace("`", "``");
        String sql;
        if (database != null && !database.trim().isEmpty()) {
            String safeDb = database.replace("`", "``");
            sql = "SHOW CREATE TABLE `" + safeDb + "`.`" + safeTable + "`";
        } else {
            sql = "SHOW CREATE TABLE `" + safeTable + "`";
        }
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
                try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                    while (rs.next()) {
                        columns.add(ColumnInfo.builder()
                                .name(rs.getString("COLUMN_NAME"))
                                .dataType(rs.getString("TYPE_NAME"))
                                .fullType(rs.getString("TYPE_NAME"))
                                .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                                .primaryKey(false)
                                .defaultValue(rs.getString("COLUMN_DEF"))
                                .build());
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
    public String getSchemaSummary(Connection conn, String dbType, String databaseName) {
        StringBuilder summary = new StringBuilder();
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            String actualDbName = databaseName;
            if (actualDbName == null || actualDbName.isEmpty()) actualDbName = conn.getSchema();
            if (actualDbName == null || actualDbName.isEmpty()) actualDbName = metaData.getUserName();

            String schemaPattern = null;
            if (dbType.equalsIgnoreCase("ORACLE") && actualDbName != null) {
                schemaPattern = actualDbName.toUpperCase();
            }

            try (ResultSet rs = metaData.getTables(null, schemaPattern, "%", new String[]{"TABLE", "VIEW"})) {
                int tableCount = 0;
                while (rs.next() && tableCount < 100) { // Sınır koyalım ki token limitini aşmasın
                    String tableName = rs.getString("TABLE_NAME");
                    summary.append("Tablo: ").append(tableName).append(" (");
                    
                    try (ResultSet colRs = metaData.getColumns(null, schemaPattern, tableName, "%")) {
                        boolean first = true;
                        while (colRs.next()) {
                            if (!first) summary.append(", ");
                            summary.append(colRs.getString("COLUMN_NAME")).append(" ")
                                   .append(colRs.getString("TYPE_NAME"));
                            first = false;
                        }
                    }
                    summary.append(")\n");
                    tableCount++;
                }
            }
        } catch (SQLException e) {
            System.err.println("Schema summary error: " + e.getMessage());
        }
        return summary.toString();
    }

    public List<String> getMongoDatabases(MongoClient mongo) {
        List<String> dbs = new ArrayList<>();
        try {
            MongoIterable<String> dbNames = mongo.listDatabaseNames();
            for (String name : dbNames) {
                dbs.add(name);
            }
        } catch (Exception e) {
            dbs.add("admin");
            dbs.add("local");
        }
        return dbs;
    }

    public SchemaResponse getMongoSchema(MongoClient mongo, String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            databaseName = "test"; // default
        }
        MongoDatabase db = mongo.getDatabase(databaseName);
        List<TableInfo> tables = new ArrayList<>();
        try {
            for (String collName : db.listCollectionNames()) {
                tables.add(new TableInfo(collName, "TABLE", 0));
            }
        } catch (Exception e) {
            // Ignored, maybe unauthorized
        }
        return SchemaResponse.builder()
                .databaseName(databaseName)
                .tables(tables)
                .views(List.of())
                .build();
    }

    public List<ColumnInfo> getMongoColumns(MongoClient mongo, String databaseName, String collectionName) {
        MongoDatabase db = mongo.getDatabase(databaseName);
        List<ColumnInfo> columns = new ArrayList<>();
        try {
            // Sample the first 10 documents to infer schema
            List<Document> docs = new ArrayList<>();
            db.getCollection(collectionName).find().limit(10).into(docs);
            
            List<String> fieldNames = new ArrayList<>();
            for (Document doc : docs) {
                for (String key : doc.keySet()) {
                    if (!fieldNames.contains(key)) {
                        fieldNames.add(key);
                        boolean isId = "_id".equals(key);
                        columns.add(ColumnInfo.builder()
                            .name(key)
                            .dataType(doc.get(key) != null ? doc.get(key).getClass().getSimpleName() : "Object")
                            .primaryKey(isId)
                            .build());
                    }
                }
            }
            if (columns.isEmpty()) {
                columns.add(ColumnInfo.builder()
                    .name("_id")
                    .dataType("ObjectId")
                    .primaryKey(true)
                    .build());
            }
        } catch (Exception e) {
            // Ignored
        }
        return columns;
    }

    public List<String> getRedisDatabases(JedisPooled redis) {
        // Redis uses numeric databases (usually 0-15). Just return ["0"] to satisfy UI
        return List.of("0");
    }

    public SchemaResponse getRedisSchema(JedisPooled redis, String databaseName) {
        // We will fake a single table called "Keys" to hold redis records
        return SchemaResponse.builder()
                .databaseName(databaseName != null ? databaseName : "0")
                .tables(List.of(new TableInfo("Keys", "TABLE", 0)))
                .views(List.of())
                .build();
    }

    public List<ColumnInfo> getRedisColumns(JedisPooled redis, String databaseName, String collectionName) {
        return List.of(
            ColumnInfo.builder().name("key").dataType("String").primaryKey(true).build(),
            ColumnInfo.builder().name("type").dataType("String").build(),
            ColumnInfo.builder().name("value").dataType("String").build()
        );
    }

    public List<String> getCassandraDatabases(CqlSession cassandra) {
        List<String> keyspaces = new ArrayList<>();
        cassandra.execute("SELECT keyspace_name FROM system_schema.keyspaces").forEach(row -> {
            keyspaces.add(row.getString("keyspace_name"));
        });
        return keyspaces;
    }

    public SchemaResponse getCassandraSchema(CqlSession cassandra, String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            return SchemaResponse.builder().build();
        }
        List<TableInfo> tables = new ArrayList<>();
        cassandra.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name = '" + databaseName + "'")
                 .forEach(row -> tables.add(new TableInfo(row.getString("table_name"), "TABLE", 0)));
        return SchemaResponse.builder()
                .databaseName(databaseName)
                .tables(tables)
                .views(List.of())
                .build();
    }

    public List<ColumnInfo> getCassandraColumns(CqlSession cassandra, String databaseName, String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        cassandra.execute("SELECT column_name, type, kind FROM system_schema.columns WHERE keyspace_name = '" + databaseName + "' AND table_name = '" + tableName + "'")
                 .forEach(row -> {
                     boolean isPk = "partition_key".equals(row.getString("kind")) || "clustering".equals(row.getString("kind"));
                     columns.add(ColumnInfo.builder()
                         .name(row.getString("column_name"))
                         .dataType(row.getString("type"))
                         .primaryKey(isPk)
                         .build());
                 });
        return columns;
    }

    public List<String> getMemcachedDatabases(MemcachedClient memcached) {
        return List.of("0");
    }

    public SchemaResponse getMemcachedSchema(MemcachedClient memcached, String databaseName) {
        return SchemaResponse.builder()
                .databaseName(databaseName != null ? databaseName : "0")
                .tables(List.of(new TableInfo("Keys", "TABLE", 0)))
                .views(List.of())
                .build();
    }

    public List<ColumnInfo> getMemcachedColumns(MemcachedClient memcached, String databaseName, String collectionName) {
        return List.of(
            ColumnInfo.builder().name("key").dataType("String").primaryKey(true).build(),
            ColumnInfo.builder().name("value").dataType("String").build()
        );
    }

    public List<String> getNeo4jDatabases(Driver neo4j) {
        return List.of("neo4j");
    }

    public SchemaResponse getNeo4jSchema(Driver neo4j, String databaseName) {
        List<TableInfo> tables = new ArrayList<>();
        try (Session session = neo4j.session()) {
            session.run("CALL db.labels()").list().forEach(record -> {
                tables.add(new TableInfo(record.get(0).asString(), "NODE", 0));
            });
        }
        return SchemaResponse.builder()
                .databaseName("neo4j")
                .tables(tables)
                .views(List.of())
                .build();
    }

    public List<ColumnInfo> getNeo4jColumns(Driver neo4j, String databaseName, String labelName) {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Session session = neo4j.session()) {
            session.run("MATCH (n:`" + labelName + "`) RETURN keys(n) LIMIT 1").list().forEach(record -> {
                record.get(0).asList().forEach(prop -> {
                    columns.add(ColumnInfo.builder().name(prop.toString()).dataType("Property").build());
                });
            });
        }
        if (columns.isEmpty()) {
            columns.add(ColumnInfo.builder().name("id").dataType("Long").primaryKey(true).build());
        }
        return columns;
    }

    public List<String> getDynamoDatabases(software.amazon.awssdk.services.dynamodb.DynamoDbClient client) {
        return List.of("DynamoDB");
    }
    public SchemaResponse getDynamoSchema(software.amazon.awssdk.services.dynamodb.DynamoDbClient client, String dbName) {
        List<TableInfo> tables = new ArrayList<>();
        client.listTables().tableNames().forEach(name -> tables.add(new TableInfo(name, "TABLE", 0)));
        return SchemaResponse.builder().databaseName(dbName != null ? dbName : "DynamoDB").tables(tables).build();
    }
    public List<ColumnInfo> getDynamoColumns(software.amazon.awssdk.services.dynamodb.DynamoDbClient client, String dbName, String tableName) {
        return List.of(ColumnInfo.builder().name("Item").dataType("JSON").build());
    }

    public List<String> getArangoDatabases(com.arangodb.ArangoDB client) {
        return List.of("_system");
    }
    public SchemaResponse getArangoSchema(com.arangodb.ArangoDB client, String dbName) {
        List<TableInfo> tables = new ArrayList<>();
        client.db().getCollections().forEach(col -> tables.add(new TableInfo(col.getName(), "DOCUMENT", 0)));
        return SchemaResponse.builder().databaseName(dbName != null ? dbName : "_system").tables(tables).build();
    }
    public List<ColumnInfo> getArangoColumns(com.arangodb.ArangoDB client, String dbName, String tableName) {
        return List.of(ColumnInfo.builder().name("_key").dataType("String").primaryKey(true).build(), ColumnInfo.builder().name("_id").dataType("String").build());
    }

    public List<String> getNeptuneDatabases(org.apache.tinkerpop.gremlin.driver.Client client) {
        return List.of("NeptuneGraph");
    }
    public SchemaResponse getNeptuneSchema(org.apache.tinkerpop.gremlin.driver.Client client, String dbName) {
        return SchemaResponse.builder().databaseName("NeptuneGraph").tables(List.of(new TableInfo("Vertices", "GRAPH", 0), new TableInfo("Edges", "GRAPH", 0))).build();
    }
    public List<ColumnInfo> getNeptuneColumns(org.apache.tinkerpop.gremlin.driver.Client client, String dbName, String tableName) {
        return List.of(ColumnInfo.builder().name("id").dataType("String").primaryKey(true).build(), ColumnInfo.builder().name("label").dataType("String").build());
    }

    public List<String> getHBaseDatabases(org.apache.hadoop.hbase.client.Connection client) {
        return List.of("HBase");
    }
    public SchemaResponse getHBaseSchema(org.apache.hadoop.hbase.client.Connection client, String dbName) {
        List<TableInfo> tables = new ArrayList<>();
        try {
            for (org.apache.hadoop.hbase.TableName tn : client.getAdmin().listTableNames()) {
                tables.add(new TableInfo(tn.getNameAsString(), "TABLE", 0));
            }
        } catch(Exception e) {}
        return SchemaResponse.builder().databaseName("HBase").tables(tables).build();
    }
    public List<ColumnInfo> getHBaseColumns(org.apache.hadoop.hbase.client.Connection client, String dbName, String tableName) {
        return List.of(ColumnInfo.builder().name("RowKey").dataType("Bytes").primaryKey(true).build(), ColumnInfo.builder().name("ColumnFamily").dataType("Map").build());
    }

    public List<String> getCouchDatabases(String[] clientData) {
        return List.of("CouchDB");
    }
    public SchemaResponse getCouchSchema(String[] clientData, String dbName) {
        return SchemaResponse.builder().databaseName("CouchDB").tables(List.of(new TableInfo("_all_docs", "COLLECTION", 0))).build();
    }
    public List<ColumnInfo> getCouchColumns(String[] clientData, String dbName, String tableName) {
        return List.of(ColumnInfo.builder().name("_id").dataType("String").primaryKey(true).build(), ColumnInfo.builder().name("_rev").dataType("String").build());
    }
}
