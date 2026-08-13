package com.sqleditor.model;

import java.time.Instant;

/** İstemciye dönen kayıtlı bağlantı metadatası; parola hiçbir zaman dönmez. */
public record SavedConnectionResponse(
        String id,
        String connectionName,
        String dbType,
        String host,
        int port,
        String databaseName,
        String username,
        Instant updatedAt
) { }
