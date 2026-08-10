package com.sqleditor.model;

import java.util.List;

/** SQL çalıştırma sonucu: kolonlar, satırlar veya etkilenen kayıt sayısı. */
public class QueryResponse {

    private List<String> columns;
    private List<List<String>> rows;
    private Integer updateCount;
    private boolean truncated;
    private long executionTimeMs;
    private String message;

    public QueryResponse(List<String> columns, List<List<String>> rows, Integer updateCount,
                         boolean truncated, long executionTimeMs, String message) {
        this.columns = columns;
        this.rows = rows;
        this.updateCount = updateCount;
        this.truncated = truncated;
        this.executionTimeMs = executionTimeMs;
        this.message = message;
    }

    public List<String> getColumns() { return columns; }
    public List<List<String>> getRows() { return rows; }
    public Integer getUpdateCount() { return updateCount; }
    public boolean isTruncated() { return truncated; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public String getMessage() { return message; }
}
