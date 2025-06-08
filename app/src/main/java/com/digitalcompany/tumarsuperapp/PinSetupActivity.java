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

public class PinSetupActivity extends AppCompatActivity {

    private static final String TAG = "PinSetupActivity";
    // Ключи SharedPreferences
    private static final String PREFS_NAME = "PinSecurityPrefs";
    private static final String KEY_PIN_SET = "pin_is_set";
    private static final String KEY_PIN_HASH = "pin_hash";

    // Состояния для процесса установки PIN
    private enum State { ENTER_NEW, CONFIRM_NEW }
    private State currentState = State.ENTER_NEW;

    // Элементы UI
    private EditText etPinEntry1;
    private EditText etPinEntry2;
    private Button btnPinConfirm;
    private TextView tvPinInstruction;
    private TextView tvPinError;

    // Переменные для хранения данных
    private String firstPin = "";
    // Флаг, указывающий, меняем ли мы PIN или устанавливаем впервые
    private boolean isChangeMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        // Инициализация UI элементов
        etPinEntry1 = findViewById(R.id.et_pin_entry_1);
        etPinEntry2 = findViewById(R.id.et_pin_entry_2);
        btnPinConfirm = findViewById(R.id.btn_pin_confirm);
        tvPinInstruction = findViewById(R.id.tv_pin_instruction);
        tvPinError = findViewById(R.id.tv_pin_error);

        // Проверяем, была ли эта Activity запущена с намерением сменить PIN (например, из Профиля)
        // Если запущена из PinEntryActivity, isChangeMode будет false.
        isChangeMode = getIntent().getBooleanExtra("IS_CHANGE_MODE", false);

