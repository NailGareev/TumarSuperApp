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

    private enum TransferMode { PHONE, CARD, INTERNATIONAL }

    private TransferMode currentMode = TransferMode.PHONE;
    private String selectedCurrency = "USD";

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private LinearLayout tabPhone, tabCard, tabInternational;
    private TextView tabPhoneTitle, tabPhoneSub;
    private TextView tabCardTitle, tabCardSub;
    private TextView tabIntlTitle, tabIntlSub;

    // ── Sections ──────────────────────────────────────────────────────────────
    private LinearLayout sectionPhone, sectionCard, sectionInternational;

    // ── Phone inputs ──────────────────────────────────────────────────────────
    private TextInputLayout tilRecipientPhone;
    private TextInputEditText etRecipientPhone;

    // ── Recipient lookup ──────────────────────────────────────────────────────
    private CardView cardRecipientInfo, cardRecipientNotFound;
    private LinearLayout cardLookupLoading;
    private TextView tvRecipientName;
    private boolean recipientFound = false;

    // ── Card inputs ───────────────────────────────────────────────────────────
    private TextInputLayout tilRecipientCard;
    private TextInputEditText etRecipientCard;

    // ── International inputs ──────────────────────────────────────────────────
    private TextInputLayout tilIntlCountry, tilIntlRecipient, tilIntlIban, tilIntlSwift, tilIntlPurpose;
    private TextInputEditText etIntlCountry, etIntlRecipient, etIntlIban, etIntlSwift, etIntlPurpose;
    private TextView chipUsd, chipEur, chipRub, chipGbp;

    // ── Shared ────────────────────────────────────────────────────────────────
    private TextInputLayout tilTransferAmount;
    private TextInputEditText etTransferAmount;
    private MaterialButton btnConfirmTransfer;
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

        // Tabs
        tabPhone = view.findViewById(R.id.tab_phone);
        tabCard = view.findViewById(R.id.tab_card);
        tabInternational = view.findViewById(R.id.tab_international);
        tabPhoneTitle = view.findViewById(R.id.tab_phone_title);
        tabPhoneSub = view.findViewById(R.id.tab_phone_sub);
        tabCardTitle = view.findViewById(R.id.tab_card_title);
        tabCardSub = view.findViewById(R.id.tab_card_sub);
        tabIntlTitle = view.findViewById(R.id.tab_intl_title);
        tabIntlSub = view.findViewById(R.id.tab_intl_sub);

        // Sections
        sectionPhone = view.findViewById(R.id.section_phone);
        sectionCard = view.findViewById(R.id.section_card);
        sectionInternational = view.findViewById(R.id.section_international);

        // Phone
        tilRecipientPhone = view.findViewById(R.id.til_recipient_phone);
        etRecipientPhone = view.findViewById(R.id.et_recipient_phone);
        cardRecipientInfo = view.findViewById(R.id.card_recipient_info);
        cardRecipientNotFound = view.findViewById(R.id.card_recipient_not_found);
        cardLookupLoading = view.findViewById(R.id.card_lookup_loading);
        tvRecipientName = view.findViewById(R.id.tv_recipient_name);

        // Card
        tilRecipientCard = view.findViewById(R.id.til_recipient_card);
        etRecipientCard = view.findViewById(R.id.et_recipient_card);

        // International
        tilIntlCountry = view.findViewById(R.id.til_intl_country);
        etIntlCountry = view.findViewById(R.id.et_intl_country);
        tilIntlRecipient = view.findViewById(R.id.til_intl_recipient);
        etIntlRecipient = view.findViewById(R.id.et_intl_recipient);
        tilIntlIban = view.findViewById(R.id.til_intl_iban);
        etIntlIban = view.findViewById(R.id.et_intl_iban);
        tilIntlSwift = view.findViewById(R.id.til_intl_swift);
        etIntlSwift = view.findViewById(R.id.et_intl_swift);
        tilIntlPurpose = view.findViewById(R.id.til_intl_purpose);
        etIntlPurpose = view.findViewById(R.id.et_intl_purpose);
        chipUsd = view.findViewById(R.id.chip_usd);
        chipEur = view.findViewById(R.id.chip_eur);
        chipRub = view.findViewById(R.id.chip_rub);
        chipGbp = view.findViewById(R.id.chip_gbp);

        // Shared
        tilTransferAmount = view.findViewById(R.id.til_transfer_amount);
        etTransferAmount = view.findViewById(R.id.et_transfer_amount);
        btnConfirmTransfer = view.findViewById(R.id.btn_confirm_transfer);
        progressBarTransfer = view.findViewById(R.id.progressBarTransfer);

        // Tab clicks
        tabPhone.setOnClickListener(v -> switchMode(TransferMode.PHONE));
        tabCard.setOnClickListener(v -> switchMode(TransferMode.CARD));
        tabInternational.setOnClickListener(v -> switchMode(TransferMode.INTERNATIONAL));

        // Currency chips
        chipUsd.setOnClickListener(v -> selectCurrency("USD"));
        chipEur.setOnClickListener(v -> selectCurrency("EUR"));
        chipRub.setOnClickListener(v -> selectCurrency("RUB"));
        chipGbp.setOnClickListener(v -> selectCurrency("GBP"));

        // Phone lookup (debounced)
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
        clearAllErrors();

        // Reset all tabs to inactive
        tabPhone.setBackgroundResource(R.drawable.bg_tab_inactive);
        tabCard.setBackgroundResource(R.drawable.bg_tab_inactive);
        tabInternational.setBackgroundResource(R.drawable.bg_tab_inactive);
        setTabColors(tabPhoneTitle, tabPhoneSub, false);
        setTabColors(tabCardTitle, tabCardSub, false);
        setTabColors(tabIntlTitle, tabIntlSub, false);

        // Hide all sections
        sectionPhone.setVisibility(View.GONE);
        sectionCard.setVisibility(View.GONE);
        sectionInternational.setVisibility(View.GONE);

        // Activate selected tab
        switch (mode) {
            case PHONE:
                tabPhone.setBackgroundResource(R.drawable.bg_tab_active);
                setTabColors(tabPhoneTitle, tabPhoneSub, true);
                sectionPhone.setVisibility(View.VISIBLE);
                break;
            case CARD:
                tabCard.setBackgroundResource(R.drawable.bg_tab_active);
                setTabColors(tabCardTitle, tabCardSub, true);
                sectionCard.setVisibility(View.VISIBLE);
                break;
            case INTERNATIONAL:
                tabInternational.setBackgroundResource(R.drawable.bg_tab_active);
                setTabColors(tabIntlTitle, tabIntlSub, true);
                sectionInternational.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setTabColors(TextView title, TextView sub, boolean active) {
        title.setTextColor(active ? 0xFFFFFFFF : 0xFF6200EE);
        sub.setTextColor(active ? 0xCCFFFFFF : 0xFF757575);
    }

    // ── Currency selection ────────────────────────────────────────────────────

    private void selectCurrency(String currency) {
        selectedCurrency = currency;
        chipUsd.setBackgroundResource("USD".equals(currency) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipEur.setBackgroundResource("EUR".equals(currency) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipRub.setBackgroundResource("RUB".equals(currency) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipGbp.setBackgroundResource("GBP".equals(currency) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipUsd.setTextColor("USD".equals(currency) ? 0xFFFFFFFF : 0xFF6200EE);
        chipEur.setTextColor("EUR".equals(currency) ? 0xFFFFFFFF : 0xFF6200EE);
        chipRub.setTextColor("RUB".equals(currency) ? 0xFFFFFFFF : 0xFF6200EE);
        chipGbp.setTextColor("GBP".equals(currency) ? 0xFFFFFFFF : 0xFF6200EE);
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
        String amountStr = etTransferAmount.getText() != null
                ? etTransferAmount.getText().toString().trim() : "";

        BigDecimal amount = null;
        boolean amountInvalid = false;

        if (amountStr.isEmpty()) {
            tilTransferAmount.setError("Введите сумму перевода");
            etTransferAmount.requestFocus();
            amountInvalid = true;
        } else {
            try {
                amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
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

        switch (currentMode) {
            case PHONE: transferByPhone(amount, amountInvalid); break;
            case CARD: transferByCard(amount, amountInvalid); break;
            case INTERNATIONAL: transferInternational(amount, amountInvalid); break;
        }
    }

    private void transferByPhone(BigDecimal amount, boolean amountInvalid) {
        tilRecipientPhone.setError(null);
        String phone = text(etRecipientPhone);
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
        if (apiService == null) { Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show(); showLoading(false); return; }

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
        String card = text(etRecipientCard).replaceAll("\\s", "");
        boolean invalid = amountInvalid;

        if (card.isEmpty()) {
            tilRecipientCard.setError("Введите номер карты");
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

    private void transferInternational(BigDecimal amount, boolean amountInvalid) {
        clearIntlErrors();
        String country = text(etIntlCountry);
        String recipient = text(etIntlRecipient);
        String iban = text(etIntlIban).replaceAll("\\s", "");
        String swift = text(etIntlSwift).trim();
        boolean invalid = amountInvalid;

        if (country.isEmpty()) {
            tilIntlCountry.setError("Укажите страну назначения");
            etIntlCountry.requestFocus();
            invalid = true;
        }
        if (recipient.isEmpty()) {
            tilIntlRecipient.setError("Введите ФИО получателя");
            if (!invalid) etIntlRecipient.requestFocus();
            invalid = true;
        } else if (!recipient.matches("[A-Za-z\\s-]+")) {
            tilIntlRecipient.setError("Только латинские буквы");
            if (!invalid) etIntlRecipient.requestFocus();
            invalid = true;
        }
        if (iban.isEmpty()) {
            tilIntlIban.setError("Введите IBAN или номер счёта");
            if (!invalid) etIntlIban.requestFocus();
            invalid = true;
        }
        if (swift.isEmpty()) {
            tilIntlSwift.setError("Введите SWIFT/BIC код банка");
            if (!invalid) etIntlSwift.requestFocus();
            invalid = true;
        } else if (swift.length() < 8 || swift.length() > 11) {
            tilIntlSwift.setError("SWIFT/BIC: 8 или 11 символов");
            if (!invalid) etIntlSwift.requestFocus();
            invalid = true;
        }

        if (invalid || amount == null) return;

        // Show confirmation summary
        String summary = String.format(
                "Международный перевод:\n%s %s\nПолучатель: %s\nСчёт: %s\nБанк: %s\nСтрана: %s\n\n— Функция в разработке",
                amount.toPlainString(), selectedCurrency,
                recipient, iban.length() > 8 ? iban.substring(0, 4) + "..." + iban.substring(iban.length() - 4) : iban,
                swift, country
        );
        Toast.makeText(getContext(), summary, Toast.LENGTH_LONG).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String maskCard(String card) {
        if (card.length() != 16) return card;
        return card.substring(0, 4) + " **** **** " + card.substring(12);
    }

    private void showLoading(boolean loading) {
        if (progressBarTransfer != null) progressBarTransfer.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnConfirmTransfer != null) btnConfirmTransfer.setEnabled(!loading);
    }

    private void clearAllErrors() {
        tilRecipientPhone.setError(null);
        tilRecipientCard.setError(null);
        tilTransferAmount.setError(null);
        clearIntlErrors();
    }

    private void clearIntlErrors() {
        tilIntlCountry.setError(null);
        tilIntlRecipient.setError(null);
        tilIntlIban.setError(null);
        tilIntlSwift.setError(null);
        tilIntlPurpose.setError(null);
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
