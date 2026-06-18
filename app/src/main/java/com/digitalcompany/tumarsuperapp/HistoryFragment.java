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

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private DonutChartView miniDonutChart;
    private LinearLayout llDonutLegend;

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
    private int selectedPeriodDays  = 30;
    private String selectedCategory = CAT_ALL;
    private String selectedType     = "ALL"; // ALL / EXPENSE / INCOME
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
        cardSummary    = view.findViewById(R.id.card_summary);
        miniDonutChart = view.findViewById(R.id.mini_donut_chart);
        llDonutLegend  = view.findViewById(R.id.ll_donut_legend);

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
        miniDonutChart  = view.findViewById(R.id.mini_donut_chart);
        llDonutLegend   = view.findViewById(R.id.ll_donut_legend);

        view.findViewById(R.id.btn_history_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.btn_history_filter).setOnClickListener(v -> openFilterSheet());

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

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setSystemNavVisible(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).restoreNavBars();
    }

    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(historyAdapter);
        rvTransactions.setNestedScrollingEnabled(false);
        historyAdapter.setOnTransactionClickListener(tx -> {
            if (!isAdded()) return;
            TransactionDetailFragment detail =
                    TransactionDetailFragment.newInstance(tx, currentUserId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });
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
                    int marketCount = 0;
                    for (Transaction t : allTransactions) {
                        String type = t.getTransactionType();
                        String desc = t.getDescription();
                        if ("MARKET_REFUND".equals(type)
                                || ("PAYMENT".equals(type) && desc != null && desc.contains("Tumar Market"))) {
                            marketCount++;
                        }
                    }
                    Log.d(TAG, "Загружено транзакций: " + allTransactions.size() + ", из них Market: " + marketCount);
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

        Calendar cutoff = Calendar.getInstance(TimeZone.getDefault());
        cutoff.add(Calendar.DAY_OF_YEAR, -selectedPeriodDays);
        Date cutoffDate = cutoff.getTime();

        List<Transaction> periodFiltered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            // Include if within period OR if timestamp is missing (don't silently discard)
            if (t.getTimestamp() == null || !t.getTimestamp().before(cutoffDate)) {
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

        updateSummary(periodFiltered);
        updateDonutChart(periodFiltered);
    }

    private boolean matchesCategory(Transaction t) {
        String txType = t.getTransactionType();
        boolean isIncoming = t.getRecipientId() == currentUserId;
        boolean isCancelled = "cancelled".equals(t.getPaymentStatus());
        boolean isMarketPayment = "PAYMENT".equals(txType)
                && t.getDescription() != null
                && t.getDescription().contains("Tumar Market");

        // Type filter (ALL / EXPENSE / INCOME)
        if ("EXPENSE".equals(selectedType)) {
            if ("TOPUP".equals(txType)) return false;
            if ("TRANSFER".equals(txType) && isIncoming) return false;
            if ("MARKET_REFUND".equals(txType)) return false;
            if (isCancelled) return false; // cancelled payments are not expenses
        } else if ("INCOME".equals(selectedType)) {
            if ("PAYMENT".equals(txType) && !isCancelled) return false;
            if ("TRANSFER".equals(txType) && !isIncoming) return false;
        }

        // Category filter
        if (CAT_ALL.equals(selectedCategory)) return true;
        switch (selectedCategory) {
            case CAT_MARKET:
                if ("MARKET_REFUND".equals(txType)) return true;
                return isMarketPayment; // includes cancelled market payments
            case CAT_TRANSFER:
                return "TRANSFER".equals(txType);
            case CAT_PAYMENT:
                return "PAYMENT".equals(txType) && !isMarketPayment;
            case CAT_INCOME:
                if ("TOPUP".equals(txType)) return true;
                if ("MARKET_REFUND".equals(txType)) return true;
                if ("TRANSFER".equals(txType)) return isIncoming;
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

        TimeZone tz = TimeZone.getDefault();
        Calendar today     = Calendar.getInstance(tz);
        Calendar yesterday = Calendar.getInstance(tz);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat sameYearFmt  = new SimpleDateFormat("d MMMM", new Locale("ru"));
        SimpleDateFormat otherYearFmt = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
        sameYearFmt.setTimeZone(tz);
        otherYearFmt.setTimeZone(tz);

        List<Object> result = new ArrayList<>();
        String lastDateLabel  = null;
        BigDecimal dayExpense = BigDecimal.ZERO;
        BigDecimal dayIncome  = BigDecimal.ZERO;
        int groupStartIndex   = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Transaction t = sorted.get(i);

            String label;
            if (t.getTimestamp() == null) {
                label = "Без даты";
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
                if (lastDateLabel != null) {
                    result.set(groupStartIndex, new HistoryAdapter.DateGroup(lastDateLabel, dayExpense, dayIncome));
                }
                groupStartIndex = result.size();
                result.add(new HistoryAdapter.DateGroup(label, BigDecimal.ZERO, BigDecimal.ZERO));
                dayExpense = BigDecimal.ZERO;
                dayIncome  = BigDecimal.ZERO;
                lastDateLabel = label;
            }

            if (t.getAmount() != null) {
                String type = t.getTransactionType();
                boolean cancelled = "cancelled".equals(t.getPaymentStatus());
                BigDecimal amt = t.getAmount().abs();
                if ("PAYMENT".equals(type) && !cancelled) {
                    dayExpense = dayExpense.add(amt);
                } else if ("MARKET_REFUND".equals(type)) {
                    dayIncome = dayIncome.add(amt);
                } else if ("TOPUP".equals(type)) {
                    dayIncome = dayIncome.add(amt);
                } else if ("TRANSFER".equals(type)) {
                    if (t.getSenderId() == currentUserId) dayExpense = dayExpense.add(amt);
                    else if (t.getRecipientId() == currentUserId) dayIncome = dayIncome.add(amt);
                }
            }

            result.add(t);
        }

        if (lastDateLabel != null) {
            result.set(groupStartIndex, new HistoryAdapter.DateGroup(lastDateLabel, dayExpense, dayIncome));
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
            boolean isCancelled = "cancelled".equals(t.getPaymentStatus());
            BigDecimal amt = t.getAmount().abs();

            if ("TOPUP".equals(type) || "MARKET_REFUND".equals(type)) {
                totalIncome = totalIncome.add(amt);
            } else if ("PAYMENT".equals(type) && !isCancelled) {
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

    private void updateDonutChart(List<Transaction> periodFiltered) {
        if (miniDonutChart == null) return;

        BigDecimal payTotal    = BigDecimal.ZERO;
        BigDecimal marketTotal = BigDecimal.ZERO;
        BigDecimal outTotal    = BigDecimal.ZERO;
        BigDecimal topupTotal  = BigDecimal.ZERO;
        BigDecimal inTotal     = BigDecimal.ZERO;

        for (Transaction t : periodFiltered) {
            if (t.getAmount() == null) continue;
            String type = t.getTransactionType();
            BigDecimal amt = t.getAmount().abs();
            boolean cancelled = "cancelled".equals(t.getPaymentStatus());
            boolean isMarket = "PAYMENT".equals(type)
                    && t.getDescription() != null
                    && t.getDescription().contains("Tumar Market");

            if ("TOPUP".equals(type)) {
                topupTotal = topupTotal.add(amt);
            } else if ("MARKET_REFUND".equals(type)) {
                marketTotal = marketTotal.add(amt);
            } else if ("PAYMENT".equals(type) && !cancelled) {
                if (isMarket) marketTotal = marketTotal.add(amt);
                else          payTotal    = payTotal.add(amt);
            } else if ("TRANSFER".equals(type)) {
                if (t.getRecipientId() == currentUserId)   inTotal  = inTotal.add(amt);
                else if (t.getSenderId() == currentUserId) outTotal = outTotal.add(amt);
            }
        }

        BigDecimal grandTotal = payTotal.add(marketTotal).add(outTotal).add(topupTotal).add(inTotal);
        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            miniDonutChart.setData(new float[0], new int[0], "", "0 ₸");
            if (llDonutLegend != null) llDonutLegend.removeAllViews();
            return;
        }

        String[]    names   = {"Платежи", "Market", "Переводы", "Пополнения", "Вх. переводы"};
        BigDecimal[] amounts = {payTotal, marketTotal, outTotal, topupTotal, inTotal};
        int[]        colors  = {0xFFD97222, 0xFF6B21A8, 0xFF1A4A8A, 0xFF1A8A4A, 0xFF9C27B0};

        float[] pcts = new float[names.length];
        for (int i = 0; i < amounts.length; i++) {
            pcts[i] = amounts[i].divide(grandTotal, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).floatValue();
        }

        BigDecimal totalExp = payTotal.add(marketTotal).add(outTotal);
        NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
        fmt.setGroupingUsed(true);
        fmt.setMaximumFractionDigits(0);
        miniDonutChart.setData(pcts, colors, "Расходы", fmt.format(totalExp.longValue()) + " ₸");

        if (llDonutLegend != null) {
            llDonutLegend.removeAllViews();
            for (int i = 0; i < names.length; i++) {
                if (amounts[i].compareTo(BigDecimal.ZERO) > 0) {
                    llDonutLegend.addView(buildMiniLegendRow(names[i], Math.round(pcts[i]), colors[i]));
                }
            }
        }
    }

    private android.view.View buildMiniLegendRow(String name, int pct, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 3, 0, 3);

        android.widget.ImageView dot = new android.widget.ImageView(requireContext());
        float d = getResources().getDisplayMetrics().density;
        int sz = (int)(7 * d);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(sz, sz);
        dotLp.setMarginEnd((int)(5 * d));
        dot.setLayoutParams(dotLp);
        GradientDrawable circ = new GradientDrawable();
        circ.setShape(GradientDrawable.OVAL);
        circ.setColor(color);
        dot.setBackground(circ);

        TextView tvName = new TextView(requireContext());
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);
        tvName.setText(name);
        tvName.setTextSize(10f);
        tvName.setTextColor(0xFF555555);

        TextView tvPct = new TextView(requireContext());
        tvPct.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvPct.setText(pct + "%");
        tvPct.setTextSize(10f);
        tvPct.setTextColor(0xFF111111);
        tvPct.getPaint().setFakeBoldText(true);

        row.addView(dot);
        row.addView(tvName);
        row.addView(tvPct);
        return row;
    }

    private void openFilterSheet() {
        HistoryFilterBottomSheet sheet = HistoryFilterBottomSheet.newInstance(
                selectedPeriodDays, selectedCategory, selectedType);
        sheet.setOnFilterApplyListener((days, cat, type) -> {
            selectedPeriodDays = days;
            selectedCategory   = cat;
            selectedType       = type;
            updatePeriodChipVisuals();
            updateCategoryChipVisuals();
            applyFilters();
        });
        sheet.show(getChildFragmentManager(), "HistoryFilter");
    }

    private void showCategoryBreakdown() {
        Calendar cutoff = Calendar.getInstance(TimeZone.getDefault());
        cutoff.add(Calendar.DAY_OF_YEAR, -selectedPeriodDays);
        Date cutoffDate = cutoff.getTime();

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getTimestamp() != null && !t.getTimestamp().before(cutoffDate)) {
                filtered.add(t);
            }
        }

        BigDecimal topupTotal  = BigDecimal.ZERO; int topupCount  = 0;
        BigDecimal payTotal    = BigDecimal.ZERO; int payCount    = 0;
        BigDecimal marketTotal = BigDecimal.ZERO; int marketCount = 0;
        BigDecimal outTotal    = BigDecimal.ZERO; int outCount    = 0;
        BigDecimal inTotal     = BigDecimal.ZERO; int inCount     = 0;

        for (Transaction t : filtered) {
            if (t.getAmount() == null) continue;
            BigDecimal amt = t.getAmount().abs();
            String type = t.getTransactionType();
            boolean cancelled = "cancelled".equals(t.getPaymentStatus());
            boolean isMarket = "PAYMENT".equals(type)
                    && t.getDescription() != null
                    && t.getDescription().contains("Tumar Market");

            if ("TOPUP".equals(type)) {
                topupTotal = topupTotal.add(amt); topupCount++;
            } else if ("MARKET_REFUND".equals(type)) {
                marketTotal = marketTotal.add(amt); marketCount++;
            } else if ("PAYMENT".equals(type) && !cancelled) {
                if (isMarket) { marketTotal = marketTotal.add(amt); marketCount++; }
                else          { payTotal    = payTotal.add(amt);    payCount++;    }
            } else if ("TRANSFER".equals(type)) {
                if (t.getRecipientId() == currentUserId) { inTotal  = inTotal.add(amt);  inCount++;  }
                else if (t.getSenderId() == currentUserId) { outTotal = outTotal.add(amt); outCount++; }
            }
        }

        String periodLabel = periodDaysToLabel(selectedPeriodDays);
        CategoryHistoryFragment cat = CategoryHistoryFragment.newInstance(
                topupTotal, topupCount, payTotal, payCount,
                marketTotal, marketCount, outTotal, outCount, inTotal, inCount, periodLabel);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, cat)
                .addToBackStack(null)
                .commit();
    }

    private String periodDaysToLabel(int days) {
        if (days == 7)   return "1 неделя";
        if (days == 30)  return "1 месяц";
        if (days == 90)  return "3 месяца";
        if (days == 180) return "6 месяцев";
        return "1 год";
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
