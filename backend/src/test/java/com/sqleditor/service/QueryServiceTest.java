package com.sqleditor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class QueryServiceTest {

    private QueryService queryService;

    @BeforeEach
    public void setUp() {
        // QueryService'in temel fonksiyonlarını test etmek için bağımlılık olmadan (null) başlatıyoruz.
        queryService = new QueryService(null, null); 
    }

    @Test
    public void testFormatValue() throws SQLException {
        // 1. null değerin "null" string'ine dönüşmesi lazım
        assertNull(queryService.formatValue(null));
        
        // 2. Sayıların direkt string olarak dönmesi lazım
        assertEquals("123", queryService.formatValue(123));
        
        // 3. Normal metinlerin olduğu gibi dönmesi lazım
        assertEquals("test string", queryService.formatValue("test string"));
        
        // 4. Byte dizilerinin (BLOB) "[BLOB - X bytes]" şeklinde dönmesi lazım
        byte[] bytes = new byte[]{0x48, 0x65, 0x6c, 0x6c, 0x6f}; // "Hello" byte array
        assertEquals("[BINARY]", queryService.formatValue(bytes));
    }

    @Test
    public void testCheckRolePermissions() {
        // ADMIN
        assertDoesNotThrow(() -> queryService.checkRolePermissions("ADMIN", "UPDATE"));
        
        // READ_ONLY
        assertDoesNotThrow(() -> queryService.checkRolePermissions("READ_ONLY", "SELECT * FROM users"));
        assertDoesNotThrow(() -> queryService.checkRolePermissions("READ_ONLY", "EXPLAIN SELECT * FROM users"));
        
        SecurityException ex1 = assertThrows(SecurityException.class, () -> queryService.checkRolePermissions("READ_ONLY", "UPDATE users SET age = 20"));
        assertTrue(ex1.getMessage().contains("READ_ONLY"));
        
        SecurityException ex2 = assertThrows(SecurityException.class, () -> queryService.checkRolePermissions("READ_ONLY", "DROP TABLE users"));
        assertTrue(ex2.getMessage().contains("READ_ONLY"));
        
        // EDITOR
        assertDoesNotThrow(() -> queryService.checkRolePermissions("EDITOR", "SELECT * FROM users"));
        assertDoesNotThrow(() -> queryService.checkRolePermissions("EDITOR", "UPDATE users SET age = 20"));
        assertDoesNotThrow(() -> queryService.checkRolePermissions("EDITOR", "INSERT INTO users (name) VALUES ('test')"));
        
        SecurityException ex3 = assertThrows(SecurityException.class, () -> queryService.checkRolePermissions("EDITOR", "DROP TABLE users"));
        assertTrue(ex3.getMessage().contains("EDITOR"));
        
        SecurityException ex4 = assertThrows(SecurityException.class, () -> queryService.checkRolePermissions("EDITOR", "ALTER TABLE users ADD COLUMN name VARCHAR(50)"));
        assertTrue(ex4.getMessage().contains("EDITOR"));
    }

}
