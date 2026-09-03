package com.sqleditor.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class MaskingService {
    private final JdbcTemplate db;

    public MaskingService(JdbcTemplate db) {
        this.db = db;
    }

    public Map<String, String> getPoliciesForRole(String role) {
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        
        List<Map<String, Object>> rows = db.queryForList(
            "SELECT column_name, mask_type FROM data_masking_policies WHERE role_name = ?", role);
            
        Map<String, String> policies = new HashMap<>();
        for (Map<String, Object> row : rows) {
            policies.put((String) row.get("column_name"), (String) row.get("mask_type"));
        }
        return policies;
    }

    public Object maskValue(Object value, String maskType) {
        if (value == null) return null;
        String valStr = value.toString();
        
        if ("FULL".equalsIgnoreCase(maskType)) {
            return "******";
        } else if ("LAST_4".equalsIgnoreCase(maskType)) {
            if (valStr.length() <= 4) return "****";
            return "******" + valStr.substring(valStr.length() - 4);
        } else if ("EMAIL".equalsIgnoreCase(maskType)) {
            int atIndex = valStr.indexOf('@');
            if (atIndex > 1) {
                return valStr.charAt(0) + "***@" + valStr.substring(atIndex + 1);
            }
            return "******";
        } else if ("NULL".equalsIgnoreCase(maskType)) {
            return null;
        }
        
        return "******"; // Fallback
    }
}
