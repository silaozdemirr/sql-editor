package com.sqleditor.model;

import jakarta.validation.constraints.NotBlank;

/** Editörden çalıştırılacak tek SQL ifadesi. Bağlantı tokenı HTTP başlığındadır. */
public class QueryRequest {

    @NotBlank(message = "SQL sorgusu boş olamaz")
    private String sql;

    private String queryId;
    private java.util.List<java.util.Map<String, String>> filters;
    private java.util.List<java.util.Map<String, Object>> sorts;
    private Integer limit;
    private Integer offset;
    private Boolean includeCount;

    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }
    public java.util.List<java.util.Map<String, String>> getFilters() { return filters; }
    public void setFilters(java.util.List<java.util.Map<String, String>> filters) { this.filters = filters; }
    public java.util.List<java.util.Map<String, Object>> getSorts() { return sorts; }
    public void setSorts(java.util.List<java.util.Map<String, Object>> sorts) { this.sorts = sorts; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public Boolean getIncludeCount() { return includeCount; }
    public void setIncludeCount(Boolean includeCount) { this.includeCount = includeCount; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
}
