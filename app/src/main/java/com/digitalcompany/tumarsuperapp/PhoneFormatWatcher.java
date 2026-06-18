package com.digitalcompany.tumarsuperapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Formats a Kazakhstan phone number as +7 (XXX) XXX XX XX while typing.
 * Use {@link #raw(EditText)} to get the stripped +7XXXXXXXXXX value for API calls.
 */
public class PhoneFormatWatcher implements TextWatcher {

    private final EditText editText;
    private boolean formatting;

    public PhoneFormatWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (formatting) return;
        formatting = true;

        // Extract only digits
        String digits = s.toString().replaceAll("[^0-9]", "");

        // Ensure starts with 7 (Kazakhstan)
        if (!digits.startsWith("7")) {
            digits = "7" + digits;
        }
        // Cap at 11 digits (+7 + 10 local digits)
        if (digits.length() > 11) {
            digits = digits.substring(0, 11);
        }

        String formatted = format(digits);
        s.replace(0, s.length(), formatted);
        // Move cursor to end
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().length());
        }

        formatting = false;
    }

    private static String format(String digits) {
        if (digits.isEmpty()) return "";
        int len = digits.length();
        StringBuilder sb = new StringBuilder("+7");
        if (len <= 1) return sb.toString();

        // Area code — open paren, up to 3 digits; close paren + space only when group 3 starts
        sb.append(" (").append(digits, 1, Math.min(4, len));
        if (len <= 4) return sb.toString();

        sb.append(") ").append(digits, 4, Math.min(7, len));
        if (len <= 7) return sb.toString();

        sb.append(" ").append(digits, 7, Math.min(9, len));
        if (len <= 9) return sb.toString();

        sb.append(" ").append(digits, 9, Math.min(11, len));
        return sb.toString();
    }

    /** Returns the raw +7XXXXXXXXXX value stripped of formatting. */
    public static String raw(EditText et) {
        String text = et.getText() != null ? et.getText().toString() : "";
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.length() < 11) return text.replaceAll("[^+0-9]", "");
        return "+7" + digits.substring(1);
    }

    /** Returns true when the field contains a full 11-digit KZ number (+7 + 10 digits). */
    public static boolean isComplete(EditText et) {
        return raw(et).length() == 12;
    }
}
