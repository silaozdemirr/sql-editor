package com.sqleditor.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class TableCreateRequest {
    @NotBlank
    private String databaseName;
    @NotBlank
    private String tableName;
    @NotEmpty
    private List<ColumnDef> columns;

    public static class ColumnDef {
        private String name;
        private String type;
        private boolean primaryKey;
        private boolean autoIncrement;
        private boolean notNull;
        private String foreignKeyTable;
        private String foreignKeyColumn;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
        public boolean isNotNull() { return notNull; }
        public void setNotNull(boolean notNull) { this.notNull = notNull; }
        public String getForeignKeyTable() { return foreignKeyTable; }
        public void setForeignKeyTable(String foreignKeyTable) { this.foreignKeyTable = foreignKeyTable; }
        public String getForeignKeyColumn() { return foreignKeyColumn; }
        public void setForeignKeyColumn(String foreignKeyColumn) { this.foreignKeyColumn = foreignKeyColumn; }
    }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public List<ColumnDef> getColumns() { return columns; }
    public void setColumns(List<ColumnDef> columns) { this.columns = columns; }
}
