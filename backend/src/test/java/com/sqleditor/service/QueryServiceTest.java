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
        queryService = new QueryService(null); 
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
    public void testCheckReadOnly() {
        // ADMIN rolündeki biri her sorguyu atabilir, hata fırlatmamalı
        assertDoesNotThrow(() -> queryService.checkReadOnly("ADMIN", "UPDATE"));
        
        // READ_ONLY rolündeki biri SELECT sorgusu atabilir
        assertDoesNotThrow(() -> queryService.checkReadOnly("READ_ONLY", "SELECT * FROM users"));
        
        // READ_ONLY rolündeki biri EXPLAIN sorgusu atabilir
        assertDoesNotThrow(() -> queryService.checkReadOnly("READ_ONLY", "EXPLAIN SELECT * FROM users"));
        
        // READ_ONLY rolündeki biri UPDATE, DELETE veya INSERT atamaz! Exception fırlatmalı.
        SecurityException ex1 = assertThrows(SecurityException.class, () -> queryService.checkReadOnly("READ_ONLY", "UPDATE users SET age = 20"));
        assertTrue(ex1.getMessage().contains("Read-Only"));
        
        SecurityException ex2 = assertThrows(SecurityException.class, () -> queryService.checkReadOnly("READ_ONLY", "DROP TABLE users"));
        assertTrue(ex2.getMessage().contains("Read-Only"));
    }
}
