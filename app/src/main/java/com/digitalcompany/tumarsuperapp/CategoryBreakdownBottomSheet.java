package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class CategoryBreakdownBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TOPUP_TOTAL        = "topup_total";
    private static final String ARG_TOPUP_COUNT        = "topup_count";
    private static final String ARG_PAYMENT_TOTAL      = "payment_total";
    private static final String ARG_PAYMENT_COUNT      = "payment_count";
    private static final String ARG_TRANSFER_OUT_TOTAL = "transfer_out_total";
    private static final String ARG_TRANSFER_OUT_COUNT = "transfer_out_count";
    private static final String ARG_TRANSFER_IN_TOTAL  = "transfer_in_total";
    private static final String ARG_TRANSFER_IN_COUNT  = "transfer_in_count";

    public static CategoryBreakdownBottomSheet newInstance(
            BigDecimal topupTotal, int topupCount,
            BigDecimal paymentTotal, int paymentCount,
            BigDecimal transferOutTotal, int transferOutCount,
            BigDecimal transferInTotal, int transferInCount) {

        CategoryBreakdownBottomSheet sheet = new CategoryBreakdownBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TOPUP_TOTAL,        topupTotal.toPlainString());
        args.putInt(ARG_TOPUP_COUNT,           topupCount);
        args.putString(ARG_PAYMENT_TOTAL,      paymentTotal.toPlainString());
        args.putInt(ARG_PAYMENT_COUNT,         paymentCount);
        args.putString(ARG_TRANSFER_OUT_TOTAL, transferOutTotal.toPlainString());
        args.putInt(ARG_TRANSFER_OUT_COUNT,    transferOutCount);
        args.putString(ARG_TRANSFER_IN_TOTAL,  transferInTotal.toPlainString());
        args.putInt(ARG_TRANSFER_IN_COUNT,     transferInCount);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_category_breakdown, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { dismiss(); return; }

        BigDecimal topupTotal       = new BigDecimal(args.getString(ARG_TOPUP_TOTAL,        "0"));
        int        topupCount       = args.getInt(ARG_TOPUP_COUNT, 0);
        BigDecimal paymentTotal     = new BigDecimal(args.getString(ARG_PAYMENT_TOTAL,      "0"));
        int        paymentCount     = args.getInt(ARG_PAYMENT_COUNT, 0);
        BigDecimal transferOutTotal = new BigDecimal(args.getString(ARG_TRANSFER_OUT_TOTAL, "0"));
        int        transferOutCount = args.getInt(ARG_TRANSFER_OUT_COUNT, 0);
        BigDecimal transferInTotal  = new BigDecimal(args.getString(ARG_TRANSFER_IN_TOTAL,  "0"));
        int        transferInCount  = args.getInt(ARG_TRANSFER_IN_COUNT, 0);

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
        fmt.setCurrency(Currency.getInstance("KZT"));
        fmt.setMaximumFractionDigits(0);
        fmt.setMinimumFractionDigits(0);

        TextView tvTopupAmount      = view.findViewById(R.id.tv_topup_amount);
        TextView tvTopupCount       = view.findViewById(R.id.tv_topup_count);
        TextView tvPaymentAmount    = view.findViewById(R.id.tv_payment_amount);
        TextView tvPaymentCount     = view.findViewById(R.id.tv_payment_count);
        TextView tvTransferOutAmount = view.findViewById(R.id.tv_transfer_out_amount);
        TextView tvTransferOutCount  = view.findViewById(R.id.tv_transfer_out_count);
        TextView tvTransferInAmount  = view.findViewById(R.id.tv_transfer_in_amount);
        TextView tvTransferInCount   = view.findViewById(R.id.tv_transfer_in_count);

        tvTopupAmount.setText("+" + fmt.format(topupTotal));
        tvTopupCount.setText(formatCount(topupCount));

        tvPaymentAmount.setText("-" + fmt.format(paymentTotal));
        tvPaymentCount.setText(formatCount(paymentCount));

        tvTransferOutAmount.setText("-" + fmt.format(transferOutTotal));
        tvTransferOutCount.setText(formatCount(transferOutCount));

        tvTransferInAmount.setText("+" + fmt.format(transferInTotal));
        tvTransferInCount.setText(formatCount(transferInCount));
    }

    private String formatCount(int count) {
        if (count == 1) return "1 операция";
        if (count >= 2 && count <= 4) return count + " операции";
        return count + " операций";
    }
}
