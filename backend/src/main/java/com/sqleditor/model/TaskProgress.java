package com.sqleditor.model;

public class TaskProgress {
    private String taskId;
    private String status; // RUNNING, DONE, ERROR
    private int totalRows;
    private int processedRows;
    private long startTime;
    private long estimatedTimeRemainingMs;
    private String message;
    private String tableName;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getProcessedRows() { return processedRows; }
    public void setProcessedRows(int processedRows) { this.processedRows = processedRows; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEstimatedTimeRemainingMs() { return estimatedTimeRemainingMs; }
    public void setEstimatedTimeRemainingMs(long estimatedTimeRemainingMs) { this.estimatedTimeRemainingMs = estimatedTimeRemainingMs; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
}
