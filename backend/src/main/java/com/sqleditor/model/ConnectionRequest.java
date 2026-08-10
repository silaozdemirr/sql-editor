package com.sqleditor.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Veritabanı bağlantı isteği modeli.
 * Frontend'den gelen JSON bağlantı bilgilerini taşır.
 */
public class ConnectionRequest {

    @NotBlank(message = "Host boş olamaz")
    private String host;

    @NotNull(message = "Port boş olamaz")
    @Min(value = 1, message = "Port minimum 1 olmalıdır")
    @Max(value = 65535, message = "Port maximum 65535 olmalıdır")
    private Integer port;

    @NotBlank(message = "Veritabanı adı boş olamaz")
    private String database;

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    private String username;

    private String password;

    @NotBlank(message = "Veritabanı tipi boş olamaz")
    private String dbType;

    private String connectionName;

    // Getters
    public String getHost()           { return host; }
    public Integer getPort()          { return port; }
    public String getDatabase()       { return database; }
    public String getUsername()       { return username; }
    public String getPassword()       { return password; }
    public String getDbType()         { return dbType; }
    public String getConnectionName() { return connectionName; }

    // Setters
    public void setHost(String host)                     { this.host = host; }
    public void setPort(Integer port)                    { this.port = port; }
    public void setDatabase(String database)             { this.database = database; }
    public void setUsername(String username)             { this.username = username; }
    public void setPassword(String password)             { this.password = password; }
    public void setDbType(String dbType)                 { this.dbType = dbType; }
    public void setConnectionName(String connectionName) { this.connectionName = connectionName; }
}
