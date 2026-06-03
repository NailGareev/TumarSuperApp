package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.adapter.HistoryAdapter;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";

    private RecyclerView rvTransactions;
    private HistoryAdapter historyAdapter;
    private ApiService apiService;
    private ProgressBar progressBarHistory;
    private View tvHistoryEmpty;
    private TextView tvSummaryIncome;
    private TextView tvSummaryExpense;
    private CardView cardSummary;

    private TextView chipPeriod1m;
    private TextView chipPeriod3m;
    private TextView chipPeriod6m;
    private TextView chipPeriod1y;

    private List<Transaction> allTransactions = new ArrayList<>();
    private int selectedPeriodMonths = 1;
    private int currentUserId = -1;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
            SharedPreferences prefs = getActivity().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
            currentUserId = prefs.getInt(KEY_USER_ID, -1);
            historyAdapter = new HistoryAdapter(currentUserId);
        } else {
            Log.e(TAG, "Activity is null in onCreate");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTransactions    = view.findViewById(R.id.rv_transactions);
        progressBarHistory = view.findViewById(R.id.progressBarHistory);
        tvHistoryEmpty    = view.findViewById(R.id.tv_history_empty);
        tvSummaryIncome   = view.findViewById(R.id.tv_summary_income);
        tvSummaryExpense  = view.findViewById(R.id.tv_summary_expense);
        cardSummary       = view.findViewById(R.id.card_summary);

        chipPeriod1m = view.findViewById(R.id.chip_period_1m);
        chipPeriod3m = view.findViewById(R.id.chip_period_3m);
        chipPeriod6m = view.findViewById(R.id.chip_period_6m);
        chipPeriod1y = view.findViewById(R.id.chip_period_1y);

        setupRecyclerView();
        setupChips();
        setupSummaryCard();

        if (apiService != null && historyAdapter != null) {
            loadTransactionHistory();
        } else {
            Log.e(TAG, "ApiService or Adapter is null in onViewCreated");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Ошибка загрузки истории", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(historyAdapter);
    }

    private void setupChips() {
        chipPeriod1m.setOnClickListener(v -> selectPeriod(1));
        chipPeriod3m.setOnClickListener(v -> selectPeriod(3));
        chipPeriod6m.setOnClickListener(v -> selectPeriod(6));
        chipPeriod1y.setOnClickListener(v -> selectPeriod(12));
    }

    private void selectPeriod(int months) {
        selectedPeriodMonths = months;
        updateChipVisuals();
        applyFilter();
    }

    private void updateChipVisuals() {
        // Reset all chips to inactive
        setChipActive(chipPeriod1m, selectedPeriodMonths == 1);
        setChipActive(chipPeriod3m, selectedPeriodMonths == 3);
        setChipActive(chipPeriod6m, selectedPeriodMonths == 6);
        setChipActive(chipPeriod1y, selectedPeriodMonths == 12);
    }

    private void setChipActive(TextView chip, boolean active) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_chip_purple_active);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_purple);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }
    }

    private void setupSummaryCard() {
        cardSummary.setOnClickListener(v -> showCategoryBreakdown());
    }

    private void loadTransactionHistory() {
        showLoading(true);
        tvHistoryEmpty.setVisibility(View.GONE);

        Log.d(TAG, "Запрос истории транзакций...");
        apiService.getTransactionHistory().enqueue(new Callback<TransactionHistoryResponse>() {
            @Override
            public void onResponse(Call<TransactionHistoryResponse> call,
                                   Response<TransactionHistoryResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    TransactionHistoryResponse historyResponse = response.body();
                    Log.d(TAG, "История успешно загружена. Success: " + historyResponse.isSuccess());
                    if (historyResponse.isSuccess() && historyResponse.getTransactions() != null) {
                        allTransactions = new ArrayList<>(historyResponse.getTransactions());
                        Log.d(TAG, "Получено транзакций: " + allTransactions.size());
                        applyFilter();
                    } else {
                        Log.w(TAG, "API вернул success=false при загрузке истории.");
                        Toast.makeText(getContext(), "Не удалось загрузить историю", Toast.LENGTH_SHORT).show();
                        showEmpty();
                    }
                } else {
                    Log.e(TAG, "Ошибка ответа сервера при загрузке истории: " + response.code());
                    Toast.makeText(getContext(), "Ошибка сервера (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<TransactionHistoryResponse> call, Throwable t) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Ошибка сети при загрузке истории", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private void applyFilter() {
        if (!isAdded() || getContext() == null) return;

        // Determine cutoff date: selectedPeriodMonths ago from now
        Calendar cutoff = Calendar.getInstance(TimeZone.getTimeZone("Asia/Almaty"));
        cutoff.add(Calendar.MONTH, -selectedPeriodMonths);
        Date cutoffDate = cutoff.getTime();

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getTimestamp() != null && !t.getTimestamp().before(cutoffDate)) {
                filtered.add(t);
            }
        }

        if (filtered.isEmpty()) {
            historyAdapter.setItems(new ArrayList<>());
            showEmpty();
        } else {
            rvTransactions.setVisibility(View.VISIBLE);
            tvHistoryEmpty.setVisibility(View.GONE);
            List<Object> grouped = groupByDate(filtered);
            historyAdapter.setItems(grouped);
        }

        updateSummary(filtered);
    }

    private List<Object> groupByDate(List<Transaction> filtered) {
        // Sort descending by timestamp
        List<Transaction> sorted = new ArrayList<>(filtered);
        Collections.sort(sorted, (a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        TimeZone tz = TimeZone.getTimeZone("Asia/Almaty");
        Calendar today = Calendar.getInstance(tz);
        Calendar yesterday = Calendar.getInstance(tz);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        List<Object> result = new ArrayList<>();
        String lastDateLabel = null;

        SimpleDateFormat sameYearFmt = new SimpleDateFormat("d MMMM", new Locale("ru"));
        sameYearFmt.setTimeZone(tz);
        SimpleDateFormat otherYearFmt = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
        otherYearFmt.setTimeZone(tz);

        for (Transaction t : sorted) {
            if (t.getTimestamp() == null) {
                if (lastDateLabel == null || !lastDateLabel.equals("")) {
                    lastDateLabel = "";
                    result.add("");
                }
                result.add(t);
                continue;
            }

            Calendar txCal = Calendar.getInstance(tz);
            txCal.setTime(t.getTimestamp());

            String label;
            if (isSameDay(txCal, today)) {
                label = "Сегодня";
            } else if (isSameDay(txCal, yesterday)) {
                label = "Вчера";
            } else if (txCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                label = sameYearFmt.format(t.getTimestamp());
            } else {
                label = otherYearFmt.format(t.getTimestamp());
            }

            if (!label.equals(lastDateLabel)) {
                lastDateLabel = label;
                result.add(label);
            }
            result.add(t);
        }

        return result;
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private void updateSummary(List<Transaction> filtered) {
        if (!isAdded() || getContext() == null) return;

        BigDecimal totalIncome  = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : filtered) {
            if (t.getAmount() == null) continue;
            String type = t.getTransactionType();
            BigDecimal amt = t.getAmount().abs();

            if ("TOPUP".equals(type)) {
                totalIncome = totalIncome.add(amt);
            } else if ("PAYMENT".equals(type) || "MARKET_REFUND".equals(type)) {
                totalExpense = totalExpense.add(amt);
            } else if ("TRANSFER".equals(type)) {
                boolean isIncoming = t.getRecipientId() == currentUserId;
                boolean isOutgoing = t.getSenderId() == currentUserId;
                if (isIncoming) {
                    totalIncome = totalIncome.add(amt);
                } else if (isOutgoing) {
                    totalExpense = totalExpense.add(amt);
                }
            }
        }

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
        fmt.setCurrency(Currency.getInstance("KZT"));
        fmt.setMaximumFractionDigits(0);
        fmt.setMinimumFractionDigits(0);

        tvSummaryIncome.setText("+" + fmt.format(totalIncome));
        tvSummaryExpense.setText("-" + fmt.format(totalExpense));
    }

    private void showCategoryBreakdown() {
        // Rebuild breakdown from currently filtered list
        Calendar cutoff = Calendar.getInstance(TimeZone.getTimeZone("Asia/Almaty"));
        cutoff.add(Calendar.MONTH, -selectedPeriodMonths);
        Date cutoffDate = cutoff.getTime();

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getTimestamp() != null && !t.getTimestamp().before(cutoffDate)) {
                filtered.add(t);
            }
        }

        BigDecimal topupTotal       = BigDecimal.ZERO;
        int        topupCount       = 0;
        BigDecimal paymentTotal     = BigDecimal.ZERO;
        int        paymentCount     = 0;
        BigDecimal transferOutTotal = BigDecimal.ZERO;
        int        transferOutCount = 0;
        BigDecimal transferInTotal  = BigDecimal.ZERO;
        int        transferInCount  = 0;

        for (Transaction t : filtered) {
            if (t.getAmount() == null) continue;
            String type = t.getTransactionType();
            BigDecimal amt = t.getAmount().abs();

            if ("TOPUP".equals(type)) {
                topupTotal = topupTotal.add(amt);
                topupCount++;
            } else if ("PAYMENT".equals(type) || "MARKET_REFUND".equals(type)) {
                paymentTotal = paymentTotal.add(amt);
                paymentCount++;
            } else if ("TRANSFER".equals(type)) {
                boolean isIncoming = t.getRecipientId() == currentUserId;
                boolean isOutgoing = t.getSenderId() == currentUserId;
                if (isIncoming) {
                    transferInTotal = transferInTotal.add(amt);
                    transferInCount++;
                } else if (isOutgoing) {
                    transferOutTotal = transferOutTotal.add(amt);
                    transferOutCount++;
                }
            }
        }

        CategoryBreakdownBottomSheet sheet = CategoryBreakdownBottomSheet.newInstance(
                topupTotal, topupCount,
                paymentTotal, paymentCount,
                transferOutTotal, transferOutCount,
                transferInTotal, transferInCount);
        sheet.show(getChildFragmentManager(), "CategoryBreakdown");
    }

    private void showEmpty() {
        rvTransactions.setVisibility(View.GONE);
        tvHistoryEmpty.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarHistory != null) {
            progressBarHistory.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (rvTransactions != null) {
            rvTransactions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }
}
