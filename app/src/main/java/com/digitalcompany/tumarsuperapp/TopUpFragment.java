package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.digitalcompany.tumarsuperapp.network.models.CardResponse;
import com.digitalcompany.tumarsuperapp.network.models.TopUpRequest;
import com.digitalcompany.tumarsuperapp.network.models.TopUpResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopUpFragment extends Fragment {

    private enum Method { CARD, BANK_TRANSFER }
    private Method selectedMethod = Method.CARD;

    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private TextView tvCurrentBalance;
    private ProgressBar progressBar;
    private MaterialButton btnTopUp;

    // Card input section
    private LinearLayout sectionCardInput;
    private TextInputLayout tilCardNumber, tilCardExpiry, tilCardCvv;
    private TextInputEditText etCardNumber, etCardExpiry, etCardCvv;

    // Bank transfer section
    private LinearLayout sectionBankTransfer;
    private ProgressBar progressCardLoad;
    private CardView cardDisplayContainer;
    private LinearLayout layoutNoCard;
    private TextView tvVirtualCardNumber, tvVirtualCardExpiry, tvVirtualCardCvv;
    private MaterialButton btnIssueCard;

    // Method selector cards
    private CardView methodCard, methodBank;
    private TextView tvMethodCardTitle, tvMethodCardSub, tvMethodBankTitle, tvMethodBankSub;
    private View ctaBar;

    // New radio / icon views
    private View viewRadioCard, viewRadioBank, viewIconCard, viewIconBank;

    private ApiService apiService;

    public TopUpFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null)
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilAmount = view.findViewById(R.id.til_topup_amount);
        etAmount = view.findViewById(R.id.et_topup_amount);
        tvCurrentBalance = view.findViewById(R.id.tv_current_balance);
        progressBar = view.findViewById(R.id.progress_topup);
        btnTopUp = view.findViewById(R.id.btn_topup);

        methodCard = view.findViewById(R.id.method_card);
        methodBank = view.findViewById(R.id.method_bank);
        tvMethodCardTitle = view.findViewById(R.id.tv_method_card_title);
        tvMethodCardSub   = view.findViewById(R.id.tv_method_card_sub);
        tvMethodBankTitle = view.findViewById(R.id.tv_method_bank_title);
        tvMethodBankSub   = view.findViewById(R.id.tv_method_bank_sub);

        sectionCardInput = view.findViewById(R.id.section_card_input);
        tilCardNumber = view.findViewById(R.id.til_card_number);
        etCardNumber = view.findViewById(R.id.et_card_number);
        tilCardExpiry = view.findViewById(R.id.til_card_expiry);
        etCardExpiry = view.findViewById(R.id.et_card_expiry);
        tilCardCvv = view.findViewById(R.id.til_card_cvv);
        etCardCvv = view.findViewById(R.id.et_card_cvv);

        viewRadioCard = view.findViewById(R.id.view_radio_card);
        viewRadioBank = view.findViewById(R.id.view_radio_bank);
        ctaBar = view.findViewById(R.id.cta_bar);
        viewIconCard = view.findViewById(R.id.view_method_icon_card);
        viewIconBank = view.findViewById(R.id.view_method_icon_bank);

        View btnBack = view.findViewById(R.id.btn_back_topup);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }

        sectionBankTransfer = view.findViewById(R.id.section_bank_transfer);
        progressCardLoad = view.findViewById(R.id.progress_card_load);
        cardDisplayContainer = view.findViewById(R.id.card_display_container);
        layoutNoCard = view.findViewById(R.id.layout_no_card);
        tvVirtualCardNumber = view.findViewById(R.id.tv_virtual_card_number);
        tvVirtualCardExpiry = view.findViewById(R.id.tv_virtual_card_expiry);
        tvVirtualCardCvv    = view.findViewById(R.id.tv_virtual_card_cvv);
        btnIssueCard = view.findViewById(R.id.btn_issue_card);

        view.findViewById(R.id.chip_topup_500).setOnClickListener(v -> etAmount.setText("500"));
        view.findViewById(R.id.chip_topup_1000).setOnClickListener(v -> etAmount.setText("1000"));
        view.findViewById(R.id.chip_topup_5000).setOnClickListener(v -> etAmount.setText("5000"));
        view.findViewById(R.id.chip_topup_10000).setOnClickListener(v -> etAmount.setText("10000"));

        methodCard.setOnClickListener(v -> selectMethod(Method.CARD));
        methodBank.setOnClickListener(v -> selectMethod(Method.BANK_TRANSFER));

        btnIssueCard.setOnClickListener(v -> issueCard());
        btnTopUp.setOnClickListener(v -> attemptTopUp());

        etCardNumber.addTextChangedListener(new CardNumberFormatter());
        etCardExpiry.addTextChangedListener(new ExpiryFormatter());

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateTopUpButtonText();
            }
        });
        updateTopUpButtonText();

        loadCurrentBalance();
    }

    private void updateTopUpButtonText() {
        String raw = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            btnTopUp.setText("Пополнить");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(raw);
            java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("ru"));
            fmt.setGroupingUsed(true);
            fmt.setMaximumFractionDigits(0);
            btnTopUp.setText("Пополнить на ₸ " + fmt.format(amount.longValue()));
        } catch (NumberFormatException e) {
            btnTopUp.setText("Пополнить");
        }
    }

    private void selectMethod(Method method) {
        selectedMethod = method;
        if (method == Method.CARD) {
            methodCard.setCardBackgroundColor(0xFF1E0A36);
            methodBank.setCardBackgroundColor(0xFFFFFFFF);
            if (tvMethodCardTitle != null) {
                tvMethodCardTitle.setTextColor(0xFFFFFFFF);
                tvMethodCardSub.setTextColor(0x80FFFFFF);
            }
            if (tvMethodBankTitle != null) {
                tvMethodBankTitle.setTextColor(0xFF212121);
                tvMethodBankSub.setTextColor(0xFF757575);
            }
            if (viewRadioCard != null) viewRadioCard.setBackgroundResource(R.drawable.bg_topup_radio_selected);
            if (viewRadioBank != null) viewRadioBank.setBackgroundResource(R.drawable.bg_topup_radio_normal);
            if (viewIconCard != null) viewIconCard.setBackgroundResource(R.drawable.bg_topup_method_icon_selected);
            if (viewIconBank != null) viewIconBank.setBackgroundResource(R.drawable.bg_topup_method_icon);
            sectionCardInput.setVisibility(View.VISIBLE);
            sectionBankTransfer.setVisibility(View.GONE);
            if (ctaBar != null) ctaBar.setVisibility(View.VISIBLE);
        } else {
            methodBank.setCardBackgroundColor(0xFF1E0A36);
            methodCard.setCardBackgroundColor(0xFFFFFFFF);
            if (tvMethodBankTitle != null) {
                tvMethodBankTitle.setTextColor(0xFFFFFFFF);
                tvMethodBankSub.setTextColor(0x80FFFFFF);
            }
            if (tvMethodCardTitle != null) {
                tvMethodCardTitle.setTextColor(0xFF212121);
                tvMethodCardSub.setTextColor(0xFF757575);
            }
            if (viewRadioBank != null) viewRadioBank.setBackgroundResource(R.drawable.bg_topup_radio_selected);
            if (viewRadioCard != null) viewRadioCard.setBackgroundResource(R.drawable.bg_topup_radio_normal);
            if (viewIconBank != null) viewIconBank.setBackgroundResource(R.drawable.bg_topup_method_icon_selected);
            if (viewIconCard != null) viewIconCard.setBackgroundResource(R.drawable.bg_topup_method_icon);
            sectionCardInput.setVisibility(View.GONE);
            sectionBankTransfer.setVisibility(View.VISIBLE);
            if (ctaBar != null) ctaBar.setVisibility(View.GONE);
            loadUserCard();
        }
    }

    private int resolveCardBg() {
        return getResources().getColor(R.color.card_bg, null);
    }

    private void loadCurrentBalance() {
        if (apiService == null) return;
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BigDecimal bal = response.body().getBalance() != null
                            ? response.body().getBalance() : BigDecimal.ZERO;
                    tvCurrentBalance.setText(formatAmount(bal));
                }
            }
            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {}
        });
    }

    private void loadUserCard() {
        if (apiService == null) return;
        progressCardLoad.setVisibility(View.VISIBLE);
        cardDisplayContainer.setVisibility(View.GONE);
        layoutNoCard.setVisibility(View.GONE);

        apiService.getCard().enqueue(new Callback<CardResponse>() {
            @Override
            public void onResponse(Call<CardResponse> call, Response<CardResponse> response) {
                if (!isAdded() || getContext() == null) return;
                progressCardLoad.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CardResponse.CardData card = response.body().getCard();
                    if (card != null) {
                        showVirtualCard(card);
                    } else {
                        layoutNoCard.setVisibility(View.VISIBLE);
                    }
                } else {
                    layoutNoCard.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<CardResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                progressCardLoad.setVisibility(View.GONE);
                layoutNoCard.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showVirtualCard(CardResponse.CardData card) {
        tvVirtualCardNumber.setText(card.getFormattedNumber());
        tvVirtualCardExpiry.setText(card.getExpiry());
        if (card.getCvv() != null) tvVirtualCardCvv.setText(card.getCvv());
        cardDisplayContainer.setVisibility(View.VISIBLE);
        layoutNoCard.setVisibility(View.GONE);
    }

    private static final String CARD_PREFS = "CardDataPrefs";

    private void issueCard() {
        if (apiService == null) return;
        btnIssueCard.setEnabled(false);
        btnIssueCard.setText("Выпускаем...");

        apiService.issueCard().enqueue(new Callback<CardResponse>() {
            @Override
            public void onResponse(Call<CardResponse> call, Response<CardResponse> response) {
                if (!isAdded() || getContext() == null) return;
                btnIssueCard.setEnabled(true);
                btnIssueCard.setText("Выпустить карту");
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CardResponse.CardData card = response.body().getCard();
                    if (card != null) {
                        layoutNoCard.setVisibility(View.GONE);
                        showVirtualCard(card);
                        saveCardToSharedPrefs(card);
                        Toast.makeText(getContext(), "✅ Карта выпущена!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Ошибка сервера";
                    Toast.makeText(getContext(), "Ошибка: " + msg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<CardResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                btnIssueCard.setEnabled(true);
                btnIssueCard.setText("Выпустить карту");
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveCardToSharedPrefs(CardResponse.CardData card) {
        if (getContext() == null || card.getCardNumber() == null) return;
        android.content.SharedPreferences.Editor ed = getContext()
                .getSharedPreferences(CARD_PREFS, android.content.Context.MODE_PRIVATE).edit();
        ed.putBoolean("card_exists", true);
        ed.putString("card_number", card.getCardNumber());
        ed.putString("card_expiry", card.getExpiry());
        ed.putString("card_cvv", card.getCvv() != null ? card.getCvv() : "");
        ed.putBoolean("card_blocked", false);
        ed.putString("card_custom_name", "Tumar карта");
        ed.apply();
    }

    private void attemptTopUp() {
        tilAmount.setError(null);
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

        if (amountStr.isEmpty()) {
            tilAmount.setError("Введите сумму пополнения");
            etAmount.requestFocus();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ONE) < 0) {
                tilAmount.setError("Минимальная сумма: 1 ₸");
                etAmount.requestFocus();
                return;
            }
            if (amount.compareTo(new BigDecimal("5000000")) > 0) {
                tilAmount.setError("Максимальная сумма: 5 000 000 ₸");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            tilAmount.setError("Введите корректное число");
            etAmount.requestFocus();
            return;
        }

        if (selectedMethod == Method.CARD) {
            if (!validateCardInputs()) return;
        } else {
            if (cardDisplayContainer.getVisibility() != View.VISIBLE) {
                Toast.makeText(getContext(), "Сначала выпустите карту Tumar Bank", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        setLoading(true);
        apiService.topUp(new TopUpRequest(amount)).enqueue(new Callback<TopUpResponse>() {
            @Override
            public void onResponse(Call<TopUpResponse> call, Response<TopUpResponse> response) {
                setLoading(false);
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BigDecimal newBal = response.body().getNewBalance();
                    String newBalStr = newBal != null ? formatAmount(newBal) : "—";

                    String amountStr = etAmount.getText() != null
                            ? etAmount.getText().toString().trim() : "0";
                    String cardLast4 = "";
                    if (selectedMethod == Method.CARD && etCardNumber != null
                            && etCardNumber.getText() != null) {
                        String digits = etCardNumber.getText().toString().replaceAll("\\s", "");
                        if (digits.length() >= 4) cardLast4 = digits.substring(digits.length() - 4);
                    }
                    String methodName = selectedMethod == Method.CARD
                            ? "Банковская карта" : "Банковский перевод";

                    // Format amount with spaces as thousands separator
                    String formattedAmt;
                    try {
                        BigDecimal amt = new BigDecimal(amountStr);
                        java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("ru"));
                        fmt.setGroupingUsed(true);
                        fmt.setMaximumFractionDigits(0);
                        formattedAmt = fmt.format(amt.longValue());
                    } catch (Exception ex) {
                        formattedAmt = amountStr;
                    }

                    TopUpSuccessFragment successFragment = TopUpSuccessFragment.newInstance(
                            formattedAmt, methodName, cardLast4, newBalStr);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, successFragment)
                            .addToBackStack("topup_success")
                            .commit();
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Ошибка пополнения";
                    Toast.makeText(getContext(), "Ошибка: " + msg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<TopUpResponse> call, Throwable t) {
                setLoading(false);
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateCardInputs() {
        tilCardNumber.setError(null);
        tilCardExpiry.setError(null);
        tilCardCvv.setError(null);

        String rawNumber = etCardNumber.getText() != null ? etCardNumber.getText().toString() : "";
        String digits = rawNumber.replaceAll("\\s", "");
        if (digits.length() != 16) {
            tilCardNumber.setError("Введите 16-значный номер карты");
            etCardNumber.requestFocus();
            return false;
        }

        String expiry = etCardExpiry.getText() != null ? etCardExpiry.getText().toString().trim() : "";
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            tilCardExpiry.setError("Формат: ММ/ГГ");
            etCardExpiry.requestFocus();
            return false;
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3, 5));
        Calendar cal = Calendar.getInstance();
        int curMonth = cal.get(Calendar.MONTH) + 1;
        int curYear = cal.get(Calendar.YEAR);
        if (month < 1 || month > 12 || year < curYear || (year == curYear && month < curMonth)) {
            tilCardExpiry.setError("Карта истекла или дата некорректна");
            etCardExpiry.requestFocus();
            return false;
        }

        String cvv = etCardCvv.getText() != null ? etCardCvv.getText().toString().trim() : "";
        if (cvv.length() < 3 || cvv.length() > 4) {
            tilCardCvv.setError("CVV: 3–4 цифры");
            etCardCvv.requestFocus();
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnTopUp.setEnabled(!loading);
    }

    private String formatAmount(BigDecimal amount) {
        try {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("kk", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);
            fmt.setMinimumFractionDigits(0);
            return fmt.format(amount);
        } catch (Exception e) {
            return amount.toPlainString() + " ₸";
        }
    }

    // Formats card number input as XXXX XXXX XXXX XXXX
    private static class CardNumberFormatter implements TextWatcher {
        private boolean editing = false;
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if (editing) return;
            editing = true;
            String digits = s.toString().replaceAll("[^\\d]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(' ');
                formatted.append(digits.charAt(i));
            }
            s.replace(0, s.length(), formatted);
            editing = false;
        }
    }

    // Formats expiry as MM/YY
    private static class ExpiryFormatter implements TextWatcher {
        private boolean editing = false;
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if (editing) return;
            editing = true;
            String digits = s.toString().replaceAll("[^\\d]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 2) formatted.append('/');
                formatted.append(digits.charAt(i));
            }
            s.replace(0, s.length(), formatted);
            editing = false;
        }
    }
}
