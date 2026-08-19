package com.sqleditor.model;

import jakarta.validation.constraints.NotBlank;

public class AiRequest {
    @NotBlank(message = "Sorgu metni boş olamaz")
    private String prompt;
    
    private String dbType;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    
    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
}
