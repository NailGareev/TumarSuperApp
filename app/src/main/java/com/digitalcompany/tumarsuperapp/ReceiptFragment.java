package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.models.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class ReceiptFragment extends Fragment {

    private static final String ARG_TRANSACTION = "transaction";

    public static ReceiptFragment newInstance(Transaction tx) {
        ReceiptFragment f = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSACTION, tx);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receipt, container, false);
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

        view.findViewById(R.id.btn_back_receipt).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        Bundle args = getArguments();
        if (args == null) return;
        Transaction tx = (Transaction) args.getSerializable(ARG_TRANSACTION);
        if (tx == null) return;

        bindReceipt(view, tx);

        view.findViewById(R.id.btn_share_receipt).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Поделиться чеком", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_download_pdf).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Скачать PDF", Toast.LENGTH_SHORT).show());
    }

    private void bindReceipt(View view, Transaction tx) {
        String type = tx.getTransactionType();
        String merchant;
        String itemName;

        if ("PAYMENT".equals(type)) {
            String raw = tx.getDescription();
            merchant = (raw != null && !raw.isEmpty()) ? raw : "Оплата услуг";
            itemName = merchant;
        } else if ("TOPUP".equals(type)) {
            merchant = "Пополнение баланса";
            itemName = "Пополнение счёта Tumar Pay";
        } else if ("MARKET_REFUND".equals(type)) {
            merchant = "Tumar Market";
            itemName = "Возврат средств";
        } else {
            merchant = "Tumar Pay";
            itemName = "Перевод средств";
        }

        ((TextView) view.findViewById(R.id.tv_receipt_merchant)).setText(merchant);
        ((TextView) view.findViewById(R.id.tv_receipt_item_name)).setText(itemName);

        if (tx.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Almaty"));
            ((TextView) view.findViewById(R.id.tv_receipt_date)).setText(sdf.format(tx.getTimestamp()));
        }

        BigDecimal amt = tx.getAmount() != null ? tx.getAmount().abs() : BigDecimal.ZERO;
        NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
        fmt.setGroupingUsed(true);
        fmt.setMaximumFractionDigits(0);
        String amtStr = fmt.format(amt.longValue()) + " ₸";

        ((TextView) view.findViewById(R.id.tv_receipt_item_amount)).setText(amtStr);
        ((TextView) view.findViewById(R.id.tv_receipt_total)).setText(amtStr);

        String txId = "TXN-" + tx.getId();
        ((TextView) view.findViewById(R.id.tv_receipt_tx_id)).setText(txId);

        // Fiscal number: derive from tx id + timestamp
        long ts = tx.getTimestamp() != null ? tx.getTimestamp().getTime() : System.currentTimeMillis();
        String fiscalNo = "№ " + txId + " · " + ((ts / 1000) % 100000);
        ((TextView) view.findViewById(R.id.tv_receipt_fiscal_no)).setText(fiscalNo);
    }
}
