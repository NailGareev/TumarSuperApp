package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    // Category filter constants
    private static final String CAT_ALL      = "ALL";
    private static final String CAT_MARKET   = "MARKET";
    private static final String CAT_TRANSFER = "TRANSFER";
    private static final String CAT_PAYMENT  = "PAYMENT";
    private static final String CAT_INCOME   = "INCOME";

    private RecyclerView rvTransactions;
    private HistoryAdapter historyAdapter;
    private ApiService apiService;
    private View progressBarHistory;
    private View tvHistoryEmpty;
    private TextView tvSummaryIncome;
    private TextView tvSummaryExpense;
    private LinearLayout cardSummary;

    // Period chips
    private TextView chipPeriod1w;
    private TextView chipPeriod1m;
    private TextView chipPeriod3m;
    private TextView chipPeriod6m;
    private TextView chipPeriod1y;

    // Category chips
    private TextView chipCatAll;
    private TextView chipCatMarket;
    private TextView chipCatTransfer;
    private TextView chipCatPayment;
    private TextView chipCatIncome;

    private List<Transaction> allTransactions = new ArrayList<>();
    private int selectedPeriodDays  = 30; // default 1 month
    private String selectedCategory = CAT_ALL;
    private int currentUserId = -1;

    public HistoryFragment() {}

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

        rvTransactions     = view.findViewById(R.id.rv_transactions);
        progressBarHistory = view.findViewById(R.id.progressBarHistory);
        tvHistoryEmpty     = view.findViewById(R.id.tv_history_empty);
        tvSummaryIncome    = view.findViewById(R.id.tv_summary_income);
        tvSummaryExpense   = view.findViewById(R.id.tv_summary_expense);
        cardSummary        = view.findViewById(R.id.card_summary);

        chipPeriod1w = view.findViewById(R.id.chip_period_1w);
        chipPeriod1m = view.findViewById(R.id.chip_period_1m);
        chipPeriod3m = view.findViewById(R.id.chip_period_3m);
        chipPeriod6m = view.findViewById(R.id.chip_period_6m);
        chipPeriod1y = view.findViewById(R.id.chip_period_1y);

        chipCatAll      = view.findViewById(R.id.chip_cat_all);
        chipCatMarket   = view.findViewById(R.id.chip_cat_market);
        chipCatTransfer = view.findViewById(R.id.chip_cat_transfer);
        chipCatPayment  = view.findViewById(R.id.chip_cat_payment);
        chipCatIncome   = view.findViewById(R.id.chip_cat_income);

        setupRecyclerView();
        setupPeriodChips();
        setupCategoryChips();
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
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void setupPeriodChips() {
        chipPeriod1w.setOnClickListener(v -> selectPeriod(7));
        chipPeriod1m.setOnClickListener(v -> selectPeriod(30));
        chipPeriod3m.setOnClickListener(v -> selectPeriod(90));
        chipPeriod6m.setOnClickListener(v -> selectPeriod(180));
        chipPeriod1y.setOnClickListener(v -> selectPeriod(365));
    }

    private void setupCategoryChips() {
        chipCatAll.setOnClickListener(v      -> selectCategory(CAT_ALL));
        chipCatMarket.setOnClickListener(v   -> selectCategory(CAT_MARKET));
        chipCatTransfer.setOnClickListener(v -> selectCategory(CAT_TRANSFER));
        chipCatPayment.setOnClickListener(v  -> selectCategory(CAT_PAYMENT));
        chipCatIncome.setOnClickListener(v   -> selectCategory(CAT_INCOME));
    }

    private void selectPeriod(int days) {
        selectedPeriodDays = days;
        updatePeriodChipVisuals();
        applyFilters();
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateCategoryChipVisuals();
        applyFilters();
    }

    private void updatePeriodChipVisuals() {
        setChipActive(chipPeriod1w, selectedPeriodDays == 7);
        setChipActive(chipPeriod1m, selectedPeriodDays == 30);
        setChipActive(chipPeriod3m, selectedPeriodDays == 90);
        setChipActive(chipPeriod6m, selectedPeriodDays == 180);
        setChipActive(chipPeriod1y, selectedPeriodDays == 365);
    }

    private void updateCategoryChipVisuals() {
        setChipActive(chipCatAll,      CAT_ALL.equals(selectedCategory));
        setChipActive(chipCatMarket,   CAT_MARKET.equals(selectedCategory));
        setChipActive(chipCatTransfer, CAT_TRANSFER.equals(selectedCategory));
        setChipActive(chipCatPayment,  CAT_PAYMENT.equals(selectedCategory));
        setChipActive(chipCatIncome,   CAT_INCOME.equals(selectedCategory));
    }

    private void setChipActive(TextView chip, boolean active) {
        if (chip == null) return;
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_chip_purple_active);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_purple);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }
    }

    private void setupSummaryCard() {
        if (cardSummary != null) {
            cardSummary.setOnClickListener(v -> showCategoryBreakdown());
        }
    }

    private void loadTransactionHistory() {
        showLoading(true);
        tvHistoryEmpty.setVisibility(View.GONE);

        apiService.getTransactionHistory().enqueue(new Callback<TransactionHistoryResponse>() {
            @Override
            public void onResponse(Call<TransactionHistoryResponse> call,
                                   Response<TransactionHistoryResponse> response) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getTransactions() != null) {
                    allTransactions = new ArrayList<>(response.body().getTransactions());
                    applyFilters();
                } else {
                    Log.w(TAG, "Не удалось загрузить историю: " + response.code());
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<TransactionHistoryResponse> call, Throwable t) {
                showLoading(false);
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Ошибка сети", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private void applyFilters() {
        if (!isAdded() || getContext() == null) return;

        Calendar cutoff = Calendar.getInstance(TimeZone.getTimeZone("Asia/Almaty"));
        cutoff.add(Calendar.DAY_OF_YEAR, -selectedPeriodDays);
        Date cutoffDate = cutoff.getTime();

        List<Transaction> periodFiltered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getTimestamp() != null && !t.getTimestamp().before(cutoffDate)) {
                periodFiltered.add(t);
            }
        }

        // Apply category filter
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : periodFiltered) {
            if (matchesCategory(t)) {
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

        updateSummary(periodFiltered); // summary always uses period filter (not category)
    }

    private boolean matchesCategory(Transaction t) {
        if (CAT_ALL.equals(selectedCategory)) return true;
        String type = t.getTransactionType();
        switch (selectedCategory) {
            case CAT_MARKET:
                return "MARKET_REFUND".equals(type);
            case CAT_TRANSFER:
                return "TRANSFER".equals(type);
            case CAT_PAYMENT:
                return "PAYMENT".equals(type);
            case CAT_INCOME:
                if ("TOPUP".equals(type)) return true;
                if ("TRANSFER".equals(type)) return t.getRecipientId() == currentUserId;
                return false;
            default:
                return true;
        }
    }

    private List<Object> groupByDate(List<Transaction> filtered) {
        List<Transaction> sorted = new ArrayList<>(filtered);
        Collections.sort(sorted, (a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        TimeZone tz = TimeZone.getTimeZone("Asia/Almaty");
        Calendar today     = Calendar.getInstance(tz);
        Calendar yesterday = Calendar.getInstance(tz);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat sameYearFmt  = new SimpleDateFormat("d MMMM", new Locale("ru"));
        SimpleDateFormat otherYearFmt = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
        sameYearFmt.setTimeZone(tz);
        otherYearFmt.setTimeZone(tz);

        List<Object> result = new ArrayList<>();
        String lastDateLabel = null;
        BigDecimal dayTotal  = BigDecimal.ZERO;
        int groupStartIndex  = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Transaction t = sorted.get(i);

            String label;
            if (t.getTimestamp() == null) {
                label = "";
            } else {
                Calendar txCal = Calendar.getInstance(tz);
                txCal.setTime(t.getTimestamp());
                if (isSameDay(txCal, today)) {
                    label = "Сегодня";
                } else if (isSameDay(txCal, yesterday)) {
                    label = "Вчера";
                } else if (txCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    label = sameYearFmt.format(t.getTimestamp());
                } else {
                    label = otherYearFmt.format(t.getTimestamp());
                }
            }

            if (!label.equals(lastDateLabel)) {
                // Finalize previous group header with its total
                if (lastDateLabel != null) {
                    result.set(groupStartIndex, new HistoryAdapter.DateGroup(lastDateLabel, dayTotal));
                }
                // Start new group
                groupStartIndex = result.size();
                result.add(new HistoryAdapter.DateGroup(label, BigDecimal.ZERO)); // placeholder
                dayTotal = BigDecimal.ZERO;
                lastDateLabel = label;
            }

            // Accumulate daily expense total
            if (t.getAmount() != null) {
                String type = t.getTransactionType();
                BigDecimal amt = t.getAmount().abs();
                if ("PAYMENT".equals(type) || "MARKET_REFUND".equals(type)) {
                    dayTotal = dayTotal.add(amt);
                } else if ("TRANSFER".equals(type) && t.getSenderId() == currentUserId) {
                    dayTotal = dayTotal.add(amt);
                }
            }

            result.add(t);
        }

        // Finalize last group
        if (lastDateLabel != null) {
            result.set(groupStartIndex, new HistoryAdapter.DateGroup(lastDateLabel, dayTotal));
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
                if (t.getRecipientId() == currentUserId) totalIncome  = totalIncome.add(amt);
                else if (t.getSenderId() == currentUserId) totalExpense = totalExpense.add(amt);
            }
        }

        NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
        fmt.setGroupingUsed(true);
        fmt.setMaximumFractionDigits(0);

        tvSummaryIncome.setText("+ " + fmt.format(totalIncome.longValue()) + " ₸");
        tvSummaryExpense.setText("- " + fmt.format(totalExpense.longValue()) + " ₸");
    }

    private void showCategoryBreakdown() {
        Calendar cutoff = Calendar.getInstance(TimeZone.getTimeZone("Asia/Almaty"));
        cutoff.add(Calendar.DAY_OF_YEAR, -selectedPeriodDays);
        Date cutoffDate = cutoff.getTime();

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getTimestamp() != null && !t.getTimestamp().before(cutoffDate)) {
                filtered.add(t);
            }
        }

        BigDecimal topupTotal = BigDecimal.ZERO; int topupCount = 0;
        BigDecimal payTotal   = BigDecimal.ZERO; int payCount   = 0;
        BigDecimal outTotal   = BigDecimal.ZERO; int outCount   = 0;
        BigDecimal inTotal    = BigDecimal.ZERO; int inCount    = 0;

        for (Transaction t : filtered) {
            if (t.getAmount() == null) continue;
            BigDecimal amt = t.getAmount().abs();
            String type = t.getTransactionType();
            if ("TOPUP".equals(type)) {
                topupTotal = topupTotal.add(amt); topupCount++;
            } else if ("PAYMENT".equals(type) || "MARKET_REFUND".equals(type)) {
                payTotal = payTotal.add(amt); payCount++;
            } else if ("TRANSFER".equals(type)) {
                if (t.getRecipientId() == currentUserId) { inTotal = inTotal.add(amt); inCount++; }
                else if (t.getSenderId() == currentUserId) { outTotal = outTotal.add(amt); outCount++; }
            }
        }

        CategoryBreakdownBottomSheet sheet = CategoryBreakdownBottomSheet.newInstance(
                topupTotal, topupCount, payTotal, payCount,
                outTotal, outCount, inTotal, inCount);
        sheet.show(getChildFragmentManager(), "CategoryBreakdown");
    }

    private void showEmpty() {
        rvTransactions.setVisibility(View.GONE);
        tvHistoryEmpty.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarHistory != null)
            progressBarHistory.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (rvTransactions != null)
            rvTransactions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}
