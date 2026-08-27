package com.sqleditor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateSql(String prompt, String schemaContext, String dbType, String apiKey) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=" + apiKey;

        String systemInstruction = "Sen bir SQL uzmanısın. Kullanıcının isteğine göre SADECE GEÇERLİ BİR SQL SORGUSU üretmelisin. " +
                "Veritabanı türü: " + dbType + ". " +
                "ÇOK ÖNEMLİ KURALLAR:\n" +
                "1. Sadece ve sadece SQL kodunu ver. Hiçbir açıklama veya markdown (`sql gibi) KULLANMA.\n" +
                "2. Eğer isim, kelime arama (LIKE) işlemi yapıyorsan ve veritabanı MySQL/MariaDB ise, 's' ve 'ş' harflerinin veritabanı tarafından karıştırılmasını önlemek için KESİNLİKLE LIKE BINARY '%...%' yapısını kullan. Asla düz LIKE kullanma!\n";

        String fullPrompt = "Veritabanı Şeması (Tablolar ve Kolonlar):\n" + schemaContext + "\n\n" +
                "İstek: " + prompt;

        // Build Gemini Request
        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> systemInstructionMap = new HashMap<>();
        systemInstructionMap.put("parts", List.of(Map.of("text", systemInstruction)));
        requestBody.put("systemInstruction", systemInstructionMap);

        Map<String, Object> contents = new HashMap<>();
        contents.put("parts", List.of(Map.of("text", fullPrompt)));
        requestBody.put("contents", List.of(contents));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(url, request, String.class);
        
        JsonNode root = mapper.readTree(response);
        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
            throw new Exception("Google API yanit dondurmedi. Guvenlik (Safety) filtresine takilmis olabilir.");
        }
        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.path("content");
        if (content.isMissingNode() || content.path("parts").isMissingNode() || content.path("parts").size() == 0) {
            String finishReason = firstCandidate.path("finishReason").asText();
            throw new Exception("AI yanit uretmeyi reddetti. Sebep: " + finishReason + " (Silme/DROP gibi tehlikeli islemler guvenlik filtresine takilabilir).");
        }
        
        String sql = content.path("parts").get(0).path("text").asText();
        
        // Temizle (Bazen markdown kod bloklarıyla dönebiliyor `sql ... `)
        sql = sql.replace("`sql", "").replace("`", "").trim();
        return sql;
    }
}
