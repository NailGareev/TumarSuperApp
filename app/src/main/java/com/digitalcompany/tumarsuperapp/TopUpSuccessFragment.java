package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TopUpSuccessFragment extends Fragment {

    private static final String ARG_AMOUNT    = "amount";
    private static final String ARG_METHOD    = "method";
    private static final String ARG_CARD_LAST4 = "card_last4";
    private static final String ARG_NEW_BAL   = "new_balance";

    public static TopUpSuccessFragment newInstance(
            String amount, String method, String cardLast4, String newBalance) {
        TopUpSuccessFragment f = new TopUpSuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_METHOD, method);
        args.putString(ARG_CARD_LAST4, cardLast4);
        args.putString(ARG_NEW_BAL, newBalance);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topup_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String amount  = args != null ? args.getString(ARG_AMOUNT, "0")    : "0";
        String method  = args != null ? args.getString(ARG_METHOD, "—")    : "—";
        String last4   = args != null ? args.getString(ARG_CARD_LAST4, "") : "";
        String newBal  = args != null ? args.getString(ARG_NEW_BAL, "—")   : "—";

        TextView tvAmount     = view.findViewById(R.id.tv_success_amount);
        TextView tvMethod     = view.findViewById(R.id.tv_success_method);
        TextView tvCard       = view.findViewById(R.id.tv_success_card);
        TextView tvNewBalance = view.findViewById(R.id.tv_success_new_balance);

        tvAmount.setText("₸ " + amount);
        tvMethod.setText(method);
        tvCard.setText(last4.isEmpty() ? "—" : "•••• " + last4);
        tvNewBalance.setText(newBal);

        View btnClose = view.findViewById(R.id.btn_close_success);
        View btnHome  = view.findViewById(R.id.btn_go_home);

        btnClose.setOnClickListener(v -> navigateHome());
        btnHome.setOnClickListener(v -> navigateHome());
    }

    private void navigateHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToHome();
        }
    }
}
