package com.sqleditor.controller;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/connections") public class ConnectionHistoryController {
 private final JdbcTemplate db; public ConnectionHistoryController(JdbcTemplate db){this.db=db;}
 @GetMapping public List<Map<String,Object>> history(org.springframework.security.core.Authentication auth){return db.queryForList("select id,connection_name,db_type,host,port,database_name,db_username,connected_at from connection_history where user_id=? order by connected_at desc limit 100",auth.getName());}
}
