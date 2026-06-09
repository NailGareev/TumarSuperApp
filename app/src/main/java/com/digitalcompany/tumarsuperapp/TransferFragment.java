package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.digitalcompany.tumarsuperapp.network.models.UserLookupResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferFragment extends Fragment {

    private static final String TAG = "TransferFragment";
    private static final long LOOKUP_DEBOUNCE_MS = 600;

    private enum TransferMode { PHONE, CARD, INTERNATIONAL }

    private TransferMode currentMode = TransferMode.PHONE;
    private String selectedCurrency = "USD";
    private boolean recipientFound = false;
    private boolean transferring = false;

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private LinearLayout tabPhone, tabCard, tabInternational;
    private TextView tabPhoneTitle, tabCardTitle, tabIntlTitle;

    // ── Sections ──────────────────────────────────────────────────────────────
    private LinearLayout sectionPhone, sectionCard, sectionInternational, sectionRecent;

    // ── Phone inputs ──────────────────────────────────────────────────────────
    private EditText etRecipientPhone, etPhoneComment;

    // ── Recipient lookup ──────────────────────────────────────────────────────
    private LinearLayout cardRecipientInfo, cardRecipientNotFound, cardLookupLoading;
    private TextView tvRecipientName;

    // ── Card inputs ───────────────────────────────────────────────────────────
    private EditText etRecipientCard;

    // ── International inputs ──────────────────────────────────────────────────
    private EditText etIntlCountry, etIntlRecipient, etIntlIban, etIntlSwift, etIntlPurpose;
    private TextView chipUsd, chipEur, chipRub, chipGbp;

    // ── Shared ────────────────────────────────────────────────────────────────
    private EditText etTransferAmount;
    private LinearLayout llBtnConfirm;
    private TextView tvCtaLabel, tvAvailableBalance;
    private ProgressBar progressBarTransfer;

    private ApiService apiService;
    private final Handler lookupHandler = new Handler(Looper.getMainLooper());
    private Runnable lookupRunnable;
    private Call<UserLookupResponse> pendingLookup;

    public TransferFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null)
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Back button
        view.findViewById(R.id.btn_transfer_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Balance
        tvAvailableBalance = view.findViewById(R.id.tv_available_balance);
        loadBalance();

        // Tabs
        tabPhone = view.findViewById(R.id.tab_phone);
        tabCard = view.findViewById(R.id.tab_card);
        tabInternational = view.findViewById(R.id.tab_international);
        tabPhoneTitle = view.findViewById(R.id.tab_phone_title);
        tabCardTitle = view.findViewById(R.id.tab_card_title);
        tabIntlTitle = view.findViewById(R.id.tab_intl_title);

        // Sections
        sectionPhone = view.findViewById(R.id.section_phone);
        sectionCard = view.findViewById(R.id.section_card);
        sectionInternational = view.findViewById(R.id.section_international);
        sectionRecent = view.findViewById(R.id.section_recent);

        // Phone
        etRecipientPhone = view.findViewById(R.id.et_recipient_phone);
        etPhoneComment = view.findViewById(R.id.et_phone_comment);
        cardRecipientInfo = view.findViewById(R.id.card_recipient_info);
        cardRecipientNotFound = view.findViewById(R.id.card_recipient_not_found);
        cardLookupLoading = view.findViewById(R.id.card_lookup_loading);
        tvRecipientName = view.findViewById(R.id.tv_recipient_name);

        // Card
        etRecipientCard = view.findViewById(R.id.et_recipient_card);

        // International
        etIntlCountry = view.findViewById(R.id.et_intl_country);
        etIntlRecipient = view.findViewById(R.id.et_intl_recipient);
        etIntlIban = view.findViewById(R.id.et_intl_iban);
        etIntlSwift = view.findViewById(R.id.et_intl_swift);
        etIntlPurpose = view.findViewById(R.id.et_intl_purpose);
        chipUsd = view.findViewById(R.id.chip_usd);
        chipEur = view.findViewById(R.id.chip_eur);
        chipRub = view.findViewById(R.id.chip_rub);
        chipGbp = view.findViewById(R.id.chip_gbp);

        // Shared
        etTransferAmount = view.findViewById(R.id.et_transfer_amount);
        progressBarTransfer = view.findViewById(R.id.progressBarTransfer);
        llBtnConfirm = view.findViewById(R.id.btn_confirm_transfer);
        tvCtaLabel = view.findViewById(R.id.tv_cta_label);

        // Tab clicks
        tabPhone.setOnClickListener(v -> switchMode(TransferMode.PHONE));
        tabCard.setOnClickListener(v -> switchMode(TransferMode.CARD));
        tabInternational.setOnClickListener(v -> switchMode(TransferMode.INTERNATIONAL));

        // Currency chips
        chipUsd.setOnClickListener(v -> selectCurrency("USD"));
        chipEur.setOnClickListener(v -> selectCurrency("EUR"));
        chipRub.setOnClickListener(v -> selectCurrency("RUB"));
        chipGbp.setOnClickListener(v -> selectCurrency("GBP"));

        // Phone auto-format + debounced lookup
        etRecipientPhone.addTextChangedListener(new PhoneFormatWatcher(etRecipientPhone) {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                clearInputError(etRecipientPhone);
                hideLookupCards();
                recipientFound = false;
                updateCtaLabel();
                if (PhoneFormatWatcher.isComplete(etRecipientPhone)) {
                    scheduleLookup(PhoneFormatWatcher.raw(etRecipientPhone));
                } else {
                    cancelPendingLookup();
                }
            }
        });

        // Card auto-format
        etRecipientCard.addTextChangedListener(new CardNumberFormatter());

        // Amount watcher — update CTA label
        etTransferAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                clearInputError(etTransferAmount);
                updateCtaLabel();
            }
        });

        // Quick amount chips
        view.findViewById(R.id.chip_1000).setOnClickListener(v -> setAmount("1000"));
        view.findViewById(R.id.chip_5000).setOnClickListener(v -> setAmount("5000"));
        view.findViewById(R.id.chip_10000).setOnClickListener(v -> setAmount("10000"));
        view.findViewById(R.id.chip_25000).setOnClickListener(v -> setAmount("25000"));

        llBtnConfirm.setOnClickListener(v -> attemptTransfer());

        // Initial state
        switchMode(TransferMode.PHONE);
        selectCurrency("USD");
    }

    private void setAmount(String amount) {
        etTransferAmount.setText(amount);
        etTransferAmount.setSelection(amount.length());
        updateCtaLabel();
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
        clearAllErrors();

        // Reset all tabs to inactive
        tabPhone.setBackground(null);
        tabCard.setBackground(null);
        tabInternational.setBackground(null);
        tabPhoneTitle.setTextColor(0xFF777777);
        tabCardTitle.setTextColor(0xFF777777);
        tabIntlTitle.setTextColor(0xFF777777);

        // Hide all sections
        sectionPhone.setVisibility(View.GONE);
        sectionCard.setVisibility(View.GONE);
        sectionInternational.setVisibility(View.GONE);
        if (sectionRecent != null) sectionRecent.setVisibility(View.GONE);

        // Activate selected tab
        int activeColor = 0xFF6200EE;
        switch (mode) {
            case PHONE:
                tabPhone.setBackgroundResource(R.drawable.bg_segment_active);
                tabPhoneTitle.setTextColor(activeColor);
                sectionPhone.setVisibility(View.VISIBLE);
                if (sectionRecent != null) sectionRecent.setVisibility(View.VISIBLE);
                break;
            case CARD:
                tabCard.setBackgroundResource(R.drawable.bg_segment_active);
                tabCardTitle.setTextColor(activeColor);
                sectionCard.setVisibility(View.VISIBLE);
                break;
            case INTERNATIONAL:
                tabInternational.setBackgroundResource(R.drawable.bg_segment_active);
                tabIntlTitle.setTextColor(activeColor);
                sectionInternational.setVisibility(View.VISIBLE);
                break;
        }
        updateCtaLabel();
    }

    // ── Currency selection ────────────────────────────────────────────────────

    private void selectCurrency(String currency) {
        selectedCurrency = currency;
        int activeText = 0xFFFFFFFF;
        int inactiveText = 0xFF6200EE;
        chipUsd.setBackgroundResource("USD".equals(currency) ? R.drawable.bg_chip_purple_active : R.drawable.bg_chip_purple);
        chipEur.setBackgroundResource("EUR".equals(currency) ? R.drawable.bg_chip_purple_active : R.drawable.bg_chip_purple);
        chipRub.setBackgroundResource("RUB".equals(currency) ? R.drawable.bg_chip_purple_active : R.drawable.bg_chip_purple);
        chipGbp.setBackgroundResource("GBP".equals(currency) ? R.drawable.bg_chip_purple_active : R.drawable.bg_chip_purple);
        chipUsd.setTextColor("USD".equals(currency) ? activeText : inactiveText);
        chipEur.setTextColor("EUR".equals(currency) ? activeText : inactiveText);
        chipRub.setTextColor("RUB".equals(currency) ? activeText : inactiveText);
        chipGbp.setTextColor("GBP".equals(currency) ? activeText : inactiveText);
        updateCtaLabel();
    }

    // ── CTA label ─────────────────────────────────────────────────────────────

    private void updateCtaLabel() {
        if (tvCtaLabel == null) return;
        String amountStr = etTransferAmount != null && etTransferAmount.getText() != null
                ? etTransferAmount.getText().toString().trim() : "";

        switch (currentMode) {
            case PHONE:
                if (!amountStr.isEmpty() && recipientFound && tvRecipientName != null
                        && tvRecipientName.getText().length() > 0) {
                    tvCtaLabel.setText("Перевести ₸ " + amountStr + " → " + tvRecipientName.getText());
                } else {
                    tvCtaLabel.setText("Перевести");
                }
                break;
            case CARD:
                tvCtaLabel.setText(amountStr.isEmpty() ? "Перевести" : "Перевести ₸ " + amountStr);
                break;
            case INTERNATIONAL:
                tvCtaLabel.setText(amountStr.isEmpty() ? "Перевести"
                        : "Перевести " + selectedCurrency + " " + amountStr);
                break;
        }
    }

    // ── Balance loading ───────────────────────────────────────────────────────

    private void loadBalance() {
        if (getContext() == null || tvAvailableBalance == null) return;
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("CardDataPrefs", Context.MODE_PRIVATE);
            for (int i = 0; i < 5; i++) {
                String balance = prefs.getString("card_" + i + "_balance", null);
                String blocked = prefs.getString("card_" + i + "_is_blocked", "false");
                if (balance != null && !"true".equals(blocked)) {
                    try {
                        long bal = (long) Double.parseDouble(balance);
                        tvAvailableBalance.setText(String.format("₸ %,d", bal).replace(',', ' '));
                    } catch (NumberFormatException e) {
                        tvAvailableBalance.setText("₸ " + balance);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Balance load error: " + e.getMessage());
        }
        tvAvailableBalance.setText("₸ —");
    }

    // ── Phone lookup ──────────────────────────────────────────────────────────

    private void scheduleLookup(String phone) {
        cancelPendingLookup();
        if (cardLookupLoading != null) cardLookupLoading.setVisibility(View.VISIBLE);
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
                if (cardLookupLoading != null) cardLookupLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    recipientFound = true;
                    if (tvRecipientName != null) tvRecipientName.setText(response.body().getDisplayName());
                    if (cardRecipientInfo != null) cardRecipientInfo.setVisibility(View.VISIBLE);
                    if (cardRecipientNotFound != null) cardRecipientNotFound.setVisibility(View.GONE);
                    updateCtaLabel();
                } else {
                    recipientFound = false;
                    if (cardRecipientInfo != null) cardRecipientInfo.setVisibility(View.GONE);
                    if (cardRecipientNotFound != null) cardRecipientNotFound.setVisibility(View.VISIBLE);
                    updateCtaLabel();
                }
            }

            @Override
            public void onFailure(Call<UserLookupResponse> call, Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                if (cardLookupLoading != null) cardLookupLoading.setVisibility(View.GONE);
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
        if (transferring) return;
        clearInputError(etTransferAmount);
        String amountStr = etTransferAmount.getText() != null
                ? etTransferAmount.getText().toString().trim() : "";

        BigDecimal amount = null;
        boolean amountInvalid = false;

        if (amountStr.isEmpty()) {
            showInputError(etTransferAmount, "Введите сумму перевода");
            amountInvalid = true;
        } else {
            try {
                amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showInputError(etTransferAmount, "Сумма должна быть больше нуля");
                    amountInvalid = true;
                    amount = null;
                }
            } catch (NumberFormatException e) {
                showInputError(etTransferAmount, "Введите корректное число");
                amountInvalid = true;
            }
        }

        switch (currentMode) {
            case PHONE: transferByPhone(amount, amountInvalid); break;
            case CARD: transferByCard(amount, amountInvalid); break;
            case INTERNATIONAL: transferInternational(amount, amountInvalid); break;
        }
    }

    private void transferByPhone(BigDecimal amount, boolean amountInvalid) {
        clearInputError(etRecipientPhone);
        String phone = PhoneFormatWatcher.raw(etRecipientPhone);
        boolean invalid = amountInvalid;

        if (phone.isEmpty()) {
            showInputError(etRecipientPhone, "Введите номер телефона получателя");
            invalid = true;
        } else if (!phone.startsWith("+7") || phone.length() != 12) {
            showInputError(etRecipientPhone, "Формат: +7XXXXXXXXXX");
            invalid = true;
        } else if (!recipientFound) {
            showInputError(etRecipientPhone, "Клиент Tumar Bank не найден");
            invalid = true;
        }

        if (invalid || amount == null) return;

        String comment = text(etPhoneComment);
        showLoading(true);
        if (apiService == null) {
            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        final String recipientName = tvRecipientName != null
                ? tvRecipientName.getText().toString() : "Получатель";
        final String amountDisplay = amount.toPlainString();

        apiService.transferFunds(new TransferRequest(phone, amount, comment)).enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    navigateToSuccess(recipientName, amountDisplay, "PHONE");
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
        clearInputError(etRecipientCard);
        String card = text(etRecipientCard).replaceAll("\\s", "");
        boolean invalid = amountInvalid;

        if (card.isEmpty()) {
            showInputError(etRecipientCard, "Введите номер карты");
            invalid = true;
        } else if (!card.matches("\\d{16}")) {
            showInputError(etRecipientCard, "Номер карты должен содержать 16 цифр");
            invalid = true;
        }

        if (invalid || amount == null) return;

        Toast.makeText(getContext(),
                "Перевод на карту " + maskCard(card) + " — функция в разработке",
                Toast.LENGTH_LONG).show();
    }

    private void transferInternational(BigDecimal amount, boolean amountInvalid) {
        clearIntlErrors();
        String country = text(etIntlCountry);
        String recipient = text(etIntlRecipient);
        String iban = text(etIntlIban).replaceAll("\\s", "");
        String swift = text(etIntlSwift).trim();
        boolean invalid = amountInvalid;

        if (country.isEmpty()) {
            showInputError(etIntlCountry, "Укажите страну назначения");
            invalid = true;
        }
        if (recipient.isEmpty()) {
            showInputError(etIntlRecipient, "Введите ФИО получателя");
            invalid = true;
        } else if (!recipient.matches("[A-Za-z\\s-]+")) {
            showInputError(etIntlRecipient, "Только латинские буквы");
            invalid = true;
        }
        if (iban.isEmpty()) {
            showInputError(etIntlIban, "Введите IBAN или номер счёта");
            invalid = true;
        }
        if (swift.isEmpty()) {
            showInputError(etIntlSwift, "Введите SWIFT/BIC код банка");
            invalid = true;
        } else if (swift.length() < 8 || swift.length() > 11) {
            showInputError(etIntlSwift, "SWIFT/BIC: 8 или 11 символов");
            invalid = true;
        }

        if (invalid || amount == null) return;

        String summary = String.format(
                "Международный перевод:\n%s %s\nПолучатель: %s\nСчёт: %s\nБанк: %s\nСтрана: %s\n\n— Функция в разработке",
                amount.toPlainString(), selectedCurrency, recipient,
                iban.length() > 8 ? iban.substring(0, 4) + "..." + iban.substring(iban.length() - 4) : iban,
                swift, country);
        Toast.makeText(getContext(), summary, Toast.LENGTH_LONG).show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToSuccess(String recipientName, String amount, String method) {
        if (!isAdded()) return;
        // Clear form state
        etRecipientPhone.setText("");
        etTransferAmount.setText("");
        if (etPhoneComment != null) etPhoneComment.setText("");
        hideLookupCards();
        recipientFound = false;
        updateCtaLabel();

        TransferSuccessFragment fragment = TransferSuccessFragment.newInstance(recipientName, amount, method);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String text(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String maskCard(String card) {
        if (card.length() != 16) return card;
        return card.substring(0, 4) + " **** **** " + card.substring(12);
    }

    private void showLoading(boolean loading) {
        transferring = loading;
        if (progressBarTransfer != null)
            progressBarTransfer.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (llBtnConfirm != null) {
            llBtnConfirm.setEnabled(!loading);
            llBtnConfirm.setAlpha(loading ? 0.6f : 1.0f);
        }
    }

    private void showInputError(EditText et, String msg) {
        if (et != null) {
            et.setBackgroundResource(R.drawable.bg_transfer_input_error);
            et.requestFocus();
        }
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void clearInputError(EditText et) {
        if (et == null) return;
        if (et == etTransferAmount) {
            et.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } else {
            et.setBackgroundResource(R.drawable.bg_topup_input_field);
        }
    }

    private void clearAllErrors() {
        clearInputError(etRecipientPhone);
        clearInputError(etRecipientCard);
        clearInputError(etTransferAmount);
        clearIntlErrors();
    }

    private void clearIntlErrors() {
        clearInputError(etIntlCountry);
        clearInputError(etIntlRecipient);
        clearInputError(etIntlIban);
        clearInputError(etIntlSwift);
        if (etIntlPurpose != null) clearInputError(etIntlPurpose);
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
