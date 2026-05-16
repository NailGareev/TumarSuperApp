package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.PayRequest;
import com.digitalcompany.tumarsuperapp.network.models.PayResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_NAME     = "name";
    private static final String ARG_ICON     = "icon";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_LABEL    = "label";
    private static final String ARG_HINT     = "hint";

    public interface OnPaymentSuccessListener {
        void onPaymentSuccess(BigDecimal newBalance);
    }

    private OnPaymentSuccessListener successListener;

    public static PaymentBottomSheet newInstance(String name, String icon, String category,
                                                  String accountLabel, String accountHint) {
        PaymentBottomSheet sheet = new PaymentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_ICON, icon);
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_LABEL, accountLabel);
        args.putString(ARG_HINT, accountHint);
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

        String name     = args.getString(ARG_NAME, "");
        String icon     = args.getString(ARG_ICON, "💳");
        String category = args.getString(ARG_CATEGORY, "");
        String label    = args.getString(ARG_LABEL, "Номер счёта");
        String hint     = args.getString(ARG_HINT, "");

        view.<TextView>findViewById(R.id.tv_sheet_icon).setText(icon);
        view.<TextView>findViewById(R.id.tv_sheet_service_name).setText(name);
        view.<TextView>findViewById(R.id.tv_sheet_category).setText(category);

        TextInputLayout tilAccount = view.findViewById(R.id.til_sheet_account);
        tilAccount.setHint(label + (hint.isEmpty() ? "" : "  (" + hint + ")"));
        TextInputEditText etAccount = view.findViewById(R.id.et_sheet_account);

        TextInputLayout tilAmount = view.findViewById(R.id.til_sheet_amount);
        TextInputEditText etAmount = view.findViewById(R.id.et_sheet_amount);

        view.findViewById(R.id.chip_sheet_500).setOnClickListener(v -> etAmount.setText("500"));
        view.findViewById(R.id.chip_sheet_1000).setOnClickListener(v -> etAmount.setText("1000"));
        view.findViewById(R.id.chip_sheet_3000).setOnClickListener(v -> etAmount.setText("3000"));
        view.findViewById(R.id.chip_sheet_5000).setOnClickListener(v -> etAmount.setText("5000"));

        MaterialButton btnPay = view.findViewById(R.id.btn_sheet_pay);
        ProgressBar progress = view.findViewById(R.id.progress_sheet);

        btnPay.setOnClickListener(v -> {
            tilAccount.setError(null);
            tilAmount.setError(null);

            String account   = etAccount.getText() != null ? etAccount.getText().toString().trim() : "";
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

            if (account.isEmpty()) { tilAccount.setError("Введите " + label.toLowerCase()); etAccount.requestFocus(); return; }
            if (amountStr.isEmpty()) { tilAmount.setError("Введите сумму"); etAmount.requestFocus(); return; }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("500000")) > 0) {
                    tilAmount.setError("От 1 до 500 000 ₸");
                    etAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                tilAmount.setError("Некорректная сумма");
                etAmount.requestFocus();
                return;
            }

            btnPay.setEnabled(false);
            progress.setVisibility(View.VISIBLE);

            ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
            api.pay(new PayRequest(name, account, amount)).enqueue(new Callback<PayResponse>() {
                @Override
                public void onResponse(Call<PayResponse> call, Response<PayResponse> response) {
                    if (!isAdded() || getContext() == null) return;
                    progress.setVisibility(View.GONE);
                    btnPay.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        BigDecimal newBal = response.body().getNewBalance();
                        Toast.makeText(getContext(),
                                "✅ Оплата прошла успешно!", Toast.LENGTH_LONG).show();
                        if (successListener != null) successListener.onPaymentSuccess(newBal);
                        dismiss();
                    } else {
                        String msg = response.body() != null && response.body().getMessage() != null
                                ? response.body().getMessage() : "Ошибка сервера";
                        Toast.makeText(getContext(), "Ошибка: " + msg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<PayResponse> call, Throwable t) {
                    if (!isAdded() || getContext() == null) return;
                    progress.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
