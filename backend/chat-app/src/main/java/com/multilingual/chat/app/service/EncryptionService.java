package com.multilingual.chat.app.service;

import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String RSA_ALGORITHM    = "RSA";
    private static final String RSA_CIPHER       = "RSA/ECB/OAEPPadding";
    // Explicit OAEP params: SHA-256 for both hash and MGF1 — required for Web Crypto API compatibility.
    private static final OAEPParameterSpec OAEP_PARAMS = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    private static final String AES_CIPHER       = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int RSA_KEY_SIZE    = 2048;
    private static final int AES_KEY_BYTES   = 32;   // AES-256
    private static final int GCM_IV_BYTES    = 12;   // 96-bit nonce recommended for GCM
    private static final int GCM_TAG_BITS    = 128;
    private static final int PBKDF2_ITERS    = 100_000;
    private static final int PBKDF2_KEY_BITS = 256;

    // ── RSA keypair ───────────────────────────────────────────────────────────

    public KeyPair generateRsaKeypair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            gen.initialize(RSA_KEY_SIZE, new SecureRandom());
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not available", e);
        }
    }

    public String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded()); // X.509
    }

    public String encodePrivateKey(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded()); // PKCS#8
    }

    public PublicKey decodePublicKey(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(new X509EncodedKeySpec(bytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to decode public key", e);
        }
    }

    public PrivateKey decodePrivateKey(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to decode private key", e);
        }
    }

    // ── AES key / IV generation ───────────────────────────────────────────────

    public byte[] generateAesKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(AES_KEY_BYTES * 8, new SecureRandom());
            return kg.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES algorithm not available", e);
        }
    }

    public byte[] generateAesIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ── AES-256-GCM encrypt / decrypt ────────────────────────────────────────

    public String aesEncrypt(byte[] key, byte[] iv, String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public String aesDecrypt(byte[] key, byte[] iv, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new RuntimeException("AES-GCM authentication tag mismatch — data may be tampered", e);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    // ── RSA-OAEP encrypt / decrypt ────────────────────────────────────────────

    public String rsaEncrypt(PublicKey key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, OAEP_PARAMS);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    public byte[] rsaDecrypt(PrivateKey key, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key, OAEP_PARAMS);
            return cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }

    // ── PBKDF2-HMAC-SHA256 key derivation ────────────────────────────────────

    public byte[] deriveKeyFromPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(), salt, PBKDF2_ITERS, PBKDF2_KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            SecretKey derived = factory.generateSecret(spec);
            spec.clearPassword();
            return derived.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 key derivation failed", e);
        }
    }
}
