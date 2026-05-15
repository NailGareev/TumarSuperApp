package com.digitalcompany.tumarsuperapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class PinEntryActivity extends AppCompatActivity {

    private static final String TAG = "PinEntryActivity";
    private static final String PIN_PREFS_NAME = "PinSecurityPrefs";
    private static final String KEY_PIN_SET = "pin_is_set";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private TextView tvError;
    private View[] dots;
    private final StringBuilder pin = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences userPrefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences pinPrefs  = getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE);

        boolean isLoggedIn = userPrefs.getBoolean(KEY_IS_LOGGED_IN, false);
        boolean isPinSet   = pinPrefs.getBoolean(KEY_PIN_SET, false);
        String  token      = userPrefs.getString(KEY_AUTH_TOKEN, null);
        String  pinHash    = pinPrefs.getString(KEY_PIN_HASH, null);

        if (isLoggedIn && token != null && isPinSet && pinHash != null) {
            setContentView(R.layout.activity_pin_entry);
            initViews();
            setupKeypad();
        } else if (isLoggedIn && token != null) {
            navigateTo(PinSetupActivity.class);
        } else {
            navigateTo(LoginActivity.class);
        }
    }

    private void initViews() {
        tvError = findViewById(R.id.tv_pin_entry_error);
        dots = new View[]{
            findViewById(R.id.pin_dot_1),
            findViewById(R.id.pin_dot_2),
            findViewById(R.id.pin_dot_3),
            findViewById(R.id.pin_dot_4)
        };
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
        if (pin.length() == 4) verifyPin();
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

    private void verifyPin() {
        SharedPreferences pinPrefs = getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE);
        String storedHash = pinPrefs.getString(KEY_PIN_HASH, null);

        if (storedHash == null) {
            showError("Ошибка входа. Переустановите приложение.");
            return;
        }

        if (SecurityUtils.verifyPin(pin.toString(), storedHash)) {
            if (!storedHash.contains(":")) {
                String newHash = SecurityUtils.hashPin(pin.toString());
                if (newHash != null) pinPrefs.edit().putString(KEY_PIN_HASH, newHash).apply();
            }
            navigateTo(MainActivity.class);
        } else {
            showError("Неверный ПИН-код");
            pin.setLength(0);
            updateDots();
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
