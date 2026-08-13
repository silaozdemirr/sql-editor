package com.sqleditor.model;

/**
 * Bağlantı yanıt modeli.
 * Backend'den frontend'e dönen JSON yapısı.
 */
public class ConnectionResponse {

    private boolean success;
    private String connectionToken;
    private String serverVersion;
    private String databaseName;
    private String host;
    private Integer port;
    private String dbType;
    private String message;
    private String errorDetail;
    private long responseTimeMs;

    // Boş constructor (Jackson için)
    public ConnectionResponse() {}

    // Builder pattern manuel implementasyon
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ConnectionResponse obj = new ConnectionResponse();

        public Builder success(boolean v)         { obj.success = v;         return this; }
        public Builder connectionToken(String v)  { obj.connectionToken = v; return this; }
        public Builder serverVersion(String v)    { obj.serverVersion = v;   return this; }
        public Builder databaseName(String v)     { obj.databaseName = v;    return this; }
        public Builder host(String v)             { obj.host = v;            return this; }
        public Builder port(Integer v)            { obj.port = v;            return this; }
        public Builder dbType(String v)           { obj.dbType = v;          return this; }
        public Builder message(String v)          { obj.message = v;         return this; }
        public Builder errorDetail(String v)      { obj.errorDetail = v;     return this; }
        public Builder responseTimeMs(long v)     { obj.responseTimeMs = v;  return this; }

        public ConnectionResponse build() { return obj; }
    }

    // Getters
    public boolean isSuccess()        { return success; }
    public String getConnectionToken() { return connectionToken; }
    public String getServerVersion()  { return serverVersion; }
    public String getDatabaseName()   { return databaseName; }
    public String getHost()           { return host; }
    public Integer getPort()          { return port; }
    public String getDbType()         { return dbType; }
    public String getMessage()        { return message; }
    public String getErrorDetail()    { return errorDetail; }
    public long getResponseTimeMs()   { return responseTimeMs; }
}
