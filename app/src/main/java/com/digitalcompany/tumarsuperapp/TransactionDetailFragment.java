package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.models.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TransactionDetailFragment extends Fragment {

    private static final String ARG_TRANSACTION  = "transaction";
    private static final String ARG_CURRENT_USER = "current_user_id";

    public static TransactionDetailFragment newInstance(Transaction tx, int currentUserId) {
        TransactionDetailFragment f = new TransactionDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSACTION, tx);
        args.putInt(ARG_CURRENT_USER, currentUserId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_detail, container, false);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back_detail).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        Bundle args = getArguments();
        if (args == null) return;

        Transaction tx = (Transaction) args.getSerializable(ARG_TRANSACTION);
        int currentUserId = args.getInt(ARG_CURRENT_USER, -1);
        if (tx == null) return;

        bindHeader(view, tx, currentUserId);
        buildDetailsTable(view, tx, currentUserId);
        bindButtons(view, tx);
    }

    private void bindHeader(View view, Transaction tx, int currentUserId) {
        String type = tx.getTransactionType();
        boolean isIncoming = tx.getRecipientId() == currentUserId;

        FrameLayout iconBg = view.findViewById(R.id.fl_detail_icon_bg);
        ImageView icon     = view.findViewById(R.id.iv_detail_icon);
        TextView tvMerchant = view.findViewById(R.id.tv_detail_merchant);
        TextView tvStatus   = view.findViewById(R.id.tv_detail_status);
        TextView tvDate     = view.findViewById(R.id.tv_detail_date);
        TextView tvAmount   = view.findViewById(R.id.tv_detail_amount);

        String merchant;
        int iconRes;
        int iconBgRes;
        int iconTint;
        String amountPrefix;
        int amountColor;

        if ("MARKET_REFUND".equals(type)) {
            merchant    = "Tumar Market";
            iconRes     = R.drawable.ic_payment;
            iconBgRes   = R.drawable.bg_icon_circle_red;
            iconTint    = 0xFFCC2222;
            amountPrefix = "+";
            amountColor = ContextCompat.getColor(requireContext(), R.color.income_color);

        } else if ("PAYMENT".equals(type)) {
            String raw  = tx.getDescription();
            merchant    = (raw != null && !raw.isEmpty()) ? raw : "Оплата услуг";
            iconRes     = R.drawable.ic_payment;
            iconBgRes   = R.drawable.bg_icon_circle_orange;
            iconTint    = 0xFFD97222;
            amountPrefix = "-";
            amountColor = ContextCompat.getColor(requireContext(), R.color.expense_color);

        } else if ("TOPUP".equals(type)) {
            merchant    = "Пополнение баланса";
            iconRes     = R.drawable.ic_add_circle_outline;
            iconBgRes   = R.drawable.bg_icon_circle_green;
            iconTint    = 0xFF1A8A4A;
            amountPrefix = "+";
            amountColor = ContextCompat.getColor(requireContext(), R.color.income_color);

        } else {
            // TRANSFER
            if (isIncoming) {
                String name = formatName(tx.getSenderFirstName(), tx.getSenderLastName());
                merchant    = "Перевод от " + (name != null ? name : nvl(tx.getSenderPhone(), "—"));
                iconRes     = R.drawable.ic_arrow_downward;
                iconBgRes   = R.drawable.bg_icon_circle_green;
                iconTint    = 0xFF1A8A4A;
                amountPrefix = "+";
                amountColor = ContextCompat.getColor(requireContext(), R.color.income_color);
            } else {
                String name = formatName(tx.getRecipientFirstName(), tx.getRecipientLastName());
                merchant    = "Перевод " + (name != null ? name : nvl(tx.getRecipientPhone(), "—"));
                iconRes     = R.drawable.ic_arrow_upward;
                iconBgRes   = R.drawable.bg_icon_circle_blue;
                iconTint    = 0xFF1A4A8A;
                amountPrefix = "-";
                amountColor = ContextCompat.getColor(requireContext(), R.color.expense_color);
            }
        }

        iconBg.setBackgroundResource(iconBgRes);
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconTint, android.graphics.PorterDuff.Mode.SRC_IN);
        tvMerchant.setText(merchant);
        tvAmount.setTextColor(amountColor);

        BigDecimal amt = tx.getAmount() != null ? tx.getAmount().abs() : BigDecimal.ZERO;
        NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
        fmt.setGroupingUsed(true);
        fmt.setMaximumFractionDigits(0);
        tvAmount.setText(amountPrefix + fmt.format(amt.longValue()) + " ₸");

        if (tx.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Almaty"));
            tvDate.setText(sdf.format(tx.getTimestamp()));
        }

        // status stays green "✓ Выполнена"
        tvStatus.setVisibility(View.VISIBLE);
    }

    private void buildDetailsTable(View view, Transaction tx, int currentUserId) {
        LinearLayout table = view.findViewById(R.id.ll_detail_table);
        float d = getResources().getDisplayMetrics().density;

        String type = tx.getTransactionType();
        String category;
        if ("PAYMENT".equals(type))         category = "Оплата";
        else if ("TOPUP".equals(type))      category = "Пополнение";
        else if ("MARKET_REFUND".equals(type)) category = "Market";
        else if (tx.getRecipientId() == currentUserId) category = "Входящий перевод";
        else                                category = "Исходящий перевод";

        BigDecimal amt = tx.getAmount() != null ? tx.getAmount().abs() : BigDecimal.ZERO;
        NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
        fmt.setGroupingUsed(true);
        fmt.setMaximumFractionDigits(0);

        String txId = "TXN-" + tx.getId();

        addDetailRow(table, "Категория",   category,               false, d);
        addDetailRow(table, "Счёт",        "Tumar Pay",            false, d);
        addDetailRow(table, "Комиссия",    "0 ₸",                  false, d);
        addDetailRow(table, "Сумма",       fmt.format(amt.longValue()) + " ₸", false, d);
        addDetailRow(table, "ID операции", txId,                   true,  d);
    }

    private void addDetailRow(LinearLayout parent, String label, String value, boolean isLast, float d) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = (int)(14 * d);
        row.setPadding(pad, (int)(11 * d), pad, (int)(11 * d));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(0xFF777777);

        TextView tvValue = new TextView(requireContext());
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvValue.setText(value);
        tvValue.setTextSize(13f);
        tvValue.getPaint().setFakeBoldText(true);
        tvValue.setTextColor(0xFF111111);

        row.addView(tvLabel);
        row.addView(tvValue);
        parent.addView(row);

        if (!isLast) {
            View divider = new View(requireContext());
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divLp.setMarginStart((int)(14 * d));
            divider.setLayoutParams(divLp);
            divider.setBackgroundColor(0xFFE6E6E6);
            parent.addView(divider);
        }
    }

    private void bindButtons(View view, Transaction tx) {
        view.findViewById(R.id.btn_repeat).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Повтор операции недоступен", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_download_receipt).setOnClickListener(v ->
                openReceipt(tx));
    }

    private void openReceipt(Transaction tx) {
        ReceiptFragment receipt = ReceiptFragment.newInstance(tx);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, receipt)
                .addToBackStack(null)
                .commit();
    }

    private static String formatName(String first, String last) {
        if (first != null && !first.isEmpty() && last != null && !last.isEmpty())
            return first + " " + last.substring(0, 1).toUpperCase() + ".";
        if (first != null && !first.isEmpty()) return first;
        if (last  != null && !last.isEmpty())  return last;
        return null;
    }

    private static String nvl(String v, String fallback) {
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
