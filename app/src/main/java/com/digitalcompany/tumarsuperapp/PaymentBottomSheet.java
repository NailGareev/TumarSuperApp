package com.digitalcompany.tumarsuperapp;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.PayRequest;
import com.digitalcompany.tumarsuperapp.network.models.PayResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_NAME         = "name";
    private static final String ARG_ICON         = "icon";
    private static final String ARG_CATEGORY     = "category";
    private static final String ARG_LABEL        = "label";
    private static final String ARG_HINT         = "hint";
    private static final String ARG_ACCENT_COLOR = "accent_color";
    private static final String ARG_INITIAL_ACCOUNT = "initial_account";

    public interface OnPaymentSuccessListener {
        void onPaymentSuccess(BigDecimal newBalance);
    }

    private OnPaymentSuccessListener successListener;

    public static PaymentBottomSheet newInstance(String name, String icon, String category,
                                                  String accountLabel, String accountHint,
                                                  int accentColor) {
        return newInstance(name, icon, category, accountLabel, accountHint, accentColor, "");
    }

    public static PaymentBottomSheet newInstance(String name, String icon, String category,
                                                 String accountLabel, String accountHint,
                                                 int accentColor, String initialAccount) {
        PaymentBottomSheet sheet = new PaymentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_ICON, icon);
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_LABEL, accountLabel);
        args.putString(ARG_HINT, accountHint);
        args.putInt(ARG_ACCENT_COLOR, accentColor);
        args.putString(ARG_INITIAL_ACCOUNT, initialAccount);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnPaymentSuccessListener(OnPaymentSuccessListener listener) {
        this.successListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { dismiss(); return; }

        String name        = args.getString(ARG_NAME, "");
        String icon        = args.getString(ARG_ICON, "💳");
        String category    = args.getString(ARG_CATEGORY, "");
        String label       = args.getString(ARG_LABEL, "Номер счёта");
        String hint        = args.getString(ARG_HINT, "");
        int accentColor    = args.getInt(ARG_ACCENT_COLOR, 0xFF6200EE);
        String initialAccount = args.getString(ARG_INITIAL_ACCOUNT, "");

        int accentLight  = (accentColor & 0x00FFFFFF) | 0x1A000000;
        int accentBorder = (accentColor & 0x00FFFFFF) | 0x47000000;

        float density = getResources().getDisplayMetrics().density;

        // Service header
        view.<TextView>findViewById(R.id.tv_sheet_icon).setText(icon);
        view.<TextView>findViewById(R.id.tv_sheet_service_name).setText(name);
        view.<TextView>findViewById(R.id.tv_sheet_category).setText(category);

        // Color the icon box
        FrameLayout flIconBox = view.findViewById(R.id.fl_sheet_icon_box);
        if (flIconBox != null) {
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setColor(accentLight);
            iconBg.setStroke((int)(1.5f * density), accentBorder);
            iconBg.setCornerRadius(13 * density);
            flIconBox.setBackground(iconBg);
        }

        // Account field label
        TextView tvLabel = view.findViewById(R.id.tv_sheet_account_label);
        if (tvLabel != null) {
            tvLabel.setText(label + (hint.isEmpty() ? "" : "  (" + hint + ")"));
        }

        // Account and amount edit texts (visual ones in the new layout)
        TextInputEditText etAccount = view.findViewById(R.id.et_sheet_account);
        TextInputEditText etAmount  = view.findViewById(R.id.et_sheet_amount);

        boolean isPhoneField = label.toLowerCase(Locale.ROOT).contains("телефон");
        boolean isKyrgyzPhone = isPhoneField && (
                hint.startsWith("+996") ||
                name.contains(" KG") ||
                name.contains("O!") ||
                name.contains("MegaCom")
        );
        if (etAccount != null) {
            if (isPhoneField) {
                etAccount.setInputType(InputType.TYPE_CLASS_PHONE);
                etAccount.addTextChangedListener(isKyrgyzPhone
                        ? new KyrgyzPhoneFormatWatcher(etAccount)
                        : new PhoneFormatWatcher(etAccount));
            }
            if (initialAccount != null && !initialAccount.trim().isEmpty()) {
                etAccount.setText(initialAccount.trim());
                if (etAccount.getText() != null) {
                    etAccount.setSelection(etAccount.getText().length());
                }
            }
        }

        // Hidden TextInputLayouts (for error display compatibility)
        TextInputLayout tilAccount = view.findViewById(R.id.til_sheet_account);

        // Error text view for amount
        TextView tvAmountError = view.findViewById(R.id.tv_amount_error);

        // CTA button text
        TextView tvBtnPayText = view.findViewById(R.id.tv_btn_pay_text);

        // Update button text as amount changes
        if (etAmount != null && tvBtnPayText != null) {
            etAmount.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    String raw = s.toString().trim();
                    if (raw.isEmpty()) {
                        tvBtnPayText.setText("Оплатить ₸ 0");
                    } else {
                        try {
                            BigDecimal val = new BigDecimal(raw);
                            tvBtnPayText.setText("Оплатить ₸ " + formatAmount(val));
                        } catch (NumberFormatException e) {
                            tvBtnPayText.setText("Оплатить ₸ 0");
                        }
                    }
                }
            });
        }

        // Chip clicks with active/inactive state toggle
        int[] chipIds = {R.id.chip_sheet_500, R.id.chip_sheet_1000, R.id.chip_sheet_3000, R.id.chip_sheet_5000};
        String[] chipValues = {"500", "1000", "3000", "5000"};
        for (int i = 0; i < chipIds.length; i++) {
            final String chipVal = chipValues[i];
            view.findViewById(chipIds[i]).setOnClickListener(v -> {
                setAmount(etAmount, tvBtnPayText, chipVal);
                updateChipStates(view, chipIds, chipVal, chipValues);
            });
        }

        LinearLayout btnPay = view.findViewById(R.id.btn_sheet_pay);
        ProgressBar progress = view.findViewById(R.id.progress_sheet);

        btnPay.setOnClickListener(v -> {
            // Clear previous errors
            if (tvAmountError != null) tvAmountError.setVisibility(View.GONE);

            String accountInput = etAccount != null && etAccount.getText() != null ? etAccount.getText().toString().trim() : "";
            String account = accountInput;
            if (isPhoneField) {
                account = isKyrgyzPhone
                        ? KyrgyzPhoneFormatWatcher.raw(etAccount)
                        : PhoneFormatWatcher.raw(etAccount);
            }
            String amountStr = etAmount  != null && etAmount.getText()  != null ? etAmount.getText().toString().trim()  : "";

            if (account.isEmpty()) {
                if (tilAccount != null) tilAccount.setError("Введите " + label.toLowerCase());
                if (etAccount != null) etAccount.requestFocus();
                return;
            }
            if (tilAccount != null) tilAccount.setError(null);

            if (amountStr.isEmpty()) {
                if (tvAmountError != null) { tvAmountError.setText("Введите сумму"); tvAmountError.setVisibility(View.VISIBLE); }
                if (etAmount != null) etAmount.requestFocus();
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("500000")) > 0) {
                    if (tvAmountError != null) { tvAmountError.setText("От 1 до 500 000 ₸"); tvAmountError.setVisibility(View.VISIBLE); }
                    if (etAmount != null) etAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                if (tvAmountError != null) { tvAmountError.setText("Некорректная сумма"); tvAmountError.setVisibility(View.VISIBLE); }
                if (etAmount != null) etAmount.requestFocus();
                return;
            }

            btnPay.setEnabled(false);
            if (progress != null) progress.setVisibility(View.VISIBLE);

            final String finalAccount = account;
            final BigDecimal finalAmount = amount;
            final String formattedAmount = formatAmount(amount);

            ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
            api.pay(new PayRequest(name, account, amount)).enqueue(new Callback<PayResponse>() {
                @Override
                public void onResponse(Call<PayResponse> call, Response<PayResponse> response) {
                    if (!isAdded() || getContext() == null) return;
                    if (progress != null) progress.setVisibility(View.GONE);
                    btnPay.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        BigDecimal newBal = response.body().getNewBalance();
                        String newBalStr = newBal != null ? formatAmount(newBal) : "—";

                        if (successListener != null) successListener.onPaymentSuccess(newBal);

                        // Navigate to PaymentSuccessFragment
                        PaymentSuccessFragment pf = PaymentSuccessFragment.newInstance(
                                name, finalAccount, formattedAmount, category, newBalStr, accentColor);
                        dismiss();
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_up_enter, R.anim.fade_out,
                                        R.anim.fade_in, R.anim.fade_out)
                                .replace(R.id.fragment_container, pf)
                                .addToBackStack("pay_success")
                                .commit();
                    } else {
                        String msg = response.body() != null && response.body().getMessage() != null
                                ? response.body().getMessage() : "Ошибка сервера";
                        if (tvAmountError != null) { tvAmountError.setText("Ошибка: " + msg); tvAmountError.setVisibility(View.VISIBLE); }
                    }
                }

                @Override
                public void onFailure(Call<PayResponse> call, Throwable t) {
                    if (!isAdded() || getContext() == null) return;
                    if (progress != null) progress.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    if (tvAmountError != null) { tvAmountError.setText("Ошибка сети: " + t.getMessage()); tvAmountError.setVisibility(View.VISIBLE); }
                }
            });
        });
    }

    private void setAmount(TextInputEditText etAmount, TextView tvBtnPayText, String value) {
        if (etAmount != null) {
            etAmount.setText(value);
            etAmount.setSelection(value.length());
        }
        if (tvBtnPayText != null) {
            try {
                BigDecimal val = new BigDecimal(value);
                tvBtnPayText.setText("Оплатить ₸ " + formatAmount(val));
            } catch (NumberFormatException e) {
                tvBtnPayText.setText("Оплатить ₸ 0");
            }
        }
    }

    private void updateChipStates(View root, int[] chipIds, String selected, String[] chipValues) {
        for (int i = 0; i < chipIds.length; i++) {
            View chip = root.findViewById(chipIds[i]);
            if (chip instanceof TextView) {
                boolean isActive = chipValues[i].equals(selected);
                chip.setBackground(root.getContext().getDrawable(
                        isActive ? R.drawable.bg_pay_chip_country_active : R.drawable.bg_pay_chip_country));
                ((TextView) chip).setTextColor(isActive ? 0xFFFFFFFF : 0xFF6200EE);
            }
        }
    }

    private String formatAmount(BigDecimal amount) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setGroupingSeparator(' ');
        sym.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.##", sym);
        return df.format(amount);
    }

    private static class KyrgyzPhoneFormatWatcher implements TextWatcher {
        private final TextInputEditText editText;
        private boolean formatting;

        KyrgyzPhoneFormatWatcher(TextInputEditText editText) {
            this.editText = editText;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (formatting) return;
            formatting = true;

            String digits = s.toString().replaceAll("[^0-9]", "");
            if (digits.startsWith("996")) {
                digits = digits.substring(3);
            }
            if (digits.length() > 9) {
                digits = digits.substring(0, 9);
            }

            StringBuilder sb = new StringBuilder("+996");
            if (!digits.isEmpty()) {
                sb.append(" ").append(digits, 0, Math.min(3, digits.length()));
            }
            if (digits.length() > 3) {
                sb.append(" ").append(digits, 3, Math.min(9, digits.length()));
            }

            s.replace(0, s.length(), sb.toString());
            if (editText.getText() != null) {
                editText.setSelection(editText.getText().length());
            }

            formatting = false;
        }

        static String raw(TextInputEditText et) {
            String text = et != null && et.getText() != null ? et.getText().toString() : "";
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.startsWith("996")) {
                digits = digits.substring(3);
            }
            if (digits.length() > 9) {
                digits = digits.substring(0, 9);
            }
            return "+996" + digits;
        }
    }
}
