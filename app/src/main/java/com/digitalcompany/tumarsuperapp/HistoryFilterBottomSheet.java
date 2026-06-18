package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class HistoryFilterBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERIOD  = "period_days";
    private static final String ARG_CAT     = "category";
    private static final String ARG_TYPE    = "type";

    public interface OnFilterApplyListener {
        void onFilterApply(int periodDays, String category, String type);
    }

    private OnFilterApplyListener listener;

    private int    selectedPeriod;
    private String selectedCategory;
    private String selectedType;

    private TextView[] periodChips;
    private TextView[] categoryChips;
    private TextView[] typeChips;

    private static final int[]    PERIOD_DAYS   = { 1, 7, 30, 90 };
    private static final String[] CAT_VALUES    = { "ALL","MARKET","TRANSFER","PAYMENT","INCOME" };
    private static final String[] TYPE_VALUES   = { "ALL","EXPENSE","INCOME" };

    public static HistoryFilterBottomSheet newInstance(int periodDays, String category, String type) {
        HistoryFilterBottomSheet f = new HistoryFilterBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_PERIOD, periodDays);
        args.putString(ARG_CAT, category);
        args.putString(ARG_TYPE, type);
        f.setArguments(args);
        return f;
    }

    public void setOnFilterApplyListener(OnFilterApplyListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            selectedPeriod   = args.getInt(ARG_PERIOD, 30);
            selectedCategory = args.getString(ARG_CAT, "ALL");
            selectedType     = args.getString(ARG_TYPE, "ALL");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Period chips
        periodChips = new TextView[]{
            view.findViewById(R.id.chip_f_today),
            view.findViewById(R.id.chip_f_week),
            view.findViewById(R.id.chip_f_month),
            view.findViewById(R.id.chip_f_3months),
        };
        for (int i = 0; i < periodChips.length; i++) {
            final int idx = i;
            periodChips[i].setOnClickListener(v -> selectPeriod(idx));
        }

        // Category chips
        categoryChips = new TextView[]{
            view.findViewById(R.id.chip_f_cat_all),
            view.findViewById(R.id.chip_f_cat_market),
            view.findViewById(R.id.chip_f_cat_transfer),
            view.findViewById(R.id.chip_f_cat_payment),
            view.findViewById(R.id.chip_f_cat_income),
        };
        for (int i = 0; i < categoryChips.length; i++) {
            final int idx = i;
            categoryChips[i].setOnClickListener(v -> selectCategory(idx));
        }

        // Type chips
        typeChips = new TextView[]{
            view.findViewById(R.id.chip_f_type_all),
            view.findViewById(R.id.chip_f_type_expense),
            view.findViewById(R.id.chip_f_type_income),
        };
        for (int i = 0; i < typeChips.length; i++) {
            final int idx = i;
            typeChips[i].setOnClickListener(v -> selectType(idx));
        }

        // Reset initial visuals
        syncPeriodVisuals();
        syncCategoryVisuals();
        syncTypeVisuals();

        // Buttons
        view.findViewById(R.id.btn_filter_reset).setOnClickListener(v -> reset());
        view.findViewById(R.id.btn_filter_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_filter_apply).setOnClickListener(v -> apply());
    }

    private void selectPeriod(int idx) {
        selectedPeriod = PERIOD_DAYS[idx];
        syncPeriodVisuals();
    }

    private void selectCategory(int idx) {
        selectedCategory = CAT_VALUES[idx];
        syncCategoryVisuals();
    }

    private void selectType(int idx) {
        selectedType = TYPE_VALUES[idx];
        syncTypeVisuals();
    }

    private void syncPeriodVisuals() {
        if (periodChips == null || !isAdded()) return;
        for (int i = 0; i < periodChips.length; i++) {
            setActive(periodChips[i], PERIOD_DAYS[i] == selectedPeriod);
        }
    }

    private void syncCategoryVisuals() {
        if (categoryChips == null || !isAdded()) return;
        for (int i = 0; i < categoryChips.length; i++) {
            setActive(categoryChips[i], CAT_VALUES[i].equals(selectedCategory));
        }
    }

    private void syncTypeVisuals() {
        if (typeChips == null || !isAdded()) return;
        for (int i = 0; i < typeChips.length; i++) {
            setActive(typeChips[i], TYPE_VALUES[i].equals(selectedType));
        }
    }

    private void setActive(TextView chip, boolean active) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_chip_purple_active);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_purple);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }
    }

    private void reset() {
        selectedPeriod   = 30;
        selectedCategory = "ALL";
        selectedType     = "ALL";
        syncPeriodVisuals();
        syncCategoryVisuals();
        syncTypeVisuals();
    }

    private void apply() {
        if (listener != null) {
            listener.onFilterApply(selectedPeriod, selectedCategory, selectedType);
        }
        dismiss();
    }
}
