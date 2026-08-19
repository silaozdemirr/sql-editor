package com.sqleditor.model;

public class AiResponse {
    private String sql;

    public AiResponse() {}

    public AiResponse(String sql) {
        this.sql = sql;
    }

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
}
