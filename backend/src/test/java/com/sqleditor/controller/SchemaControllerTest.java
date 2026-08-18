package com.sqleditor.controller;

import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.DumpService;
import com.sqleditor.service.SchemaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchemaController.class)
@AutoConfigureMockMvc(addFilters = false) // Güvenlik (Security) testlerini şimdilik devre dışı bırakıyoruz ki sadece Controller'ı test edelim
public class SchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemaService schemaService;

    @MockBean
    private ConnectionSessionService connectionSessionService;

    @MockBean
    private DumpService dumpService;

    @Test
    public void testGetSchemaWithoutToken_ShouldReturnBadRequest() throws Exception {
        // Token eksikken 400 Bad Request dönmeli
        mockMvc.perform(get("/api/schema"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetSchemaWithInvalidToken_ShouldReturnBadGateway() throws Exception {
        // Mocking an invalid token that throws SecurityException
        Mockito.when(connectionSessionService.get(any(), anyString()))
                .thenThrow(new SecurityException("Geçersiz token"));

        // Validasyon gereksinimlerini atlatmak için sahte bir Authentication kullanıyoruz (ya da AutoConfigureMockMvc(addFilters = false) olduğu için Principal geçebiliriz)
        // Spring MVC @RequestHeader("X-Connection-Token") kontrolü yapıyor.
        mockMvc.perform(get("/api/schema")
                .header("X-Connection-Token", "invalid-token")
                .principal(() -> "testUser")) // Mocking the Authentication.getName()
                .andExpect(status().isBadGateway());
    }

    @Test
    public void testGetSchemaWithValidToken_ShouldReturnOk() throws Exception {
        // Mock a connection
        Connection mockConn = Mockito.mock(Connection.class);
        Mockito.when(connectionSessionService.get("testUser", "valid-token")).thenReturn(mockConn);
        Mockito.when(schemaService.getSchema(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/schema")
                .header("X-Connection-Token", "valid-token")
                .principal(() -> "testUser"))
                .andExpect(status().isOk());
    }
}
