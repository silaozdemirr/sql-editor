package com.sqleditor.model;
import jakarta.validation.constraints.*;
public class AuthRequest { @Email @NotBlank private String email; @NotBlank @Size(min=8,max=72) private String password; @Size(min=2,max=80) private String displayName; public String getEmail(){return email;} public String getPassword(){return password;} public String getDisplayName(){return displayName;} public void setEmail(String v){email=v;} public void setPassword(String v){password=v;} public void setDisplayName(String v){displayName=v;} }
