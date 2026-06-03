package com.digitalcompany.tumarsuperapp;

import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class SecurityUtils {

    private static final String TAG = "SecurityUtils";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    public static String hashPin(String pin) {
        if (pin == null || pin.length() != 4) {
            Log.e(TAG, "Invalid PIN format for hashing.");
            return null;
        }
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(pin.toCharArray(), salt);
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            Log.e(TAG, "Error hashing PIN", e);
            return null;
        }
    }

    public static boolean verifyPin(String enteredPin, String storedValue) {
        if (enteredPin == null || storedValue == null) {
            return false;
        }
        if (!storedValue.contains(":")) {
            // Legacy SHA-256 format — migrate on next successful login
            return verifyLegacySha256(enteredPin, storedValue);
        }
        try {
            String[] parts = storedValue.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);
            byte[] enteredHash = pbkdf2(enteredPin.toCharArray(), salt);
            return constantTimeEquals(storedHash, enteredHash);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying PIN", e);
            return false;
        }
    }

    private static byte[] pbkdf2(char[] pin, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static boolean verifyLegacySha256(String pin, String storedHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return storedHash.equals(hex.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available", e);
            return false;
        }
    }
}
