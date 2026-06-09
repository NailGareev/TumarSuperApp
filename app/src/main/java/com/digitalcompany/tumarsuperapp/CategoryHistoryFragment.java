package com.digitalcompany.tumarsuperapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryHistoryFragment extends Fragment {

    private static final String ARG_TOPUP_TOTAL        = "topup_total";
    private static final String ARG_TOPUP_COUNT        = "topup_count";
    private static final String ARG_PAYMENT_TOTAL      = "payment_total";
    private static final String ARG_PAYMENT_COUNT      = "payment_count";
    private static final String ARG_TRANSFER_OUT_TOTAL = "transfer_out_total";
    private static final String ARG_TRANSFER_OUT_COUNT = "transfer_out_count";
    private static final String ARG_TRANSFER_IN_TOTAL  = "transfer_in_total";
    private static final String ARG_TRANSFER_IN_COUNT  = "transfer_in_count";
    private static final String ARG_PERIOD_LABEL       = "period_label";

    public static CategoryHistoryFragment newInstance(
            BigDecimal topupTotal,   int topupCount,
            BigDecimal payTotal,     int payCount,
            BigDecimal outTotal,     int outCount,
            BigDecimal inTotal,      int inCount,
            String periodLabel) {

        CategoryHistoryFragment f = new CategoryHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOPUP_TOTAL,        topupTotal.toPlainString());
        args.putInt(ARG_TOPUP_COUNT,           topupCount);
        args.putString(ARG_PAYMENT_TOTAL,      payTotal.toPlainString());
        args.putInt(ARG_PAYMENT_COUNT,         payCount);
        args.putString(ARG_TRANSFER_OUT_TOTAL, outTotal.toPlainString());
        args.putInt(ARG_TRANSFER_OUT_COUNT,    outCount);
        args.putString(ARG_TRANSFER_IN_TOTAL,  inTotal.toPlainString());
        args.putInt(ARG_TRANSFER_IN_COUNT,     inCount);
        args.putString(ARG_PERIOD_LABEL,       periodLabel);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back_cat).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        Bundle args = getArguments();
        if (args == null) return;

        BigDecimal topupTotal = safeDecimal(args.getString(ARG_TOPUP_TOTAL));
        int        topupCount = args.getInt(ARG_TOPUP_COUNT, 0);
        BigDecimal payTotal   = safeDecimal(args.getString(ARG_PAYMENT_TOTAL));
        int        payCount   = args.getInt(ARG_PAYMENT_COUNT, 0);
        BigDecimal outTotal   = safeDecimal(args.getString(ARG_TRANSFER_OUT_TOTAL));
        int        outCount   = args.getInt(ARG_TRANSFER_OUT_COUNT, 0);
        BigDecimal inTotal    = safeDecimal(args.getString(ARG_TRANSFER_IN_TOTAL));
        int        inCount    = args.getInt(ARG_TRANSFER_IN_COUNT, 0);
        String periodLabel    = args.getString(ARG_PERIOD_LABEL, "1 месяц");

        TextView tvSubtitle = view.findViewById(R.id.tv_cat_period_subtitle);
        tvSubtitle.setText(periodLabel);

        // Total expenses for percentage calculation
        BigDecimal totalExp = payTotal.add(outTotal);
        if (totalExp.compareTo(BigDecimal.ZERO) == 0) totalExp = BigDecimal.ONE;

        // Categories: name, amount, count, color, isExpense
        class CatData {
            String name; BigDecimal amount; int count; int color; boolean expense;
            CatData(String n, BigDecimal a, int c, int clr, boolean e) {
                name=n; amount=a; count=c; color=clr; expense=e; }
        }

        List<CatData> cats = new ArrayList<>();
        cats.add(new CatData("Платежи",          payTotal,  payCount,   0xFFD97222, true));
        cats.add(new CatData("Переводы",          outTotal,  outCount,   0xFF1A4A8A, true));
        cats.add(new CatData("Пополнения",        topupTotal,topupCount, 0xFF1A8A4A, false));
        cats.add(new CatData("Вх. переводы",      inTotal,   inCount,    0xFF6B21A8, false));

        // Donut chart
        DonutChartView donut = view.findViewById(R.id.donut_chart);
        float[] pcts  = new float[cats.size()];
        int[]   clrs  = new int[cats.size()];
        BigDecimal grandTotal = topupTotal.add(payTotal).add(outTotal).add(inTotal);
        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) grandTotal = BigDecimal.ONE;

        NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
        fmt.setGroupingUsed(true);
        fmt.setMaximumFractionDigits(0);

        for (int i = 0; i < cats.size(); i++) {
            float pct = cats.get(i).amount.divide(grandTotal, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).floatValue();
            pcts[i] = pct;
            clrs[i] = cats.get(i).color;
        }
        String centerVal = fmt.format(totalExp.longValue()) + " ₸";
        donut.setData(pcts, clrs, "Расходы", centerVal);

        // Legend
        LinearLayout llLegend = view.findViewById(R.id.ll_legend);
        for (int i = 0; i < cats.size(); i++) {
            int pctInt = Math.round(pcts[i]);
            llLegend.addView(buildLegendRow(cats.get(i).name, pctInt, clrs[i]));
        }

        // Category list with progress bars
        LinearLayout llList = view.findViewById(R.id.ll_category_list);
        for (int i = 0; i < cats.size(); i++) {
            CatData cd = cats.get(i);
            int pctInt = Math.round(pcts[i]);
            String amtStr = (cd.expense ? "-" : "+") + fmt.format(cd.amount.longValue()) + " ₸";
            boolean isLast = (i == cats.size() - 1);
            llList.addView(buildCategoryRow(cd.name, cd.count, amtStr, pctInt, cd.color, cd.expense, isLast));
        }
    }

    private View buildLegendRow(String name, int pct, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 4, 0, 4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);

        View dot = new View(requireContext());
        int sz = (int)(8 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(sz, sz);
        dotLp.setMarginEnd((int)(6 * getResources().getDisplayMetrics().density));
        dot.setLayoutParams(dotLp);
        android.graphics.drawable.GradientDrawable circ = new android.graphics.drawable.GradientDrawable();
        circ.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circ.setColor(color);
        dot.setBackground(circ);

        TextView tvName = new TextView(requireContext());
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);
        tvName.setText(name);
        tvName.setTextSize(11f);
        tvName.setTextColor(0xFF444444);

        TextView tvPct = new TextView(requireContext());
        tvPct.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvPct.setText(pct + "%");
        tvPct.setTextSize(11f);
        tvPct.setTextColor(0xFF111111);
        tvPct.getPaint().setFakeBoldText(true);

        row.addView(dot);
        row.addView(tvName);
        row.addView(tvPct);
        return row;
    }

    private View buildCategoryRow(String name, int count, String amount, int pct, int color,
                                   boolean expense, boolean isLast) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout outer = new LinearLayout(requireContext());
        outer.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(14 * d);
        outer.setPadding(pad, (int)(11 * d), pad, (int)(11 * d));
        outer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Row: icon | name/amount row + progress
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Icon box
        FrameLayout iconBox = new FrameLayout(requireContext());
        int iconSz = (int)(38 * d);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(iconSz, iconSz));
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setColor((color & 0x00FFFFFF) | 0x1A000000);
        iconBg.setCornerRadius(10 * d);
        iconBox.setBackground(iconBg);

        View innerDot = new View(requireContext());
        int innerSz = (int)(18 * d);
        FrameLayout.LayoutParams innerLp = new FrameLayout.LayoutParams(innerSz, innerSz);
        innerLp.gravity = android.view.Gravity.CENTER;
        innerDot.setLayoutParams(innerLp);
        android.graphics.drawable.GradientDrawable innerBg = new android.graphics.drawable.GradientDrawable();
        innerBg.setColor((color & 0x00FFFFFF) | 0x33000000);
        innerBg.setCornerRadius(5 * d);
        innerDot.setBackground(innerBg);
        iconBox.addView(innerDot);

        // Text column
        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart((int)(12 * d));
        textCol.setLayoutParams(textLp);

        // Name + amount row
        LinearLayout nameAmtRow = new LinearLayout(requireContext());
        nameAmtRow.setOrientation(LinearLayout.HORIZONTAL);
        nameAmtRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        nameAmtRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvName = new TextView(requireContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name);
        tvName.setTextSize(13f);
        tvName.getPaint().setFakeBoldText(true);
        tvName.setTextColor(0xFF111111);

        TextView tvAmt = new TextView(requireContext());
        tvAmt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvAmt.setText(amount);
        tvAmt.setTextSize(13f);
        tvAmt.getPaint().setFakeBoldText(true);
        tvAmt.setTextColor(expense ? 0xFFC62828 : 0xFF2E7D32);

        nameAmtRow.addView(tvName);
        nameAmtRow.addView(tvAmt);

        // Progress bar
        ProgressBar progress = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(5 * d));
        progressLp.topMargin = (int)(6 * d);
        progress.setLayoutParams(progressLp);
        progress.setMax(100);
        android.graphics.drawable.GradientDrawable fill = new android.graphics.drawable.GradientDrawable();
        fill.setColor(color);
        fill.setCornerRadius(3 * d);
        progress.setProgressDrawable(new android.graphics.drawable.ClipDrawable(fill,
                android.view.Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL));
        progress.setProgress(pct);

        // Count label
        TextView tvCount = new TextView(requireContext());
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        countLp.topMargin = (int)(3 * d);
        tvCount.setLayoutParams(countLp);
        tvCount.setText(pct + "% · " + formatCount(count));
        tvCount.setTextSize(11f);
        tvCount.setTextColor(0xFF777777);

        textCol.addView(nameAmtRow);
        textCol.addView(progress);
        textCol.addView(tvCount);

        row.addView(iconBox);
        row.addView(textCol);
        outer.addView(row);

        // Divider
        if (!isLast) {
            View divider = new View(requireContext());
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divLp.topMargin = (int)(10 * d);
            divLp.setMarginStart((int)(50 * d));
            divider.setLayoutParams(divLp);
            divider.setBackgroundColor(0xFFE6E6E6);
            outer.addView(divider);
        }

        return outer;
    }

    private String formatCount(int n) {
        if (n == 0) return "нет операций";
        if (n == 1) return "1 операция";
        if (n <= 4) return n + " операции";
        return n + " операций";
    }

    private BigDecimal safeDecimal(String s) {
        try { return s != null ? new BigDecimal(s) : BigDecimal.ZERO; }
        catch (Exception e) { return BigDecimal.ZERO; }
    }
}
