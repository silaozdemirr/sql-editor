package com.sqleditor.model;

import java.util.Map;

public class QueryUpdateReq {
    private String tableName;
    private String updatedColumn;
    private String newValue;
    private Map<String, String> oldRowValues;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getUpdatedColumn() { return updatedColumn; }
    public void setUpdatedColumn(String updatedColumn) { this.updatedColumn = updatedColumn; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public Map<String, String> getOldRowValues() { return oldRowValues; }
    public void setOldRowValues(Map<String, String> oldRowValues) { this.oldRowValues = oldRowValues; }
}
