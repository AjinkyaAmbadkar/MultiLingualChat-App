package com.multilingual.chat.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService enc;

    @BeforeEach
    void setUp() {
        enc = new EncryptionService();
    }

    // ── RSA round-trip ────────────────────────────────────────────────────────

    @Test
    void rsaEncryptDecrypt_roundTrip() {
        KeyPair kp = enc.generateRsaKeypair();
        byte[] original = "hello RSA".getBytes();
        String ciphertext = enc.rsaEncrypt(kp.getPublic(), original);
        byte[] decrypted = enc.rsaDecrypt(kp.getPrivate(), ciphertext);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void rsaDecrypt_wrongPrivateKey_throws() {
        KeyPair kp1 = enc.generateRsaKeypair();
        KeyPair kp2 = enc.generateRsaKeypair();
        String ciphertext = enc.rsaEncrypt(kp1.getPublic(), "secret".getBytes());
        assertThrows(RuntimeException.class, () -> enc.rsaDecrypt(kp2.getPrivate(), ciphertext));
    }

    // ── AES round-trip ────────────────────────────────────────────────────────

    @Test
    void aesEncryptDecrypt_roundTrip() {
        byte[] key = enc.generateAesKey();
        byte[] iv  = enc.generateAesIv();
        String plaintext = "Hello, World! 🌍";
        String ciphertext = enc.aesEncrypt(key, iv, plaintext);
        assertEquals(plaintext, enc.aesDecrypt(key, iv, ciphertext));
    }

    @Test
    void aesDecrypt_wrongKey_throws() {
        byte[] key1 = enc.generateAesKey();
        byte[] key2 = enc.generateAesKey();
        byte[] iv   = enc.generateAesIv();
        String ciphertext = enc.aesEncrypt(key1, iv, "secret message");
        assertThrows(RuntimeException.class, () -> enc.aesDecrypt(key2, iv, ciphertext));
    }

    @Test
    void aesDecrypt_tamperedCiphertext_throwsAEADBadTagException() {
        byte[] key = enc.generateAesKey();
        byte[] iv  = enc.generateAesIv();
        String ciphertext = enc.aesEncrypt(key, iv, "tamper me");

        // Flip one byte in the Base64-decoded ciphertext
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        raw[0] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(raw);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> enc.aesDecrypt(key, iv, tampered));
        assertTrue(ex.getCause() instanceof AEADBadTagException,
                "Expected AEADBadTagException but got: " + ex.getCause());
    }

    // ── Key encoding round-trip ───────────────────────────────────────────────

    @Test
    void publicKeyEncodeDecodeRoundTrip() {
        KeyPair kp = enc.generateRsaKeypair();
        String encoded = enc.encodePublicKey(kp.getPublic());
        PublicKey decoded = enc.decodePublicKey(encoded);
        assertEquals(kp.getPublic(), decoded);
    }

    @Test
    void privateKeyEncodeDecodeRoundTrip() {
        KeyPair kp = enc.generateRsaKeypair();
        String encoded = enc.encodePrivateKey(kp.getPrivate());
        PrivateKey decoded = enc.decodePrivateKey(encoded);
        assertEquals(kp.getPrivate(), decoded);
    }

    // ── PBKDF2 determinism ────────────────────────────────────────────────────

    @Test
    void deriveKeyFromPassword_sameSalt_producesIdenticalKey() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] key1 = enc.deriveKeyFromPassword("myPassword123!", salt);
        byte[] key2 = enc.deriveKeyFromPassword("myPassword123!", salt);
        assertArrayEquals(key1, key2);
    }

    @Test
    void deriveKeyFromPassword_differentPassword_producesDifferentKey() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] key1 = enc.deriveKeyFromPassword("passwordA", salt);
        byte[] key2 = enc.deriveKeyFromPassword("passwordB", salt);
        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    // ── Full hybrid flow ──────────────────────────────────────────────────────

    @Test
    void hybridFlow_encryptAndDecryptMessage() {
        KeyPair receiverKp = enc.generateRsaKeypair();

        // Sender side
        byte[] aesKey = enc.generateAesKey();
        byte[] aesIv  = enc.generateAesIv();
        String plaintext = "Hybrid encryption test message";
        String encryptedMessage = enc.aesEncrypt(aesKey, aesIv, plaintext);
        String wrappedAesKey    = enc.rsaEncrypt(receiverKp.getPublic(), aesKey);

        // Receiver side
        byte[] unwrappedAesKey = enc.rsaDecrypt(receiverKp.getPrivate(), wrappedAesKey);
        String decryptedMessage = enc.aesDecrypt(unwrappedAesKey, aesIv, encryptedMessage);

        assertEquals(plaintext, decryptedMessage);
    }
}
