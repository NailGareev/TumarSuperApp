package com.digitalcompany.tumarsuperapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity; // Импортируем Activity
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View; // Импортируем View
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// Добавляем импорт для IOException
import java.io.IOException;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.LoginRequest;
import com.digitalcompany.tumarsuperapp.network.models.LoginResponse;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Ключи для сохранения состояния входа
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";

    // Ключи для проверки, установлен ли PIN
    private static final String PIN_PREFS_NAME = "PinSecurityPrefs";
    private static final String KEY_PIN_SET = "pin_is_set";

    private TextInputLayout tilLoginEmail, tilLoginPassword;
    private TextInputEditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate");

        // --- ИСПРАВЛЕНИЕ: Передаем контекст ---
        // Используем 'this' или 'getApplicationContext()'
        apiService = ApiClient.getApiService(this);
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---

        tilLoginEmail = findViewById(R.id.til_login_email);
        etLoginEmail = findViewById(R.id.et_login_email);
        tilLoginPassword = findViewById(R.id.til_login_password);
        etLoginPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked.");
            attemptLogin();
        });

        tvGoToRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register text clicked.");
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        // Сброс ошибок
        tilLoginEmail.setError(null);
        tilLoginPassword.setError(null);

        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();
        Log.d(TAG, "Attempting login for email: " + email);

        boolean cancel = false;
        View focusView = null;

        // Валидация ... (без изменений)
        if (password.isEmpty()) {
            tilLoginPassword.setError("Введите пароль");
            focusView = etLoginPassword;
            cancel = true;
        }
        if (email.isEmpty()) {
            tilLoginEmail.setError("Введите email");
            focusView = etLoginEmail;
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilLoginEmail.setError("Некорректный формат email");
            focusView = etLoginEmail;
            cancel = true;
        }


        if (cancel) {
            Log.w(TAG, "Login validation failed.");
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // Валидация пройдена, делаем запрос
            Log.d(TAG, "Login validation passed. Sending request...");

            LoginRequest loginRequest = new LoginRequest(email, password);
            // Проверяем, что apiService не null перед использованием
            if (apiService == null) {
                Log.e(TAG, "apiService is null in attemptLogin!");
                Toast.makeText(this, "Ошибка инициализации сети", Toast.LENGTH_SHORT).show();
                return;
            }
            apiService.loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    Log.d(TAG, "Login onResponse - HTTP Code: " + response.code());

                    String responseBodyString = "N/A";
                    if (response.isSuccessful() && response.body() != null) {
                        responseBodyString = response.body().toString();
                        Log.d(TAG, "Login onResponse - Deserialized Body: " + responseBodyString);
                    } else if (response.errorBody() != null) {
                        try {
                            responseBodyString = response.errorBody().string();
                            Log.e(TAG, "Login onResponse - Error Body String: " + responseBodyString);
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                            responseBodyString = "Error reading error body";
                        }
                    } else {
                        Log.w(TAG, "Login onResponse - Body and Error Body are both null");
                        responseBodyString = "Body and Error Body are null";
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        Log.d(TAG, "Login Body Check - isSuccess(): " + loginResponse.isSuccess() + ", getToken(): " + loginResponse.getToken());

                        if (loginResponse.isSuccess() && loginResponse.getToken() != null) {
                            Log.i(TAG, ">>> Login Success conditions met! Proceeding to save state and navigate.");
                            Toast.makeText(LoginActivity.this, "Вход выполнен!", Toast.LENGTH_SHORT).show();
                            saveLoginState(loginResponse.getToken(), loginResponse.getUserId());
                            navigateToNextScreen();
                        } else {
                            String message = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Неверные учетные данные или ошибка ответа";
                            Log.w(TAG, ">>> Login Success conditions NOT met! Staying on LoginActivity. Message: " + message + ", Response Body: " + responseBodyString);
                            Toast.makeText(LoginActivity.this, "Ошибка входа: " + message, Toast.LENGTH_LONG).show();
                            tilLoginPassword.setError(" ");
                            tilLoginEmail.setError("Неверный email или пароль");
                            etLoginPassword.setText("");
                            etLoginEmail.requestFocus();
                        }
                    } else {
                        Log.e(TAG, ">>> Login response unsuccessful or body is null! Staying on LoginActivity. HTTP Code: " + response.code() + ", Response Info: " + responseBodyString);
                        Toast.makeText(LoginActivity.this, "Ошибка сервера (" + response.code() + ").", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e(TAG, ">>> !!! LOGIN NETWORK FAILURE !!! Staying on LoginActivity.", t);
                    Toast.makeText(LoginActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveLoginState(String token, Integer userId) {
        SharedPreferences prefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_AUTH_TOKEN, token);
        if (userId != null) {
            editor.putInt(KEY_USER_ID, userId);
        } else {
            editor.remove(KEY_USER_ID);
        }
        boolean success = editor.commit(); // Используем commit для надежности перед навигацией
        Log.d(TAG, "Login state saved synchronously. Success: " + success);
    }

    private void navigateToNextScreen() {
        Log.i(TAG, ">>> navigateToNextScreen() called!");
        SharedPreferences pinPrefs = getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE);
        boolean isPinSet = pinPrefs.getBoolean(KEY_PIN_SET, false);
        Log.d(TAG, "Checking PIN status for navigation. isPinSet: " + isPinSet);

        Intent intent;
        if (isPinSet) {
            Log.d(TAG, "Navigating to MainActivity.");
            intent = new Intent(this, MainActivity.class);
        } else {
            Log.d(TAG, "Navigating to PinSetupActivity.");
            intent = new Intent(this, PinSetupActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Log.d(TAG, "Finishing LoginActivity after starting next activity.");
        finish();
    }

    public static void logout(Context context) {
        Log.d(TAG, "logout() called.");
        SharedPreferences prefs = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_USER_ID);

        SharedPreferences pinPrefs = context.getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE);
        boolean pinCleared = pinPrefs.edit().clear().commit();
        Log.d(TAG, "PinSecurityPrefs cleared on logout. Success: " + pinCleared);

        boolean logoutSaved = editor.commit();
        Log.d(TAG, "User logged out, SharedPreferences updated. Success: " + logoutSaved);

        Intent intent = new Intent(context, PinEntryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        Log.d(TAG, "Redirecting to PinEntryActivity after logout.");

        if (context instanceof Activity) {
            Log.d(TAG, "Finishing the calling activity: " + context.getClass().getSimpleName());
            ((Activity) context).finish();
        }
    }
}