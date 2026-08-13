package com.sqleditor.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component public class CredentialCipher {
    private final SecretKeySpec key; private final SecureRandom random = new SecureRandom();
    public CredentialCipher(@Value("${app.credentials.encryption-key}") String encodedKey) {
        try { byte[] raw = Base64.getDecoder().decode(encodedKey); if (raw.length != 32) throw new IllegalArgumentException(); key = new SecretKeySpec(raw, "AES"); }
        catch (Exception e) { throw new IllegalStateException("CREDENTIAL_ENCRYPTION_KEY 32 byte Base64 AES anahtarı olmalıdır."); }
    }
    public Encrypted encrypt(String value) { try { byte[] iv = new byte[12]; random.nextBytes(iv); Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv)); return new Encrypted(Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8))), Base64.getEncoder().encodeToString(iv)); } catch(Exception e){throw new IllegalStateException("Şifreleme hatası",e);} }
    public String decrypt(String ciphertext, String iv) { try { Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.getDecoder().decode(iv))); return new String(c.doFinal(Base64.getDecoder().decode(ciphertext)),StandardCharsets.UTF_8); } catch(Exception e){throw new IllegalArgumentException("Saklanan parola çözülemedi",e);} }
    public record Encrypted(String ciphertext, String iv) {}
}
