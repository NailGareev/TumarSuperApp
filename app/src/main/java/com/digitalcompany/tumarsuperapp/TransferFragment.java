package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.TransferRequest;
import com.digitalcompany.tumarsuperapp.network.models.TransferResponse;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal; // Используем BigDecimal
import java.math.RoundingMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferFragment extends Fragment {

    private static final String TAG = "TransferFragment";

    private TextInputLayout tilRecipientPhone, tilTransferAmount;
    private TextInputEditText etRecipientPhone, etTransferAmount;
    private Button btnConfirmTransfer;
    private ProgressBar progressBarTransfer;
    private ApiService apiService;

    public TransferFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        } else {
            Log.e(TAG, "Activity is null in onCreate");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilRecipientPhone = view.findViewById(R.id.til_recipient_phone);
        etRecipientPhone = view.findViewById(R.id.et_recipient_phone);
        tilTransferAmount = view.findViewById(R.id.til_transfer_amount);
        etTransferAmount = view.findViewById(R.id.et_transfer_amount);
        btnConfirmTransfer = view.findViewById(R.id.btn_confirm_transfer);
        progressBarTransfer = view.findViewById(R.id.progressBarTransfer);

        btnConfirmTransfer.setOnClickListener(v -> attemptTransfer());
    }

    private void attemptTransfer() {
        // Сброс ошибок
        tilRecipientPhone.setError(null);
        tilTransferAmount.setError(null);

        String phoneInput = etRecipientPhone.getText().toString().trim();
        String amountInput = etTransferAmount.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // --- Валидация Номера Телефона ---
        String recipientPhone = null;
        // Проверяем, начинается ли с +7 и длина 12
        if (phoneInput.isEmpty()) {
            tilRecipientPhone.setError("Введите номер телефона получателя");
            focusView = etRecipientPhone;
            cancel = true;
        } else if (!phoneInput.startsWith("+7") || phoneInput.length() != 12) {
            tilRecipientPhone.setError("Формат номера: +7XXXXXXXXXX (12 символов)");
            focusView = etRecipientPhone;
            cancel = true;
        } else if (!phoneInput.substring(1).matches("\\d+")) {
            tilRecipientPhone.setError("Номер должен содержать только цифры после +");
            focusView = etRecipientPhone;
            cancel = true;
        } else {
            recipientPhone = phoneInput; // Номер валиден
        }

        // --- Валидация Суммы ---
        BigDecimal amount = null;
        if (amountInput.isEmpty()) {
            tilTransferAmount.setError("Введите сумму перевода");
            if (!cancel) focusView = etTransferAmount; // Устанавливаем фокус, если это первая ошибка
            cancel = true;
        } else {
            try {
                // Используем BigDecimal для точности
                amount = new BigDecimal(amountInput).setScale(2, RoundingMode.HALF_UP); // Округляем до 2 знаков
                if (amount.compareTo(BigDecimal.ZERO) <= 0) { // Проверяем, что сумма > 0
                    tilTransferAmount.setError("Сумма должна быть больше нуля");
                    if (!cancel) focusView = etTransferAmount;
                    cancel = true;
                    amount = null; // Сбрасываем сумму
                }
            } catch (NumberFormatException e) {
                tilTransferAmount.setError("Введите корректное число");
                if (!cancel) focusView = etTransferAmount;
                cancel = true;
            }
        }

        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
            return; // Прерываем выполнение, если есть ошибки валидации
        }

        // --- Валидация пройдена, выполняем запрос ---
        Log.d(TAG, "Validation passed. Attempting transfer to " + recipientPhone + " amount " + amount);
        showLoading(true);

        TransferRequest request = new TransferRequest(recipientPhone, amount);

        if (apiService == null) {
            Log.e(TAG, "ApiService is null. Cannot perform transfer.");
            Toast.makeText(getContext(), "Ошибка сети (сервис недоступен)", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        apiService.transferFunds(request).enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return; // Проверка фрагмента

                if (response.isSuccessful() && response.body() != null) {
                    TransferResponse transferResponse = response.body();
                    Log.d(TAG, "Transfer response: " + transferResponse);
                    if (transferResponse.isSuccess()) {
                        // --- Успешный перевод ---
                        Toast.makeText(getContext(), "Перевод выполнен успешно!", Toast.LENGTH_LONG).show();
                        // Очищаем поля
                        etRecipientPhone.setText("");
                        etTransferAmount.setText("");
                        // Можно перейти назад или обновить баланс на главном экране
                        // requireActivity().getSupportFragmentManager().popBackStack(); // Вернуться назад
                    } else {
                        // --- Ошибка со стороны бэкенда (недостаточно средств, пользователь не найден и т.д.) ---
                        String message = transferResponse.getMessage() != null ? transferResponse.getMessage() : "Не удалось выполнить перевод";
                        Log.w(TAG, "Transfer failed (API success=false): " + message);
                        Toast.makeText(getContext(), "Ошибка: " + message, Toast.LENGTH_LONG).show();
                        // Можно подсветить конкретное поле, если понятно из сообщения
                        if (message.toLowerCase().contains("recipient") || message.toLowerCase().contains("получател")) {
                            tilRecipientPhone.setError(message);
                            tilRecipientPhone.requestFocus();
                        } else if (message.toLowerCase().contains("funds") || message.toLowerCase().contains("средств")) {
                            tilTransferAmount.setError(message); // Ошибка может быть не в сумме, а в балансе, но ставим сюда
                            tilTransferAmount.requestFocus();
                        }
                    }
                } else {
                    // --- Ошибка сервера (не 2xx) ---
                    String errorMsg = "Ошибка сервера (" + response.code() + ")";
                    Log.e(TAG, "Server error during transfer: " + response.code());
                    // Попытка прочитать тело ошибки
                    if (response.errorBody() != null) {
                        try { errorMsg += ": " + response.errorBody().string(); } catch (Exception e) { /* ignore */ }
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TransferResponse> call, Throwable t) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return; // Проверка фрагмента
                Log.e(TAG, "Network error during transfer", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Метод для показа/скрытия ProgressBar
    private void showLoading(boolean isLoading) {
        if (progressBarTransfer != null) {
            progressBarTransfer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Блокируем кнопку во время загрузки
        if (btnConfirmTransfer != null) {
            btnConfirmTransfer.setEnabled(!isLoading);
        }
    }
}