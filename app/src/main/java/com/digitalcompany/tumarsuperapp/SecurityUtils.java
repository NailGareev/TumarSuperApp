package com.digitalcompany.tumarsuperapp;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;

public class SecurityUtils {

    private static final String TAG = "SecurityUtils";

    // ВАЖНО: В РЕАЛЬНОМ ПРИЛОЖЕНИИ ИСПОЛЬЗУЙТЕ СОЛЬ (SALT)!
    // Соль - это случайные данные, уникальные для каждого пользователя,
    // которые добавляются к паролю перед хешированием.
    // Это значительно усложняет подбор пароля по радужным таблицам.
    // Соль нужно хранить вместе с хешем.
    // Пример: String saltedPin = pin + userSpecificSalt;

    public static String hashPin(String pin) {
        if (pin == null || pin.length() != 4) {
            Log.e(TAG, "Invalid PIN format for hashing.");
            return null; // Или выбросить исключение
        }
        try {
            // Используем SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // В реальном приложении добавьте соль: byte[] hash = digest.digest((pin + salt).getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            // Конвертируем байтовый массив в Hex строку
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 Algorithm not found", e);
            return null; // Обработка ошибки
        }
    }

    // Вспомогательный метод для конвертации байтов в Hex
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Метод для проверки введенного пина с сохраненным хешем
    public static boolean verifyPin(String enteredPin, String storedHash) {
        if (enteredPin == null || storedHash == null) {
            return false;
        }
        String newHash = hashPin(enteredPin); // Хешируем введенный пин (с той же солью, если она используется!)
        return storedHash.equals(newHash);
    }
}
