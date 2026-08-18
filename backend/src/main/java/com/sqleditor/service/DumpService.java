package com.sqleditor.service;

import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DumpService {

    public void dump(Connection conn, OutputStream out) throws SQLException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.write("-- SQL Editor Database Dump\n");
            writer.write("-- --------------------------------------------------------\n\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");
            
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }

            for (String table : tables) {
                writer.write("-- Table structure for table `" + table + "`\n");
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) {
                        writer.write(rs.getString(2) + ";\n\n");
                    }
                }

                writer.write("-- Dumping data for table `" + table + "`\n");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
                    
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    
                    while (rs.next()) {
                        writer.write("INSERT INTO `" + table + "` VALUES (");
                        for (int i = 1; i <= colCount; i++) {
                            Object obj = rs.getObject(i);
                            if (obj == null) {
                                writer.write("NULL");
                            } else if (obj instanceof Number) {
                                writer.write(obj.toString());
                            } else if (obj instanceof Boolean) {
                                writer.write(((Boolean)obj) ? "1" : "0");
                            } else {
                                String val = obj.toString()
                                    .replace("\\", "\\\\")
                                    .replace("'", "''")
                                    .replace("\n", "\\n")
                                    .replace("\r", "\\r");
                                writer.write("'" + val + "'");
                            }
                            if (i < colCount) writer.write(", ");
                        }
                        writer.write(");\n");
                    }
                }
                writer.write("\n");
            }
            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
            writer.flush();
        } catch (Exception e) {
            throw new SQLException("Dump error", e);
        }
    }
}
