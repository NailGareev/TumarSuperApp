package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TopUpSuccessFragment extends Fragment {

    private static final String ARG_AMOUNT     = "amount";
    private static final String ARG_METHOD     = "method";
    private static final String ARG_CARD_LAST4 = "card_last4";
    private static final String ARG_NEW_BAL    = "new_balance";

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
        String amount = args != null ? args.getString(ARG_AMOUNT, "0")    : "0";
        String method = args != null ? args.getString(ARG_METHOD, "—")    : "—";
        String last4  = args != null ? args.getString(ARG_CARD_LAST4, "") : "";
        String newBal = args != null ? args.getString(ARG_NEW_BAL, "—")   : "—";

        TextView tvAmount     = view.findViewById(R.id.tv_success_amount);
        TextView tvMethod     = view.findViewById(R.id.tv_success_method);
        TextView tvCard       = view.findViewById(R.id.tv_success_card);
        TextView tvNewBalance = view.findViewById(R.id.tv_success_new_balance);

        tvAmount.setText("₸ " + amount);
        tvMethod.setText(method);
        tvCard.setText(last4.isEmpty() ? "—" : "•••• " + last4);
        tvNewBalance.setText(newBal);

        view.findViewById(R.id.btn_close_success).setOnClickListener(v -> navigateHome());
        view.findViewById(R.id.btn_go_home).setOnClickListener(v -> navigateHome());

        // Animate content
        playEnterAnimations(view);
    }

    private void playEnterAnimations(View root) {
        View checkmark   = root.findViewById(R.id.checkmark_container);
        View tvTitle     = root.findViewById(R.id.tv_success_title);
        View tvSubtitle  = root.findViewById(R.id.tv_success_subtitle);
        View tvAmount    = root.findViewById(R.id.tv_success_amount);
        View detailsCard = root.findViewById(R.id.details_card);
        View newBalance  = root.findViewById(R.id.new_balance_row);
        View btnHome     = root.findViewById(R.id.btn_go_home);

        // Start all hidden
        View[] fadeViews = { tvTitle, tvSubtitle, tvAmount, detailsCard, newBalance, btnHome };
        for (View v : fadeViews) {
            if (v != null) v.setAlpha(0f);
        }

        // 1. Checkmark bounces in
        if (checkmark != null) {
            checkmark.setAlpha(0f);
            checkmark.postDelayed(() -> {
                if (!isAdded()) return;
                checkmark.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.success_checkmark));
                checkmark.setAlpha(1f);
            }, 100);
        }

        // 2. Text fades + slides up sequentially
        long delay = 420;
        for (View v : fadeViews) {
            if (v == null) continue;
            final long d = delay;
            v.postDelayed(() -> {
                if (!isAdded()) return;
                v.setAlpha(1f);
                v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_slide_up));
            }, d);
            delay += 80;
        }
    }

    private void navigateHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToHome();
        }
    }
}
