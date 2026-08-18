package com.sqleditor.model;

import java.util.List;

public class ErdResponse {
    private List<ErdTable> tables;
    private List<ErdEdge> edges;

    public ErdResponse(List<ErdTable> tables, List<ErdEdge> edges) {
        this.tables = tables;
        this.edges = edges;
    }

    public List<ErdTable> getTables() { return tables; }
    public List<ErdEdge> getEdges() { return edges; }

    public static class ErdTable {
        private String name;
        private List<ColumnInfo> columns;

        public ErdTable(String name, List<ColumnInfo> columns) {
            this.name = name;
            this.columns = columns;
        }

        public String getName() { return name; }
        public List<ColumnInfo> getColumns() { return columns; }
    }

    public static class ErdEdge {
        private String sourceTable;
        private String sourceColumn;
        private String targetTable;
        private String targetColumn;

        public ErdEdge(String sourceTable, String sourceColumn, String targetTable, String targetColumn) {
            this.sourceTable = sourceTable;
            this.sourceColumn = sourceColumn;
            this.targetTable = targetTable;
            this.targetColumn = targetColumn;
        }

        public String getSourceTable() { return sourceTable; }
        public String getSourceColumn() { return sourceColumn; }
        public String getTargetTable() { return targetTable; }
        public String getTargetColumn() { return targetColumn; }
    }
}
