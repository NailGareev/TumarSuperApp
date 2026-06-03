package com.digitalcompany.tumarsuperapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PinSetupActivity extends AppCompatActivity {

    private static final String TAG = "PinSetupActivity";
    private static final String PREFS_NAME = "PinSecurityPrefs";
    private static final String KEY_PIN_SET = "pin_is_set";
    private static final String KEY_PIN_HASH = "pin_hash";

    private enum State { ENTER_NEW, CONFIRM_NEW }
    private State currentState = State.ENTER_NEW;

    private TextView tvInstruction;
    private TextView tvError;
    private View[] dots;
    private final StringBuilder pin = new StringBuilder();
    private String firstPin = "";
    private boolean isChangeMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        isChangeMode = getIntent().getBooleanExtra("IS_CHANGE_MODE", false);

        tvInstruction = findViewById(R.id.tv_pin_instruction);
        tvError       = findViewById(R.id.tv_pin_error);
        dots = new View[]{
            findViewById(R.id.pin_dot_1),
            findViewById(R.id.pin_dot_2),
            findViewById(R.id.pin_dot_3),
            findViewById(R.id.pin_dot_4)
        };

        setupKeypad();
        updateUIState();
    }

    private void setupKeypad() {
        int[] digitIds = {R.id.key_1, R.id.key_2, R.id.key_3,
                          R.id.key_4, R.id.key_5, R.id.key_6,
                          R.id.key_7, R.id.key_8, R.id.key_9};
        for (int i = 0; i < digitIds.length; i++) {
            final String digit = String.valueOf(i + 1);
            findViewById(digitIds[i]).setOnClickListener(v -> onDigit(digit));
        }
        findViewById(R.id.key_0).setOnClickListener(v -> onDigit("0"));
        findViewById(R.id.key_backspace).setOnClickListener(v -> onBackspace());
    }

    private void onDigit(String d) {
        if (pin.length() >= 4) return;
        tvError.setVisibility(View.GONE);
        pin.append(d);
        updateDots();
        if (pin.length() == 4) handleComplete();
    }

    private void onBackspace() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
            updateDots();
            tvError.setVisibility(View.GONE);
        }
    }

    private void updateDots() {
        for (int i = 0; i < 4; i++) {
            dots[i].setBackgroundResource(
                i < pin.length() ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty
            );
        }
    }

    private void handleComplete() {
        if (currentState == State.ENTER_NEW) {
            firstPin = pin.toString();
            currentState = State.CONFIRM_NEW;
            updateUIState();
        } else {
            if (firstPin.equals(pin.toString())) {
                savePin(firstPin);
            } else {
                tvError.setText("ПИН-коды не совпадают");
                tvError.setVisibility(View.VISIBLE);
                firstPin = "";
                currentState = State.ENTER_NEW;
                updateUIState();
            }
        }
    }

    private void updateUIState() {
        pin.setLength(0);
        updateDots();
        tvError.setVisibility(View.GONE);

        if (currentState == State.ENTER_NEW) {
            tvInstruction.setText(isChangeMode ? "Введите новый ПИН-код" : "Придумайте ПИН-код");
        } else {
            tvInstruction.setText("Повторите ПИН-код");
        }
    }

    private void savePin(String rawPin) {
        String hashedPin = SecurityUtils.hashPin(rawPin);
        if (hashedPin == null) {
            tvError.setText("Ошибка сохранения ПИН-кода");
            tvError.setVisibility(View.VISIBLE);
            firstPin = "";
            currentState = State.ENTER_NEW;
            updateUIState();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean ok = prefs.edit().putString(KEY_PIN_HASH, hashedPin).putBoolean(KEY_PIN_SET, true).commit();

        if (ok) {
            Toast.makeText(this, "ПИН-код установлен!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } else {
            tvError.setText("Ошибка сохранения ПИН-кода");
            tvError.setVisibility(View.VISIBLE);
            firstPin = "";
            currentState = State.ENTER_NEW;
            updateUIState();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentState == State.CONFIRM_NEW) {
            currentState = State.ENTER_NEW;
            firstPin = "";
            updateUIState();
        } else {
            super.onBackPressed();
            finishAffinity();
        }
    }
}
