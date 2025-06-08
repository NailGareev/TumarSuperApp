package com.digitalcompany.tumarsuperapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PinEntryActivity extends AppCompatActivity {

    private static final String TAG = "PinEntryActivity";

    // Ключи SharedPreferences для PIN
    private static final String PIN_PREFS_NAME = "PinSecurityPrefs";
    private static final String KEY_PIN_SET = "pin_is_set";
    private static final String KEY_PIN_HASH = "pin_hash";

    // Ключи SharedPreferences для пользователя (Регистрация и Вход)
    private static final String USER_PREFS_NAME = "UserPrefs";
    // private static final String KEY_IS_REGISTERED = "is_registered"; // Больше не нужен для этой логики
    private static final String KEY_IS_LOGGED_IN = "is_logged_in"; // Флаг входа
    private static final String KEY_AUTH_TOKEN = "auth_token";     // Токен

    private EditText etPinEntry;
    private Button btnPinLogin;
    private TextView tvPinEntryError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        // --- ИСПРАВЛЕННАЯ ЛОГИКА ПРОВЕРКИ ---
        SharedPreferences userPrefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences pinPrefs = getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE);

        boolean isLoggedIn = userPrefs.getBoolean(KEY_IS_LOGGED_IN, false);
        boolean isPinSet = pinPrefs.getBoolean(KEY_PIN_SET, false);
        String token = userPrefs.getString(KEY_AUTH_TOKEN, null);
        String pinHash = pinPrefs.getString(KEY_PIN_HASH, null); // Проверяем и наличие хеша

        // 1. Проверяем, вошел ли пользователь и установлен ли у него PIN
        if (isLoggedIn && token != null && isPinSet && pinHash != null) {
            // Сценарий: Пользователь вошел и у него есть PIN -> Показать экран ввода PIN
            Log.d(TAG, "Пользователь вошел и PIN установлен. Отображение экрана ввода PIN.");
            setContentView(R.layout.activity_pin_entry);
            etPinEntry = findViewById(R.id.et_pin_entry);
            btnPinLogin = findViewById(R.id.btn_pin_login);
            tvPinEntryError = findViewById(R.id.tv_pin_entry_error);
            setupListeners();
        }
        // 2. Проверяем, вошел ли пользователь, но PIN еще НЕ установлен
        else if (isLoggedIn && token != null && (!isPinSet || pinHash == null)) {
            // Сценарий: Пользователь вошел (например, только что через LoginActivity), но PIN не настроен -> Отправить на установку PIN
            Log.d(TAG, "Пользователь вошел, но PIN не установлен/отсутствует. Запуск PinSetupActivity.");
            navigateTo(PinSetupActivity.class);
        }
        // 3. Если пользователь НЕ вошел в систему (независимо от статуса регистрации или PIN)
        else {
            // Сценарий: Пользователь не авторизован -> Отправить на экран входа
            Log.d(TAG, "Пользователь не вошел в систему. Перенаправление на LoginActivity.");
            navigateTo(LoginActivity.class); // <-- ВСЕГДА отправляем на логин, если не авторизован
        }
        // --- КОНЕЦ ИСПРАВЛЕННОЙ ЛОГИКИ ---

        // Лог о завершении onCreate будет выводиться только если остаемся на этом экране
        if (findViewById(R.id.et_pin_entry) != null) { // Проверяем, загружен ли макет
            Log.d(TAG, "onCreate finished (сценарий ввода PIN)");
        }
    }

    // Вспомогательный метод для навигации с очисткой стека
    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Закрываем текущую PinEntryActivity
    }


    // Методы setupListeners(), verifyPin(), showError(), onBackPressed() остаются такими же

    private void setupListeners() {
        // Этот код выполняется только если показан R.layout.activity_pin_entry
        if (etPinEntry == null || btnPinLogin == null) {
            Log.e(TAG, "Попытка настроить слушателей до инициализации UI!");
            return;
        }
        btnPinLogin.setEnabled(false);
        etPinEntry.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (tvPinEntryError != null) {
                    tvPinEntryError.setVisibility(View.GONE);
                }
                // Проверка на null перед доступом
                if (btnPinLogin != null) {
                    btnPinLogin.setEnabled(s.length() == 4);
                }
            }
        });
        btnPinLogin.setOnClickListener(v -> verifyPin());
    }

    private void verifyPin() {
        String enteredPin = etPinEntry.getText().toString();
        SharedPreferences pinPrefs = getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE);
        String storedHash = pinPrefs.getString(KEY_PIN_HASH, null);

        if (storedHash == null) {
            Log.e(TAG, "Ошибка: PIN помечен как установленный, но хэш отсутствует!");
            showError("Ошибка входа. Попробуйте переустановить приложение.");
            // Можно добавить перенаправление на LoginActivity или PinSetupActivity
            // navigateTo(LoginActivity.class);
            return;
        }

        // Предполагается, что SecurityUtils.verifyPin существует и работает корректно
        if (SecurityUtils.verifyPin(enteredPin, storedHash)) {
            Log.d(TAG, "Проверка PIN успешна. Запуск MainActivity.");
            navigateTo(MainActivity.class); // Используем navigateTo для консистентности
        } else {
            Log.w(TAG, "Проверка PIN не удалась.");
            showError("Неверный ПИН-код");
            etPinEntry.setText("");
        }
    }

    private void showError(String message) {
        if (tvPinEntryError != null) {
            tvPinEntryError.setText(message);
            tvPinEntryError.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "Попытка показать ошибку PIN до инициализации UI: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Если пользователь на экране ввода PIN и нажимает назад, лучше просто выйти из приложения
        // Вместо super.onBackPressed(), чтобы не попасть на пустой экран или предыдущую активность в стеке
        finishAffinity();
        Log.d(TAG, "Нажата кнопка Назад на экране ввода PIN. Завершение приложения.");
    }
}