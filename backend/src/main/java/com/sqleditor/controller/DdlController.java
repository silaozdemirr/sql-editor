package com.sqleditor.controller;

import com.sqleditor.model.TableCreateRequest;
import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.Statement;

@RestController
@RequestMapping("/api/ddl")
public class DdlController {
    private final ConnectionSessionService sessions;
    private final QueryService queryService;

    public DdlController(ConnectionSessionService sessions, QueryService queryService) {
        this.sessions = sessions;
        this.queryService = queryService;
    }

    @PostMapping("/create-table")
    public ResponseEntity<java.util.Map<String, String>> createTable(@Valid @RequestBody TableCreateRequest req,
                                                                     @RequestHeader("X-Connection-Token") String token,
                                                                     Authentication auth) throws Exception {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        
        try (Connection conn = sessions.get(auth.getName(), token)) {
            // Build DDL (Simplified, targeting generic SQL/MySQL dialect for MVP)
            StringBuilder sql = new StringBuilder("CREATE TABLE ");
            sql.append("`").append(req.getDatabaseName()).append("`.`").append(req.getTableName()).append("` (");
            
            java.util.List<String> pks = new java.util.ArrayList<>();
            for (int i = 0; i < req.getColumns().size(); i++) {
                TableCreateRequest.ColumnDef c = req.getColumns().get(i);
                sql.append("`").append(c.getName()).append("` ").append(c.getType());
                
                if (c.isNotNull()) sql.append(" NOT NULL");
                if (c.isAutoIncrement()) sql.append(" AUTO_INCREMENT");
                if (c.isPrimaryKey()) pks.add("`" + c.getName() + "`");
                
                if (i < req.getColumns().size() - 1) sql.append(", ");
            }
            if (!pks.isEmpty()) {
                sql.append(", PRIMARY KEY (").append(String.join(", ", pks)).append(")");
            }
            sql.append(");");

            // Execute using QueryService to leverage audit & readonly checks
            queryService.execute(conn, sql.toString(), role, auth.getName(), token);

            return ResponseEntity.ok(java.util.Map.of("message", "Tablo basariyla olusturuldu", "sql", sql.toString()));
        }
    }
}
