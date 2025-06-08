package com.digitalcompany.tumarsuperapp;

// --- Импорты для сети (Retrofit) ---
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.RegistrationRequest;
import com.digitalcompany.tumarsuperapp.network.models.RegistrationResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// --- Стандартные импорты ---
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context; // Добавлен импорт Context
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.IOException; // Добавлен импорт IOException

public class RegistrationActivity extends AppCompatActivity {

    // --- Константы SharedPreferences (только для флага) ---
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_REGISTERED = "is_registered";

    // --- View компоненты ---
    private TextInputLayout tilPhone, tilEmail, tilAge, tilFirstName, tilLastName, tilPassword;
    private TextInputEditText etPhone, etEmail, etAge, etFirstName, etLastName, etPassword;
    private Button btnRegister;

    // --- Retrofit Service ---
    private ApiService apiService;

    // --- Тег для логирования ---
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // --- ИСПРАВЛЕНИЕ: Передаем контекст ---
        apiService = ApiClient.getApiService(this);
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---

        // --- Инициализация View ---
        tilPhone = findViewById(R.id.til_phone);
        etPhone = findViewById(R.id.et_phone);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilFirstName = findViewById(R.id.til_first_name);
        etFirstName = findViewById(R.id.et_first_name);
        tilLastName = findViewById(R.id.til_last_name);
        etLastName = findViewById(R.id.et_last_name);
        tilAge = findViewById(R.id.til_age);
        etAge = findViewById(R.id.et_age);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);

        // --- Установка слушателя на кнопку ---
        btnRegister.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        // --- Сброс ошибок валидации ---
        tilPhone.setError(null);
        tilEmail.setError(null);
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilAge.setError(null);
        tilPassword.setError(null);

        // --- Получение данных из полей ввода ---
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // --- Валидация данных ---
        if (password.isEmpty()) {
            tilPassword.setError("Введите пароль");
            focusView = etPassword;
            cancel = true;
        } else if (password.length() < 6) {
            tilPassword.setError("Пароль должен быть не менее 6 символов");
            focusView = etPassword;
            cancel = true;
        }
        int age = -1;
        if (ageStr.isEmpty()) {
            tilAge.setError("Введите возраст");
            focusView = etAge;
            cancel = true;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age <= 0 || age > 120) {
                    tilAge.setError("Введите корректный возраст");
                    focusView = etAge;
                    cancel = true;
                }
            } catch (NumberFormatException e) {
                tilAge.setError("Введите числовое значение");
                focusView = etAge;
                cancel = true;
            }
        }
        if (lastName.isEmpty()) {
            tilLastName.setError("Введите фамилию");
            focusView = etLastName;
            cancel = true;
        } else if (!lastName.matches("[a-zA-Zа-яА-ЯёЁ\\s-]+")) {
            tilLastName.setError("Фамилия может содержать только буквы, пробелы, дефисы");
            focusView = etLastName;
            cancel = true;
        }
        if (firstName.isEmpty()) {
            tilFirstName.setError("Введите имя");
            focusView = etFirstName;
            cancel = true;
        } else if (!firstName.matches("[a-zA-Zа-яА-ЯёЁ\\s-]+")) {
            tilFirstName.setError("Имя может содержать только буквы, пробелы, дефисы");
            focusView = etFirstName;
            cancel = true;
        }
        if (email.isEmpty()) {
            tilEmail.setError("Введите email");
            focusView = etEmail;
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Некорректный формат email");
            focusView = etEmail;
            cancel = true;
        }
        if (phone.isEmpty()) {
            tilPhone.setError("Введите номер телефона");
            focusView = etPhone;
            cancel = true;
        } else if (!phone.startsWith("+7") || phone.length() != 12) {
            tilPhone.setError("Номер должен начинаться с +7 и содержать 12 символов");
            focusView = etPhone;
            cancel = true;
        } else if (!phone.substring(1).matches("\\d+")) {
            tilPhone.setError("Номер должен содержать только цифры после +");
            focusView = etPhone;
            cancel = true;
        }


        // --- Проверка результата валидации ---
        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // --- Все данные валидны, выполняем API запрос ---
            Log.d(TAG, "Валидация регистрации пройдена. Отправка запроса...");
            // showProgressDialog();

            RegistrationRequest request = new RegistrationRequest(firstName, lastName, email, phone, age, password);

            // Проверяем, что apiService не null перед использованием
            if (apiService == null) {
                Log.e(TAG, "apiService is null in attemptRegistration!");
                Toast.makeText(this, "Ошибка инициализации сети", Toast.LENGTH_SHORT).show();
                return;
            }
            apiService.registerUser(request).enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    // hideProgressDialog();
                    Log.d(TAG, "Получен ответ на регистрацию: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        RegistrationResponse registrationResponse = response.body();
                        Log.d(TAG, "Тело ответа регистрации: " + registrationResponse);

                        if (registrationResponse.isSuccess()) {
                            Log.d(TAG, "Регистрация на сервере успешна. Сообщение: " + registrationResponse.getMessage());
                            // Сохраняем флаг, что пользователь зарегистрирован
                            saveRegistrationFlag();

                            Toast.makeText(RegistrationActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

                            // Переход к экрану установки PIN-кода
                            Intent intent = new Intent(RegistrationActivity.this, PinSetupActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMessage = registrationResponse.getMessage() != null ? registrationResponse.getMessage() : "Неизвестная ошибка регистрации";
                            Log.w(TAG, "Ошибка регистрации от сервера: " + errorMessage);
                            Toast.makeText(RegistrationActivity.this, "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
                            // Установка ошибки на соответствующее поле
                            if (errorMessage.toLowerCase().contains("email")) { tilEmail.setError(errorMessage); tilEmail.requestFocus(); }
                            else if (errorMessage.toLowerCase().contains("phone")) { tilPhone.setError(errorMessage); tilPhone.requestFocus();}
                        }
                    } else {
                        String errorBodyStr = "Не удалось обработать ответ сервера";
                        if (response.errorBody() != null) {
                            try {
                                errorBodyStr = response.errorBody().string();
                            } catch (IOException e) { // Исправлено на IOException
                                Log.e(TAG, "Ошибка чтения response.errorBody()", e);
                            }
                        }
                        Log.e(TAG, "Ошибка ответа API при регистрации: " + response.code() + ", Тело ошибки: " + errorBodyStr);
                        Toast.makeText(RegistrationActivity.this, "Ошибка сервера (" + response.code() + "). Попробуйте позже.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                    // hideProgressDialog();
                    Log.e(TAG, "Ошибка сети при регистрации", t);
                    Toast.makeText(RegistrationActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveRegistrationFlag() {
        SharedPreferences prefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_REGISTERED, true);
        editor.apply();
        Log.d(TAG, "Флаг KEY_IS_REGISTERED сохранен в SharedPreferences.");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy завершен.");
    }

    // Опциональные методы для ProgressBar
    // private void showProgressDialog() { /* ... */ }
    // private void hideProgressDialog() { /* ... */ }
}