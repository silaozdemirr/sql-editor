package com.sqleditor.model;

import jakarta.validation.constraints.NotBlank;

/** Editörden çalıştırılacak tek SQL ifadesi. Bağlantı tokenı HTTP başlığındadır. */
public class QueryRequest {

    @NotBlank(message = "SQL sorgusu boş olamaz")
    private String sql;

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
}
