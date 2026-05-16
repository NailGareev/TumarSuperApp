package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.TransferRequest;
import com.digitalcompany.tumarsuperapp.network.models.TransferResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserLookupResponse;
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
    private static final long LOOKUP_DEBOUNCE_MS = 600;

    private enum TransferMode { PHONE, CARD }
    private TransferMode currentMode = TransferMode.PHONE;

    // Tabs
    private LinearLayout tabPhone, tabCard;
    private TextView tabPhoneTitle, tabPhoneSub, tabCardTitle, tabCardSub;
    private LinearLayout sectionPhone, sectionCard;

    // Phone inputs
    private TextInputLayout tilRecipientPhone;
    private TextInputEditText etRecipientPhone;

    // Recipient info cards
    private CardView cardRecipientInfo, cardRecipientNotFound;
    private LinearLayout cardLookupLoading;
    private TextView tvRecipientName;

    // Card inputs
    private TextInputLayout tilRecipientCard;
    private TextInputEditText etRecipientCard;

    // Amount
    private TextInputLayout tilTransferAmount;
    private TextInputEditText etTransferAmount;

    private MaterialButton btnConfirmTransfer;
    private ProgressBar progressBarTransfer;

    private ApiService apiService;
    private final Handler lookupHandler = new Handler(Looper.getMainLooper());
    private Runnable lookupRunnable;
    private Call<UserLookupResponse> pendingLookup;
    private boolean recipientFound = false;

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

        // Tabs
        tabPhone = view.findViewById(R.id.tab_phone);
        tabCard = view.findViewById(R.id.tab_card);
        tabPhoneTitle = view.findViewById(R.id.tab_phone_title);
        tabPhoneSub = view.findViewById(R.id.tab_phone_sub);
        tabCardTitle = view.findViewById(R.id.tab_card_title);
        tabCardSub = view.findViewById(R.id.tab_card_sub);
        sectionPhone = view.findViewById(R.id.section_phone);
        sectionCard = view.findViewById(R.id.section_card);

        // Phone
        tilRecipientPhone = view.findViewById(R.id.til_recipient_phone);
        etRecipientPhone = view.findViewById(R.id.et_recipient_phone);

        // Recipient lookup
        cardRecipientInfo = view.findViewById(R.id.card_recipient_info);
        cardRecipientNotFound = view.findViewById(R.id.card_recipient_not_found);
        cardLookupLoading = view.findViewById(R.id.card_lookup_loading);
        tvRecipientName = view.findViewById(R.id.tv_recipient_name);

        // Card
        tilRecipientCard = view.findViewById(R.id.til_recipient_card);
        etRecipientCard = view.findViewById(R.id.et_recipient_card);

        // Amount
        tilTransferAmount = view.findViewById(R.id.til_transfer_amount);
        etTransferAmount = view.findViewById(R.id.et_transfer_amount);

        btnConfirmTransfer = view.findViewById(R.id.btn_confirm_transfer);
        progressBarTransfer = view.findViewById(R.id.progressBarTransfer);

        tabPhone.setOnClickListener(v -> switchMode(TransferMode.PHONE));
        tabCard.setOnClickListener(v -> switchMode(TransferMode.CARD));

        // Phone lookup on text change (debounced)
        etRecipientPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilRecipientPhone.setError(null);
                hideLookupCards();
                recipientFound = false;
                String phone = s.toString().trim();
                if (phone.length() == 12 && phone.startsWith("+7")) {
                    scheduleLookup(phone);
                } else {
                    cancelPendingLookup();
                }
            }
        });

        // Card auto-format
        etRecipientCard.addTextChangedListener(new CardNumberFormatter());

        // Quick amount chips
        view.findViewById(R.id.chip_1000).setOnClickListener(v -> etTransferAmount.setText("1000"));
        view.findViewById(R.id.chip_5000).setOnClickListener(v -> etTransferAmount.setText("5000"));
        view.findViewById(R.id.chip_10000).setOnClickListener(v -> etTransferAmount.setText("10000"));

        btnConfirmTransfer.setOnClickListener(v -> attemptTransfer());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelPendingLookup();
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private void switchMode(TransferMode mode) {
        currentMode = mode;
        hideLookupCards();
        recipientFound = false;
        tilRecipientPhone.setError(null);
        tilRecipientCard.setError(null);

        if (mode == TransferMode.PHONE) {
            tabPhone.setBackgroundResource(R.drawable.bg_tab_active);
            tabCard.setBackgroundResource(R.drawable.bg_tab_inactive);
            tabPhoneTitle.setTextColor(0xFFFFFFFF);
            tabPhoneSub.setTextColor(0xCCFFFFFF);
            tabCardTitle.setTextColor(0xFF6200EE);
            tabCardSub.setTextColor(0xFF757575);
            sectionPhone.setVisibility(View.VISIBLE);
            sectionCard.setVisibility(View.GONE);
        } else {
            tabCard.setBackgroundResource(R.drawable.bg_tab_active);
            tabPhone.setBackgroundResource(R.drawable.bg_tab_inactive);
            tabCardTitle.setTextColor(0xFFFFFFFF);
            tabCardSub.setTextColor(0xCCFFFFFF);
            tabPhoneTitle.setTextColor(0xFF6200EE);
            tabPhoneSub.setTextColor(0xFF757575);
            sectionCard.setVisibility(View.VISIBLE);
            sectionPhone.setVisibility(View.GONE);
        }
    }

    // ── Phone lookup ──────────────────────────────────────────────────────────

    private void scheduleLookup(String phone) {
        cancelPendingLookup();
        cardLookupLoading.setVisibility(View.VISIBLE);
        lookupRunnable = () -> lookupUser(phone);
        lookupHandler.postDelayed(lookupRunnable, LOOKUP_DEBOUNCE_MS);
    }

    private void cancelPendingLookup() {
        if (lookupRunnable != null) {
            lookupHandler.removeCallbacks(lookupRunnable);
            lookupRunnable = null;
        }
        if (pendingLookup != null) {
            pendingLookup.cancel();
            pendingLookup = null;
        }
    }

    private void lookupUser(String phone) {
        if (apiService == null || !isAdded()) return;
        pendingLookup = apiService.lookupUserByPhone(phone);
        pendingLookup.enqueue(new Callback<UserLookupResponse>() {
            @Override
            public void onResponse(Call<UserLookupResponse> call, Response<UserLookupResponse> response) {
                if (!isAdded() || getContext() == null || call.isCanceled()) return;
                cardLookupLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    recipientFound = true;
                    tvRecipientName.setText(response.body().getDisplayName());
                    cardRecipientInfo.setVisibility(View.VISIBLE);
                    cardRecipientNotFound.setVisibility(View.GONE);
                } else {
                    recipientFound = false;
                    cardRecipientInfo.setVisibility(View.GONE);
                    cardRecipientNotFound.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<UserLookupResponse> call, Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                cardLookupLoading.setVisibility(View.GONE);
                Log.w(TAG, "Lookup failed: " + t.getMessage());
            }
        });
    }

    private void hideLookupCards() {
        if (cardRecipientInfo != null) cardRecipientInfo.setVisibility(View.GONE);
        if (cardRecipientNotFound != null) cardRecipientNotFound.setVisibility(View.GONE);
        if (cardLookupLoading != null) cardLookupLoading.setVisibility(View.GONE);
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    private void attemptTransfer() {
        tilTransferAmount.setError(null);
        String amountInput = etTransferAmount.getText() != null
                ? etTransferAmount.getText().toString().trim() : "";

        BigDecimal amount = null;
        boolean amountInvalid = false;

        if (amountInput.isEmpty()) {
            tilTransferAmount.setError("Введите сумму перевода");
            etTransferAmount.requestFocus();
            amountInvalid = true;
        } else {
            try {
                amount = new BigDecimal(amountInput).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    tilTransferAmount.setError("Сумма должна быть больше нуля");
                    etTransferAmount.requestFocus();
                    amountInvalid = true;
                    amount = null;
                }
            } catch (NumberFormatException e) {
                tilTransferAmount.setError("Введите корректное число");
                etTransferAmount.requestFocus();
                amountInvalid = true;
            }
        }

        if (currentMode == TransferMode.PHONE) {
            transferByPhone(amount, amountInvalid);
        } else {
            transferByCard(amount, amountInvalid);
        }
    }

    private void transferByPhone(BigDecimal amount, boolean amountInvalid) {
        tilRecipientPhone.setError(null);
        String phone = etRecipientPhone.getText() != null
                ? etRecipientPhone.getText().toString().trim() : "";
        boolean invalid = amountInvalid;

        if (phone.isEmpty()) {
            tilRecipientPhone.setError("Введите номер телефона получателя");
            etRecipientPhone.requestFocus();
            invalid = true;
        } else if (!phone.startsWith("+7") || phone.length() != 12) {
            tilRecipientPhone.setError("Формат: +7XXXXXXXXXX");
            etRecipientPhone.requestFocus();
            invalid = true;
        } else if (!recipientFound) {
            tilRecipientPhone.setError("Клиент Tumar Bank не найден");
            etRecipientPhone.requestFocus();
            invalid = true;
        }

        if (invalid || amount == null) return;

        showLoading(true);
        if (apiService == null) {
            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        apiService.transferFunds(new TransferRequest(phone, amount)).enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Перевод выполнен успешно!", Toast.LENGTH_LONG).show();
                    etRecipientPhone.setText("");
                    etTransferAmount.setText("");
                    hideLookupCards();
                    recipientFound = false;
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Не удалось выполнить перевод";
                    Toast.makeText(getContext(), "Ошибка: " + msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TransferResponse> call, Throwable t) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void transferByCard(BigDecimal amount, boolean amountInvalid) {
        tilRecipientCard.setError(null);
        String card = etRecipientCard.getText() != null
                ? etRecipientCard.getText().toString().replaceAll("\\s", "") : "";
        boolean invalid = amountInvalid;

        if (card.isEmpty()) {
            tilRecipientCard.setError("Введите номер карты получателя");
            etRecipientCard.requestFocus();
            invalid = true;
        } else if (!card.matches("\\d{16}")) {
            tilRecipientCard.setError("Номер карты должен содержать 16 цифр");
            etRecipientCard.requestFocus();
            invalid = true;
        }

        if (invalid || amount == null) return;

        Toast.makeText(getContext(),
                "Перевод на карту " + maskCard(card) + " — функция в разработке",
                Toast.LENGTH_LONG).show();
    }

    private String maskCard(String card) {
        if (card.length() != 16) return card;
        return card.substring(0, 4) + " **** **** " + card.substring(12);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarTransfer != null)
            progressBarTransfer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnConfirmTransfer != null)
            btnConfirmTransfer.setEnabled(!isLoading);
    }

    // ── Card number formatter ─────────────────────────────────────────────────

    private static class CardNumberFormatter implements TextWatcher {
        private boolean formatting;

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (formatting) return;
            formatting = true;
            String digits = s.toString().replaceAll("\\s", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append(' ');
                sb.append(digits.charAt(i));
            }
            s.replace(0, s.length(), sb.toString());
            formatting = false;
        }
    }
}
