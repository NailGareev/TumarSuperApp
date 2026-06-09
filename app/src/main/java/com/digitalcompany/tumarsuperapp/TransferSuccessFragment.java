package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class TransferSuccessFragment extends Fragment {

    private static final String TAG = "TransferSuccessFragment";
    private static final String ARG_RECIPIENT = "recipient_name";
    private static final String ARG_AMOUNT    = "amount";
    private static final String ARG_METHOD    = "method";

    public static TransferSuccessFragment newInstance(String recipientName, String amount, String method) {
        TransferSuccessFragment f = new TransferSuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RECIPIENT, recipientName);
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_METHOD, method);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String recipientName = args != null ? args.getString(ARG_RECIPIENT, "Получатель") : "Получатель";
        String amount = args != null ? args.getString(ARG_AMOUNT, "0") : "0";
        String method = args != null ? args.getString(ARG_METHOD, "PHONE") : "PHONE";

        // Initials from recipient name
        TextView tvInitials = view.findViewById(R.id.tv_success_initials);
        if (tvInitials != null) {
            tvInitials.setText(buildInitials(recipientName));
        }

        // To label
        TextView tvToLabel = view.findViewById(R.id.tv_success_to_label);
        if (tvToLabel != null) {
            tvToLabel.setText("Отправлено " + recipientName);
        }

        // Amount
        TextView tvAmount = view.findViewById(R.id.tv_success_amount);
        if (tvAmount != null) {
            try {
                long amountLong = (long) Double.parseDouble(amount);
                tvAmount.setText(String.format("₸ %,d", amountLong).replace(',', ' '));
            } catch (NumberFormatException e) {
                tvAmount.setText("₸ " + amount);
            }
        }

        // Recipient detail row
        TextView tvRecipient = view.findViewById(R.id.tv_success_recipient);
        if (tvRecipient != null) tvRecipient.setText(recipientName);

        // Method detail row
        TextView tvMethod = view.findViewById(R.id.tv_success_method);
        if (tvMethod != null) {
            switch (method) {
                case "CARD": tvMethod.setText("По номеру карты"); break;
                case "INTERNATIONAL": tvMethod.setText("SWIFT / SEPA"); break;
                default: tvMethod.setText("По номеру телефона"); break;
            }
        }

        // Balance
        TextView tvBalance = view.findViewById(R.id.tv_success_balance);
        if (tvBalance != null) loadBalance(tvBalance);

        // Close button → pop back to transfer
        view.findViewById(R.id.btn_success_close).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Repeat → go back to TransferFragment
        view.findViewById(R.id.btn_success_repeat).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Home → clear entire back stack
        view.findViewById(R.id.btn_success_home).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE));
    }

    private String buildInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private void loadBalance(TextView tvBalance) {
        if (getContext() == null) return;
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("CardDataPrefs", Context.MODE_PRIVATE);
            for (int i = 0; i < 5; i++) {
                String balance = prefs.getString("card_" + i + "_balance", null);
                String blocked = prefs.getString("card_" + i + "_is_blocked", "false");
                if (balance != null && !"true".equals(blocked)) {
                    try {
                        long bal = (long) Double.parseDouble(balance);
                        tvBalance.setText(String.format("₸ %,d", bal).replace(',', ' '));
                    } catch (NumberFormatException e) {
                        tvBalance.setText("₸ " + balance);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Balance load error: " + e.getMessage());
        }
        tvBalance.setText("₸ —");
    }
}
