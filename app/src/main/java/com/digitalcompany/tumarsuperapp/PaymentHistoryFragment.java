package com.digitalcompany.tumarsuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentHistoryFragment extends Fragment {

    private static final String TAG = "PaymentHistoryFragment";

    private static final String[] PERIOD_LABELS = {"Все", "Этот месяц", "3 месяца", "Год"};
    private static final String[] CAT_LABELS = {
            "Все категории", "📱 Связь", "💡 ЖКХ", "🌐 Интернет", "🎮 Игры", "📺 ТВ"
    };
    private static final String[] CAT_EMOJIS = {"", "📱", "💡", "🌐", "🎮", "📺"};

    private int selectedPeriod = 0;
    private int selectedCat = 0;
    private List<Transaction> allPayments = new ArrayList<>();

    private LinearLayout llPeriodChips;
    private LinearLayout llCatChips;
    private LinearLayout llHistoryContent;
    private ProgressBar progressBar;
    private LinearLayout llEmpty;
    private NestedScrollView scrollHistory;

    private TextView tvStatTotal, tvStatMonthly, tvStatCats;

    private final View[] periodChipViews = new View[PERIOD_LABELS.length];
    private final View[] catChipViews = new View[CAT_LABELS.length];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_pay_hist_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        tvStatTotal   = view.findViewById(R.id.tv_stat_total);
        tvStatMonthly = view.findViewById(R.id.tv_stat_monthly);
        tvStatCats    = view.findViewById(R.id.tv_stat_cats);

        llPeriodChips  = view.findViewById(R.id.ll_period_chips);
        llCatChips     = view.findViewById(R.id.ll_cat_chips);
        llHistoryContent = view.findViewById(R.id.ll_history_content);
        progressBar    = view.findViewById(R.id.progressBarPayHistory);
        llEmpty        = view.findViewById(R.id.ll_pay_hist_empty);
        scrollHistory  = view.findViewById(R.id.scroll_pay_history);

        buildPeriodChips();
        buildCatChips();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private void buildPeriodChips() {
        float dp = dp();
        for (int i = 0; i < PERIOD_LABELS.length; i++) {
            final int idx = i;
            TextView chip = new TextView(requireContext());
            chip.setText(PERIOD_LABELS[i]);
            chip.setTextSize(11);
            chip.setTypeface(null, Typeface.BOLD);
            int hp = (int)(12 * dp), vp = (int)(6 * dp);
            chip.setPadding(hp, vp, hp, vp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int)(8 * dp));
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> { selectedPeriod = idx; refreshChipStates(); applyFilters(); });
            periodChipViews[i] = chip;
            llPeriodChips.addView(chip);
        }
        refreshChipStates();
    }

    private void buildCatChips() {
        float dp = dp();
        for (int i = 0; i < CAT_LABELS.length; i++) {
            final int idx = i;
            TextView chip = new TextView(requireContext());
            chip.setText(CAT_LABELS[i]);
            chip.setTextSize(10);
            chip.setTypeface(null, Typeface.BOLD);
            int hp = (int)(10 * dp), vp = (int)(5 * dp);
            chip.setPadding(hp, vp, hp, vp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int)(6 * dp));
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> { selectedCat = idx; refreshChipStates(); applyFilters(); });
            catChipViews[i] = chip;
            llCatChips.addView(chip);
        }
        refreshChipStates();
    }

    private void refreshChipStates() {
        for (int i = 0; i < periodChipViews.length; i++) {
            if (periodChipViews[i] == null) continue;
            boolean active = (i == selectedPeriod);
            periodChipViews[i].setBackgroundResource(
                    active ? R.drawable.bg_chip_purple_active : R.drawable.bg_chip_purple);
            ((TextView) periodChipViews[i]).setTextColor(active ? Color.WHITE : 0xFF6B21A8);
        }
        for (int i = 0; i < catChipViews.length; i++) {
            if (catChipViews[i] == null) continue;
            boolean active = (i == selectedCat);
            catChipViews[i].setBackgroundResource(
                    active ? R.drawable.bg_pay_cat_chip_active : R.drawable.bg_pay_cat_chip_inactive);
            ((TextView) catChipViews[i]).setTextColor(active ? Color.WHITE : 0xFF777777);
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData() {
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.getTransactionHistory().enqueue(new Callback<TransactionHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<TransactionHistoryResponse> call,
                                   @NonNull Response<TransactionHistoryResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getTransactions() != null) {
                    allPayments.clear();
                    for (Transaction t : response.body().getTransactions()) {
                        if ("PAYMENT".equals(t.getTransactionType())) {
                            allPayments.add(t);
                        }
                    }
                }
                updateStats();
                applyFilters();
            }
            @Override
            public void onFailure(@NonNull Call<TransactionHistoryResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.w(TAG, "Load failed: " + t.getMessage());
                progressBar.setVisibility(View.GONE);
                applyFilters();
            }
        });
    }

    private void updateStats() {
        if (!isAdded()) return;
        int total = allPayments.size();
        tvStatTotal.setText(String.valueOf(total));

        // Monthly sum: transactions in current month
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        BigDecimal monthlySum = BigDecimal.ZERO;
        for (Transaction t : allPayments) {
            if (t.getTimestamp() != null && t.getTimestamp().after(monthStart.getTime())
                    && t.getAmount() != null) {
                monthlySum = monthlySum.add(t.getAmount());
            }
        }
        try {
            long sum = monthlySum.longValue();
            NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
            tvStatMonthly.setText("₸ " + fmt.format(sum));
        } catch (Exception e) {
            tvStatMonthly.setText("₸ 0");
        }

        // Unique categories
        Set<String> cats = new HashSet<>();
        for (Transaction t : allPayments) {
            cats.add(getEmoji(t.getDescription()));
        }
        tvStatCats.setText(String.valueOf(cats.size()));
    }

    // ── Filtering & rendering ─────────────────────────────────────────────────

    private void applyFilters() {
        if (!isAdded() || llHistoryContent == null) return;

        Date cutoff = getPeriodCutoff();
        String catEmoji = CAT_EMOJIS[selectedCat];

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allPayments) {
            if (cutoff != null && t.getTimestamp() != null && t.getTimestamp().before(cutoff)) continue;
            if (!catEmoji.isEmpty() && !getEmoji(t.getDescription()).equals(catEmoji)) continue;
            filtered.add(t);
        }

        if (filtered.isEmpty()) {
            scrollHistory.setVisibility(View.GONE);
            llEmpty.setVisibility(View.VISIBLE);
        } else {
            llEmpty.setVisibility(View.GONE);
            scrollHistory.setVisibility(View.VISIBLE);
            buildHistoryList(filtered);
        }
    }

    private void buildHistoryList(List<Transaction> transactions) {
        llHistoryContent.removeAllViews();
        float dp = dp();

        // Group by date
        SimpleDateFormat dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            String key = t.getTimestamp() != null ? dayKey.format(t.getTimestamp()) : "unknown";
            if (!groups.containsKey(key)) groups.put(key, new ArrayList<>());
            groups.get(key).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            List<Transaction> group = entry.getValue();
            Date date = group.get(0).getTimestamp();

            // Date header row
            LinearLayout headerRow = new LinearLayout(requireContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams hrLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            hrLp.bottomMargin = (int)(5 * dp);
            hrLp.setMarginStart((int)(2 * dp));
            headerRow.setLayoutParams(hrLp);

            TextView tvDate = new TextView(requireContext());
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvDate.setText(formatDateLabel(date).toUpperCase());
            tvDate.setTextSize(11);
            tvDate.setTypeface(null, Typeface.BOLD);
            tvDate.setTextColor(0xFF777777);
            tvDate.setLetterSpacing(0.08f);
            headerRow.addView(tvDate);

            // Daily total
            BigDecimal dayTotal = BigDecimal.ZERO;
            for (Transaction t : group) {
                if (t.getAmount() != null) dayTotal = dayTotal.add(t.getAmount());
            }
            TextView tvTotal = new TextView(requireContext());
            tvTotal.setText(formatAmount(dayTotal));
            tvTotal.setTextSize(11);
            tvTotal.setTypeface(null, Typeface.BOLD);
            tvTotal.setTextColor(0xFF777777);
            headerRow.addView(tvTotal);

            llHistoryContent.addView(headerRow);

            // Items card
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_menu_section_card);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = (int)(12 * dp);
            card.setLayoutParams(cardLp);

            for (int i = 0; i < group.size(); i++) {
                Transaction t = group.get(i);
                card.addView(buildPaymentItemView(t));
                if (i < group.size() - 1) {
                    View divider = new View(requireContext());
                    LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    divLp.setMarginStart((int)(54 * dp));
                    divider.setLayoutParams(divLp);
                    divider.setBackgroundColor(0xFFE6E6E6);
                    card.addView(divider);
                }
            }

            llHistoryContent.addView(card);
        }
    }

    private View buildPaymentItemView(Transaction t) {
        float dp = dp();
        int pad = (int)(12 * dp);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding((int)(12 * dp), pad, (int)(12 * dp), (int)(8 * dp));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Category icon
        String emoji = getEmoji(t.getDescription());
        int accentColor = getAccentColor(emoji);

        FrameLayout iconBox = new FrameLayout(requireContext());
        int iconSize = (int)(36 * dp);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd((int)(10 * dp));
        iconBox.setLayoutParams(iconLp);
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        iconBg.setColor(blendWithAlpha(accentColor, 0x20));
        iconBg.setCornerRadius(10 * dp);
        iconBox.setBackground(iconBg);

        TextView tvEmoji = new TextView(requireContext());
        FrameLayout.LayoutParams eLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        eLp.gravity = Gravity.CENTER;
        tvEmoji.setLayoutParams(eLp);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(16);
        iconBox.addView(tvEmoji);
        row.addView(iconBox);

        // Main content column (service + sub + repeat)
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Top line: service name + amount
        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        String serviceName = getServiceName(t.getDescription());
        TextView tvService = new TextView(requireContext());
        tvService.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvService.setText(serviceName);
        tvService.setTextSize(13);
        tvService.setTypeface(null, Typeface.BOLD);
        tvService.setTextColor(0xFF111111);
        topRow.addView(tvService);

        TextView tvAmount = new TextView(requireContext());
        tvAmount.setText(t.getAmount() != null ? formatAmount(t.getAmount()) : "—");
        tvAmount.setTextSize(13);
        tvAmount.setTypeface(null, Typeface.BOLD);
        tvAmount.setTextColor(0xFF111111);
        topRow.addView(tvAmount);
        col.addView(topRow);

        // Bottom line: sub-details + time + status badge
        LinearLayout botRow = new LinearLayout(requireContext());
        botRow.setOrientation(LinearLayout.HORIZONTAL);
        botRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = (int)(2 * dp);
        botRow.setLayoutParams(bLp);

        String sub = getSubInfo(t.getDescription());
        TextView tvSub = new TextView(requireContext());
        tvSub.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvSub.setText(sub);
        tvSub.setTextSize(11);
        tvSub.setTextColor(0xFF777777);
        tvSub.setSingleLine(true);
        botRow.addView(tvSub);

        // Time + status
        LinearLayout timeStatus = new LinearLayout(requireContext());
        timeStatus.setOrientation(LinearLayout.HORIZONTAL);
        timeStatus.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tsLp.setMarginStart((int)(8 * dp));
        timeStatus.setLayoutParams(tsLp);

        if (t.getTimestamp() != null) {
            TextView tvTime = new TextView(requireContext());
            tvTime.setText(new SimpleDateFormat("HH:mm", Locale.US).format(t.getTimestamp()));
            tvTime.setTextSize(10);
            tvTime.setTextColor(0xFFBBBBBB);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.setMarginEnd((int)(6 * dp));
            tvTime.setLayoutParams(tlp);
            timeStatus.addView(tvTime);
        }

        // Status badge
        TextView tvStatus = new TextView(requireContext());
        tvStatus.setText("Выполнен");
        tvStatus.setTextSize(9);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextColor(0xFF1A8A4A);
        int sp = (int)(7 * dp), svp = (int)(1 * dp);
        tvStatus.setPadding(sp, svp, sp, svp);
        tvStatus.setBackgroundResource(R.drawable.bg_transfer_info_green);
        timeStatus.addView(tvStatus);
        botRow.addView(timeStatus);
        col.addView(botRow);

        // "↺ Повторить" button (purple pill)
        TextView tvRepeat = new TextView(requireContext());
        tvRepeat.setText("↺ Повторить");
        tvRepeat.setTextSize(10);
        tvRepeat.setTypeface(null, Typeface.BOLD);
        tvRepeat.setTextColor(0xFF6B21A8);
        int rh = (int)(10 * dp), rv = (int)(3 * dp);
        tvRepeat.setPadding(rh, rv, rh, rv);
        tvRepeat.setBackgroundResource(R.drawable.bg_chip_purple);
        LinearLayout.LayoutParams rpLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rpLp.topMargin = (int)(6 * dp);
        tvRepeat.setLayoutParams(rpLp);
        tvRepeat.setClickable(true);
        tvRepeat.setFocusable(true);
        tvRepeat.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        col.addView(tvRepeat);

        row.addView(col);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Date getPeriodCutoff() {
        if (selectedPeriod == 0) return null;
        Calendar cal = Calendar.getInstance();
        switch (selectedPeriod) {
            case 1: // Этот месяц
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                break;
            case 2: cal.add(Calendar.MONTH, -3); break;
            case 3: cal.add(Calendar.YEAR, -1); break;
        }
        return cal.getTime();
    }

    private String formatDateLabel(Date date) {
        if (date == null) return "Ранее";
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        Calendar txCal = Calendar.getInstance();
        txCal.setTime(date);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM", new Locale("ru"));
        if (isSameDay(txCal, today)) return "Сегодня, " + sdf.format(date);
        if (isSameDay(txCal, yesterday)) return "Вчера, " + sdf.format(date);
        return sdf.format(date);
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "₸ 0";
        try {
            long val = amount.longValue();
            NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
            return "₸ " + fmt.format(val);
        } catch (Exception e) {
            return "₸ " + amount.toPlainString();
        }
    }

    private String getEmoji(String desc) {
        if (desc == null) return "💳";
        String s = desc.toLowerCase();
        if (s.contains("beeline") || s.contains("activ") || s.contains("kcell")
                || s.contains("tele2") || s.contains("altel") || s.contains("megacom")
                || s.contains("o!") || s.contains("связь") || s.contains("мобил")) return "📱";
        if (s.contains("электро") || s.contains("коммунал") || s.contains("жкх")
                || s.contains("вода") || s.contains("водо") || s.contains("газ")
                || s.contains("кск") || s.contains("тепло")) return "💡";
        if (s.contains("интернет") || s.contains("telecom") || s.contains("казахтелеком")
                || s.contains("wifi") || s.contains("кабель")) return "🌐";
        if (s.contains("game") || s.contains("игр") || s.contains("playstation")
                || s.contains("steam") || s.contains("xbox")) return "🎮";
        if (s.contains("ivi") || s.contains("okko") || s.contains("netflix")
                || s.contains("кино") || s.contains("телевид") || s.contains("тв ")) return "📺";
        if (s.contains("авиа") || s.contains("такси") || s.contains("транспорт")
                || s.contains("жд")) return "✈️";
        if (s.contains("kaspi") || s.contains("кошел") || s.contains("wallet")) return "💳";
        return "💳";
    }

    private int getAccentColor(String emoji) {
        switch (emoji) {
            case "📱": return 0xFF1A4A8A;
            case "💡": return 0xFFD97222;
            case "🌐": return 0xFF1A8A4A;
            case "🎮": return 0xFFCC2222;
            case "📺": return 0xFFD97222;
            case "✈️": return 0xFF6B21A8;
            default: return 0xFFC9A227;
        }
    }

    private String getServiceName(String desc) {
        if (desc == null || desc.isEmpty()) return "Платёж";
        int dot = desc.indexOf(" · ");
        return dot > 0 ? desc.substring(0, dot) : desc;
    }

    private String getSubInfo(String desc) {
        if (desc == null || desc.isEmpty()) return "";
        int dot = desc.indexOf(" · ");
        return dot > 0 ? desc.substring(dot + 3) : "";
    }

    private int blendWithAlpha(int color, int alpha) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(alpha, r, g, b);
    }

    private float dp() {
        return requireContext().getResources().getDisplayMetrics().density;
    }
}
