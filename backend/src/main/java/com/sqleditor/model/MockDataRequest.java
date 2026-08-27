package com.sqleditor.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MockDataRequest {
    @NotBlank
    private String databaseName;
    @NotBlank
    private String tableName;
    @Min(1)
    private int rowCount;
    @NotEmpty
    private List<ColumnMapping> mappings;

    public static class ColumnMapping {
        private String columnName;
        private String fakerType; // e.g. "Name.fullName", "Internet.email", "Number.randomNumber"

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getFakerType() { return fakerType; }
        public void setFakerType(String fakerType) { this.fakerType = fakerType; }
    }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    public List<ColumnMapping> getMappings() { return mappings; }
    public void setMappings(List<ColumnMapping> mappings) { this.mappings = mappings; }
}
