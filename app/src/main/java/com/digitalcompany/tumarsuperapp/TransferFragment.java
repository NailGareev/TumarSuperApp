package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.TransferRequest;
import com.digitalcompany.tumarsuperapp.network.models.TransferResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferFragment extends Fragment {

    private static final String TAG = "TransferFragment";

    private enum TransferMode { PHONE, CARD }
    private TransferMode currentMode = TransferMode.PHONE;

    // Tabs
    private LinearLayout tabPhone, tabCard;
    private LinearLayout sectionPhone, sectionCard;

    // Phone inputs
    private TextInputLayout tilRecipientPhone;
    private TextInputEditText etRecipientPhone;

    // Card inputs
    private TextInputLayout tilRecipientCard;
    private TextInputEditText etRecipientCard;

    // Amount
    private TextInputLayout tilTransferAmount;
    private TextInputEditText etTransferAmount;

    private MaterialButton btnConfirmTransfer;
    private ProgressBar progressBarTransfer;

    private ApiService apiService;

    public TransferFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabPhone = view.findViewById(R.id.tab_phone);
        tabCard = view.findViewById(R.id.tab_card);
        sectionPhone = view.findViewById(R.id.section_phone);
        sectionCard = view.findViewById(R.id.section_card);

        tilRecipientPhone = view.findViewById(R.id.til_recipient_phone);
        etRecipientPhone = view.findViewById(R.id.et_recipient_phone);
        tilRecipientCard = view.findViewById(R.id.til_recipient_card);
        etRecipientCard = view.findViewById(R.id.et_recipient_card);
        tilTransferAmount = view.findViewById(R.id.til_transfer_amount);
        etTransferAmount = view.findViewById(R.id.et_transfer_amount);
        btnConfirmTransfer = view.findViewById(R.id.btn_confirm_transfer);
        progressBarTransfer = view.findViewById(R.id.progressBarTransfer);

        tabPhone.setOnClickListener(v -> switchMode(TransferMode.PHONE));
        tabCard.setOnClickListener(v -> switchMode(TransferMode.CARD));

        // Auto-format card number with spaces (XXXX XXXX XXXX XXXX)
        etRecipientCard.addTextChangedListener(new CardNumberFormatter());

        // Quick amount chips
        view.findViewById(R.id.chip_1000).setOnClickListener(v -> etTransferAmount.setText("1000"));
        view.findViewById(R.id.chip_5000).setOnClickListener(v -> etTransferAmount.setText("5000"));
        view.findViewById(R.id.chip_10000).setOnClickListener(v -> etTransferAmount.setText("10000"));

        btnConfirmTransfer.setOnClickListener(v -> attemptTransfer());
    }

    private void switchMode(TransferMode mode) {
        currentMode = mode;

        if (mode == TransferMode.PHONE) {
            tabPhone.setBackgroundResource(R.drawable.bg_tab_active);
            tabCard.setBackgroundResource(R.drawable.bg_tab_inactive);
            ((TextView) tabPhone.findViewWithTag(null) != null
                    ? tabPhone.getChildAt(1) : tabPhone.getChildAt(1))
                    .setVisibility(View.VISIBLE);

            sectionPhone.setVisibility(View.VISIBLE);
            sectionCard.setVisibility(View.GONE);
            btnConfirmTransfer.setText("Перевести");

            // Reset tab label colors
            setTabLabelColors(tabPhone, "#FFFFFF", "#CCFFFFFF");
            setTabLabelColors(tabCard, "#6200EE", "#757575");

            tilRecipientCard.setError(null);
        } else {
            tabCard.setBackgroundResource(R.drawable.bg_tab_active);
            tabPhone.setBackgroundResource(R.drawable.bg_tab_inactive);

            sectionCard.setVisibility(View.VISIBLE);
            sectionPhone.setVisibility(View.GONE);
            btnConfirmTransfer.setText("Перевести");

            setTabLabelColors(tabCard, "#FFFFFF", "#CCFFFFFF");
            setTabLabelColors(tabPhone, "#6200EE", "#757575");

            tilRecipientPhone.setError(null);
        }
    }

    private void setTabLabelColors(LinearLayout tab, String labelColor, String sublabelColor) {
        if (tab.getChildCount() >= 3) {
            ((TextView) tab.getChildAt(1)).setTextColor(android.graphics.Color.parseColor(labelColor));
            ((TextView) tab.getChildAt(2)).setTextColor(android.graphics.Color.parseColor(sublabelColor));
        }
    }

    private void attemptTransfer() {
        tilTransferAmount.setError(null);

        String amountInput = etTransferAmount.getText() != null
                ? etTransferAmount.getText().toString().trim() : "";

        BigDecimal amount = null;
        boolean cancel = false;

        if (amountInput.isEmpty()) {
            tilTransferAmount.setError("Введите сумму перевода");
            etTransferAmount.requestFocus();
            cancel = true;
        } else {
            try {
                amount = new BigDecimal(amountInput).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    tilTransferAmount.setError("Сумма должна быть больше нуля");
                    etTransferAmount.requestFocus();
                    cancel = true;
                    amount = null;
                }
            } catch (NumberFormatException e) {
                tilTransferAmount.setError("Введите корректное число");
                etTransferAmount.requestFocus();
                cancel = true;
            }
        }

        if (currentMode == TransferMode.PHONE) {
            cancel = validateAndTransferByPhone(amount, cancel);
        } else {
            cancel = validateAndTransferByCard(amount, cancel);
        }
    }

    private boolean validateAndTransferByPhone(BigDecimal amount, boolean amountInvalid) {
        tilRecipientPhone.setError(null);

        String phoneInput = etRecipientPhone.getText() != null
                ? etRecipientPhone.getText().toString().trim() : "";
        boolean cancel = amountInvalid;

        if (phoneInput.isEmpty()) {
            tilRecipientPhone.setError("Введите номер телефона получателя");
            etRecipientPhone.requestFocus();
            cancel = true;
        } else if (!phoneInput.startsWith("+7") || phoneInput.length() != 12) {
            tilRecipientPhone.setError("Формат: +7XXXXXXXXXX (12 символов)");
            etRecipientPhone.requestFocus();
            cancel = true;
        } else if (!phoneInput.substring(1).matches("\\d+")) {
            tilRecipientPhone.setError("Только цифры после +");
            etRecipientPhone.requestFocus();
            cancel = true;
        }

        if (cancel) return true;

        Log.d(TAG, "Transfer by phone to " + phoneInput + ", amount " + amount);
        showLoading(true);

        TransferRequest request = new TransferRequest(phoneInput, amount);

        if (apiService == null) {
            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return true;
        }

        apiService.transferFunds(request).enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    TransferResponse result = response.body();
                    if (result.isSuccess()) {
                        Toast.makeText(getContext(), "Перевод выполнен успешно!", Toast.LENGTH_LONG).show();
                        etRecipientPhone.setText("");
                        etTransferAmount.setText("");
                    } else {
                        String msg = result.getMessage() != null ? result.getMessage() : "Не удалось выполнить перевод";
                        if (msg.toLowerCase().contains("recipient") || msg.toLowerCase().contains("получател")) {
                            tilRecipientPhone.setError(msg);
                        } else {
                            Toast.makeText(getContext(), "Ошибка: " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Ошибка сервера (" + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TransferResponse> call, Throwable t) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Network error", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        return false;
    }

    private boolean validateAndTransferByCard(BigDecimal amount, boolean amountInvalid) {
        tilRecipientCard.setError(null);

        String cardInput = etRecipientCard.getText() != null
                ? etRecipientCard.getText().toString().replaceAll("\\s", "") : "";
        boolean cancel = amountInvalid;

        if (cardInput.isEmpty()) {
            tilRecipientCard.setError("Введите номер карты получателя");
            etRecipientCard.requestFocus();
            cancel = true;
        } else if (!cardInput.matches("\\d{16}")) {
            tilRecipientCard.setError("Номер карты должен содержать 16 цифр");
            etRecipientCard.requestFocus();
            cancel = true;
        }

        if (cancel) return true;

        // Card transfers — feature in development, show informational message
        Toast.makeText(getContext(),
                "Перевод по карте №" + maskCard(cardInput) + " на сумму " +
                        amount.toPlainString() + " ₸ — функция в разработке",
                Toast.LENGTH_LONG).show();

        return false;
    }

    private String maskCard(String card) {
        if (card.length() != 16) return card;
        return card.substring(0, 4) + " **** **** " + card.substring(12);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarTransfer != null) {
            progressBarTransfer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnConfirmTransfer != null) {
            btnConfirmTransfer.setEnabled(!isLoading);
        }
    }

    // Auto-formats card input as: XXXX XXXX XXXX XXXX
    private static class CardNumberFormatter implements TextWatcher {
        private boolean isFormatting;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (isFormatting) return;
            isFormatting = true;

            String digits = s.toString().replaceAll("\\s", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(' ');
                formatted.append(digits.charAt(i));
            }

            s.replace(0, s.length(), formatted.toString());
            isFormatting = false;
        }
    }
}
