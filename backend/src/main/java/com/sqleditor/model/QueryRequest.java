package com.sqleditor.model;

import jakarta.validation.constraints.NotBlank;

/** Editörden çalıştırılacak SQL ve bağlı oturum kimliği. */
public class QueryRequest {

    @NotBlank(message = "Oturum kimliği boş olamaz")
    private String sessionId;

    @NotBlank(message = "SQL sorgusu boş olamaz")
    private String sql;

    public String getSessionId() { return sessionId; }
    public String getSql() { return sql; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setSql(String sql) { this.sql = sql; }
}
