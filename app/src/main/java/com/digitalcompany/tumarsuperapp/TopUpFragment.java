package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.TopUpRequest;
import com.digitalcompany.tumarsuperapp.network.models.TopUpResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopUpFragment extends Fragment {

    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private TextView tvCurrentBalance;
    private ProgressBar progressBar;
    private MaterialButton btnTopUp;
    private ApiService apiService;

    public TopUpFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null)
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
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

        view.findViewById(R.id.chip_topup_500).setOnClickListener(v -> etAmount.setText("500"));
        view.findViewById(R.id.chip_topup_1000).setOnClickListener(v -> etAmount.setText("1000"));
        view.findViewById(R.id.chip_topup_5000).setOnClickListener(v -> etAmount.setText("5000"));
        view.findViewById(R.id.chip_topup_10000).setOnClickListener(v -> etAmount.setText("10000"));

        btnTopUp.setOnClickListener(v -> attemptTopUp());

        loadCurrentBalance();
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

        setLoading(true);
        apiService.topUp(new TopUpRequest(amount)).enqueue(new Callback<TopUpResponse>() {
            @Override
            public void onResponse(Call<TopUpResponse> call, Response<TopUpResponse> response) {
                setLoading(false);
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BigDecimal newBal = response.body().getNewBalance();
                    String newBalStr = newBal != null ? formatAmount(newBal) : "—";
                    tvCurrentBalance.setText(newBalStr);
                    etAmount.setText("");
                    Toast.makeText(getContext(),
                            "Баланс пополнен! Новый баланс: " + newBalStr,
                            Toast.LENGTH_LONG).show();
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
}