        setupListeners();
        updateUIState();
    }

    // Настройка слушателей событий для UI элементов
    private void setupListeners() {
        // Обработчик для кнопки подтверждения
        btnPinConfirm.setOnClickListener(v -> handleConfirmClick());

        // Слушатель для первого поля ввода PIN
        etPinEntry1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                tvPinError.setVisibility(View.GONE); // Скрываем ошибку при вводе
                // Активируем кнопку, только если введено 4 цифры
                btnPinConfirm.setEnabled(s.length() == 4);
            }
        });

        // Слушатель для второго поля ввода PIN (подтверждение)
        etPinEntry2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                tvPinError.setVisibility(View.GONE); // Скрываем ошибку при вводе
                // Активируем кнопку, только если введено 4 цифры
                btnPinConfirm.setEnabled(s.length() == 4);
            }
        });

        // Изначально кнопка подтверждения неактивна
        btnPinConfirm.setEnabled(false);
    }

    // Обработка нажатия кнопки подтверждения в зависимости от текущего состояния
    private void handleConfirmClick() {
        if (currentState == State.ENTER_NEW) {
            String pin1 = etPinEntry1.getText().toString();
            if (pin1.length() == 4) {
                firstPin = pin1; // Сохраняем первый введенный PIN
                currentState = State.CONFIRM_NEW; // Переходим к состоянию подтверждения
                updateUIState(); // Обновляем UI
            } else {
                // Этого не должно произойти, т.к. кнопка активна только при 4 цифрах
                showError("Введите 4 цифры");
            }
        } else if (currentState == State.CONFIRM_NEW) {
            String pin2 = etPinEntry2.getText().toString();
            if (pin2.length() == 4) {
                if (firstPin.equals(pin2)) {
                    // PIN-коды совпали - хешируем и сохраняем
                    savePin(firstPin);
                } else {
                    // PIN-коды не совпали
                    showError("ПИН-коды не совпадают");
                    firstPin = ""; // Сбрасываем сохраненный первый PIN
                    currentState = State.ENTER_NEW; // Возвращаемся к начальному состоянию
                    updateUIState(); // Обновляем UI
                }
            } else {
                showError("Введите 4 цифры");
            }
        }
    }

    // Сохранение хешированного PIN-кода в SharedPreferences
    private void savePin(String pin) {
        String hashedPin = SecurityUtils.hashPin(pin); // Хешируем PIN с помощью SecurityUtils
        if (hashedPin != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_PIN_HASH, hashedPin); // Сохраняем хеш
            editor.putBoolean(KEY_PIN_SET, true);      // Устанавливаем флаг, что PIN установлен
            boolean success = editor.commit();          // Применяем изменения синхронно

            Log.d(TAG, "PIN успешно сохранен: " + success);

            if (success) {
                Toast.makeText(this, "ПИН-код успешно установлен!", Toast.LENGTH_SHORT).show();

                // --- НАЧАЛО ИЗМЕНЕНИЯ ---
                // Запускаем MainActivity напрямую после успешной установки
                Log.d(TAG, "Установка PIN успешна. Запуск MainActivity.");
                Intent intent = new Intent(this, MainActivity.class);
                // Очищаем стек задач, чтобы пользователь не мог вернуться к установке PIN
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Закрываем PinSetupActivity
                // --- КОНЕЦ ИЗМЕНЕНИЯ ---
            } else {
                Log.e(TAG, "Не удалось записать PIN в SharedPreferences!");
                showError("Ошибка сохранения ПИН-кода");
                // Сброс состояния в случае ошибки сохранения
                firstPin = "";
                currentState = State.ENTER_NEW;
                updateUIState();
            }
        } else {
            Log.e(TAG, "Не удалось захэшировать PIN!");
            showError("Ошибка сохранения ПИН-кода");
            // Сброс состояния в случае ошибки хеширования
            firstPin = "";
            currentState = State.ENTER_NEW;
            updateUIState();
        }
    }

    // Обновление UI в соответствии с текущим состоянием (ENTER_NEW или CONFIRM_NEW)
    private void updateUIState() {
        etPinEntry1.setText(""); // Очищаем поля ввода
        etPinEntry2.setText("");
        btnPinConfirm.setEnabled(false); // Деактивируем кнопку
        tvPinError.setVisibility(View.GONE); // Скрываем текст ошибки

        if (currentState == State.ENTER_NEW) {
            // Настраиваем UI для ввода нового PIN
            tvPinInstruction.setText(isChangeMode ? "Введите новый ПИН-код (4 цифры)" : "Придумайте ПИН-код (4 цифры)");
            etPinEntry1.setVisibility(View.VISIBLE);
            etPinEntry2.setVisibility(View.GONE); // Скрываем второе поле
            etPinEntry1.requestFocus(); // Фокус на первое поле
            btnPinConfirm.setText("Далее");
        } else { // CONFIRM_NEW
            // Настраиваем UI для подтверждения PIN
            tvPinInstruction.setText("Повторите ПИН-код");
            etPinEntry1.setVisibility(View.GONE); // Скрываем первое поле
            etPinEntry2.setVisibility(View.VISIBLE);
            etPinEntry2.requestFocus(); // Фокус на второе поле
            btnPinConfirm.setText("Подтвердить");
        }
    }

    // Отображение сообщения об ошибке
    private void showError(String message) {
        tvPinError.setText(message);
        tvPinError.setVisibility(View.VISIBLE);
    }

    // Обработка нажатия системной кнопки "Назад"
    @Override
    public void onBackPressed() {
        if(currentState == State.CONFIRM_NEW) {
            // Если пользователь нажал "Назад" на этапе подтверждения, возвращаемся к вводу нового PIN
            currentState = State.ENTER_NEW;
            firstPin = "";
            updateUIState();
        } else {
            // Если нажата кнопка Назад во время начальной установки (на первом шаге), выходим из приложения.
            // PinEntryActivity обрабатывает логику, если PIN уже был установлен.
            super.onBackPressed();
            finishAffinity(); // Закрываем все Activity приложения
            Log.d(TAG, "Нажата кнопка Назад при начальной установке PIN. Завершение приложения.");
        }
    }
}