package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalcompany.tumarsuperapp.adapter.TransactionAdapter; // Импорт адаптера
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private ApiService apiService;
    private ProgressBar progressBarHistory;
    private TextView tvHistoryEmpty;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
            // Создаем адаптер здесь, передавая контекст
            transactionAdapter = new TransactionAdapter(requireActivity());
        } else {
            Log.e(TAG, "Activity is null in onCreate");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTransactions = view.findViewById(R.id.rv_transactions);
        progressBarHistory = view.findViewById(R.id.progressBarHistory);
        tvHistoryEmpty = view.findViewById(R.id.tv_history_empty);

        setupRecyclerView();

        if (apiService != null && transactionAdapter != null) {
            loadTransactionHistory();
        } else {
            Log.e(TAG, "ApiService or Adapter is null in onViewCreated");
            if(getContext() != null) {
                Toast.makeText(getContext(), "Ошибка загрузки истории", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        // Устанавливаем адаптер в RecyclerView
        rvTransactions.setAdapter(transactionAdapter);
        // Можно добавить разделители ItemDecoration, если нужно
        // rvTransactions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void loadTransactionHistory() {
        showLoading(true);
        tvHistoryEmpty.setVisibility(View.GONE); // Скрываем текст "пусто" при загрузке

        Log.d(TAG, "Запрос истории транзакций...");
        apiService.getTransactionHistory().enqueue(new Callback<TransactionHistoryResponse>() {
            @Override
            public void onResponse(Call<TransactionHistoryResponse> call, Response<TransactionHistoryResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return; // Проверка

                if (response.isSuccessful() && response.body() != null) {
                    TransactionHistoryResponse historyResponse = response.body();
                    Log.d(TAG, "История успешно загружена. Success: " + historyResponse.isSuccess());
                    if (historyResponse.isSuccess() && historyResponse.getTransactions() != null) {
                        List<Transaction> transactions = historyResponse.getTransactions();
                        Log.d(TAG, "Получено транзакций: " + transactions.size());
                        if (transactions.isEmpty()) {
                            // Показываем сообщение, что история пуста
                            rvTransactions.setVisibility(View.GONE);
                            tvHistoryEmpty.setVisibility(View.VISIBLE);
                        } else {
                            // Обновляем адаптер
                            rvTransactions.setVisibility(View.VISIBLE);
                            tvHistoryEmpty.setVisibility(View.GONE);
                            transactionAdapter.submitList(transactions);
                        }
                    } else {
                        Log.w(TAG, "API вернул success=false при загрузке истории.");
                        Toast.makeText(getContext(), "Не удалось загрузить историю", Toast.LENGTH_SHORT).show();
                        tvHistoryEmpty.setText("Ошибка загрузки истории"); // Меняем текст
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                        rvTransactions.setVisibility(View.GONE);
                        // Возможно, токен невалиден?
                        // LoginActivity.logout(requireActivity());
                    }
                } else {
                    Log.e(TAG, "Ошибка ответа сервера при загрузке истории: " + response.code());
                    Toast.makeText(getContext(), "Ошибка сервера (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    tvHistoryEmpty.setText("Ошибка сервера");
                    tvHistoryEmpty.setVisibility(View.VISIBLE);
                    rvTransactions.setVisibility(View.GONE);
                    if (response.code() == 401 || response.code() == 403) {
                        // LoginActivity.logout(requireActivity());
                    }
                }
            }

            @Override
            public void onFailure(Call<TransactionHistoryResponse> call, Throwable t) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return; // Проверка
                Log.e(TAG, "Ошибка сети при загрузке истории", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                tvHistoryEmpty.setText("Ошибка сети");
                tvHistoryEmpty.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBarHistory != null) {
            progressBarHistory.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Скрываем RecyclerView во время загрузки, чтобы избежать мерцания
        if (rvTransactions != null) {
            rvTransactions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }
}