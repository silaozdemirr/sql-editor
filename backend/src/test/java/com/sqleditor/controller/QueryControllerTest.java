package com.sqleditor.controller;

import com.sqleditor.model.QueryRequest;
import com.sqleditor.model.QueryResponse;
import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.QueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryService queryService;

    @MockBean
    private ConnectionSessionService connectionSessionService;

    @Test
    public void testExecuteQueryWithoutToken_ShouldReturnBadRequest() throws Exception {
        String jsonRequest = "{\"sql\":\"SELECT * FROM users\"}";
        
        mockMvc.perform(post("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExecuteQueryWithValidToken_ShouldReturnOk() throws Exception {
        String jsonRequest = "{\"sql\":\"SELECT * FROM users\"}";
        
        Connection mockConn = Mockito.mock(Connection.class);
        Mockito.when(connectionSessionService.get(any(), eq("valid-token"))).thenReturn(mockConn);
        
        QueryResponse mockResponse = new QueryResponse(null, null, null, false, 10, "Success", null);
        Mockito.when(queryService.execute(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/query")
                .header("X-Connection-Token", "valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
                .principal(() -> "testUser"))
                .andExpect(status().isOk());
    }
}
