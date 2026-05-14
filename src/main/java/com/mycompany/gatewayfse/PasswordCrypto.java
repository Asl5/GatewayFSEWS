/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author f.matraxia
 */
public class PasswordCrypto {

    private static final String ENV_KEY_NAME = "GATEWAYFSE_DB_SECRET_KEY";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private PasswordCrypto() {
    }

    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = loadKeyFromEnvironment();

            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(output);

        } catch (Exception e) {
            throw new RuntimeException("Errore durante la cifratura della password", e);
        }
    }

    public static String decrypt(String encryptedBase64) {
        try {
            SecretKeySpec keySpec = loadKeyFromEnvironment();

            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            if (decoded.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Password cifrata non valida");
            }

            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(decoded, IV_LENGTH_BYTES, decoded.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Errore durante la decifratura della password", e);
        }
    }

    private static SecretKeySpec loadKeyFromEnvironment() {
        String keyBase64 = System.getenv(ENV_KEY_NAME);

        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException("Variabile d'ambiente " + ENV_KEY_NAME + " non configurata");
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "La chiave " + ENV_KEY_NAME + " deve essere AES-256, quindi 32 byte dopo decodifica Base64"
            );
        }

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
