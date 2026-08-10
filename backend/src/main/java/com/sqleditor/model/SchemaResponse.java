package com.sqleditor.model;

import java.util.List;

/**
 * Veritabanı schema bilgisi — tablolar ve view'lar.
 */
public class SchemaResponse {

    private String databaseName;
    private List<TableInfo> tables;
    private List<TableInfo> views;

    public SchemaResponse() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SchemaResponse obj = new SchemaResponse();
        public Builder databaseName(String v) { obj.databaseName = v; return this; }
        public Builder tables(List<TableInfo> v) { obj.tables = v; return this; }
        public Builder views(List<TableInfo> v)  { obj.views = v;  return this; }
        public SchemaResponse build() { return obj; }
    }

    public String getDatabaseName() { return databaseName; }
    public List<TableInfo> getTables()  { return tables; }
    public List<TableInfo> getViews()   { return views; }

    // ── İç sınıf: Tablo bilgisi ──
    public static class TableInfo {
        private String name;
        private String type;  // TABLE | VIEW
        private int rowCount;

        public TableInfo() {}
        public TableInfo(String name, String type, int rowCount) {
            this.name = name;
            this.type = type;
            this.rowCount = rowCount;
        }

        public String getName()  { return name; }
        public String getType()  { return type; }
        public int getRowCount() { return rowCount; }
    }
}
