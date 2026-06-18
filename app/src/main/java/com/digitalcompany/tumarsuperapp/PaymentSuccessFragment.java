package com.digitalcompany.tumarsuperapp;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Set;

public class PaymentSuccessFragment extends Fragment {

    private static final String ARG_SERVICE_NAME  = "svc_name";
    private static final String ARG_ACCOUNT       = "account";
    private static final String ARG_AMOUNT        = "amount";
    private static final String ARG_CATEGORY      = "category";
    private static final String ARG_NEW_BALANCE   = "new_balance";
    private static final String ARG_ACCENT_COLOR  = "accent_color";
    private static final String ARG_RECEIPT_NO    = "receipt_no";

    private static final String PREFS_FAVORITES   = "payment_favorites";
    private static final String KEY_FAV_SET       = "fav_set";
    private static final String KEY_FAV_ACCOUNT_PREFIX = "fav_account_";

    public static PaymentSuccessFragment newInstance(
            String serviceName, String account, String amount,
            String category, String newBalance, int accentColor) {
        return newInstance(serviceName, account, amount, category, newBalance, accentColor, null);
    }

    public static PaymentSuccessFragment newInstance(
            String serviceName, String account, String amount,
            String category, String newBalance, int accentColor, String receiptNo) {
        PaymentSuccessFragment f = new PaymentSuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVICE_NAME, serviceName);
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_NEW_BALANCE, newBalance);
        args.putInt(ARG_ACCENT_COLOR, accentColor);
        args.putString(ARG_RECEIPT_NO, receiptNo);
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
        return inflater.inflate(R.layout.fragment_payment_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String serviceName = args != null ? args.getString(ARG_SERVICE_NAME, "") : "";
        String account     = args != null ? args.getString(ARG_ACCOUNT, "")      : "";
        String amount      = args != null ? args.getString(ARG_AMOUNT, "0")      : "0";
        String category    = args != null ? args.getString(ARG_CATEGORY, "")     : "";
        String newBalance  = args != null ? args.getString(ARG_NEW_BALANCE, "—") : "—";
        int accentColor    = args != null ? args.getInt(ARG_ACCENT_COLOR, 0xFF6200EE) : 0xFF6200EE;
        String receiptNo   = args != null ? args.getString(ARG_RECEIPT_NO, null) : null;

        // Derive light / border colors
        int accentLight  = (accentColor & 0x00FFFFFF) | 0x1A000000; // 10% alpha
        int accentMedium = (accentColor & 0x00FFFFFF) | 0x35000000; // 21% alpha
        int accentBorder = (accentColor & 0x00FFFFFF) | 0x47000000; // 28% alpha

        // Service icon box
        FrameLayout flIcon = view.findViewById(R.id.fl_pay_success_icon);
        ImageView iconInner = view.findViewById(R.id.iv_pay_success_icon_inner);

        float density = getResources().getDisplayMetrics().density;
        GradientDrawable outerBg = new GradientDrawable();
        outerBg.setColor(accentLight);
        outerBg.setStroke((int)(1.5f * density), accentBorder);
        outerBg.setCornerRadius(20 * density);
        flIcon.setBackground(outerBg);

        GradientDrawable innerBg = new GradientDrawable();
        innerBg.setColor(accentMedium);
        innerBg.setCornerRadius(10 * density);
        if (iconInner != null) {
            iconInner.setBackground(innerBg);
            iconInner.setImageResource(PaymentsFragment.getServiceIconRes(serviceName));
            iconInner.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        // Populate text fields
        view.<TextView>findViewById(R.id.tv_pay_success_title).setText("Оплата прошла!");
        view.<TextView>findViewById(R.id.tv_pay_success_subtitle).setText(serviceName + " · " + account);
        view.<TextView>findViewById(R.id.tv_pay_success_amount).setText("₸ " + amount);
        view.<TextView>findViewById(R.id.tv_pay_svc_name).setText(serviceName);
        view.<TextView>findViewById(R.id.tv_pay_account).setText(account);
        view.<TextView>findViewById(R.id.tv_pay_new_balance).setText("₸ " + newBalance);
        TextView tvFee = view.findViewById(R.id.tv_pay_fee);
        if (tvFee != null) tvFee.setText("₸ 0");
        TextView tvReceiptNo = view.findViewById(R.id.tv_pay_receipt_no);
        if (tvReceiptNo != null) {
            tvReceiptNo.setText(receiptNo != null ? receiptNo : "—");
        }

        // Buttons
        view.findViewById(R.id.btn_close_pay_success).setOnClickListener(v -> navigateHome());
        view.findViewById(R.id.btn_pay_home).setOnClickListener(v -> navigateHome());
        view.findViewById(R.id.btn_pay_again).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Favorites logic
        setupFavorites(view, serviceName, category, account);

        // Entrance animations
        playEnterAnimations(view);
    }

    private void setupFavorites(View view, String serviceName, String category, String account) {
        View llAddFav   = view.findViewById(R.id.ll_add_favorite);
        View llFavAdded = view.findViewById(R.id.ll_fav_added);
        TextView tvFavAddedSub = view.findViewById(R.id.tv_fav_added_subtitle);
        View btnAdd  = view.findViewById(R.id.btn_add_favorite);
        View btnUndo = view.findViewById(R.id.btn_undo_fav);

        boolean isFav = isFavorite(serviceName);
        llAddFav.setVisibility(isFav ? View.GONE : View.VISIBLE);
        llFavAdded.setVisibility(isFav ? View.VISIBLE : View.GONE);
        if (isFav && tvFavAddedSub != null) {
            tvFavAddedSub.setText(serviceName + " · " + category);
        }

        btnAdd.setOnClickListener(v -> {
            addFavorite(serviceName, account);
            llAddFav.setVisibility(View.GONE);
            llFavAdded.setVisibility(View.VISIBLE);
            if (tvFavAddedSub != null) tvFavAddedSub.setText(serviceName + " · " + category);
        });

        btnUndo.setOnClickListener(v -> {
            removeFavorite(serviceName);
            llFavAdded.setVisibility(View.GONE);
            llAddFav.setVisibility(View.VISIBLE);
        });
    }

    private boolean isFavorite(String serviceName) {
        if (getContext() == null) return false;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_FAVORITES, android.content.Context.MODE_PRIVATE);
        Set<String> favs = prefs.getStringSet(KEY_FAV_SET, new HashSet<>());
        return favs.contains(serviceName);
    }

    private void addFavorite(String serviceName, String account) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_FAVORITES, android.content.Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_FAV_SET, new HashSet<>()));
        favs.add(serviceName);
        prefs.edit()
                .putStringSet(KEY_FAV_SET, favs)
                .putString(KEY_FAV_ACCOUNT_PREFIX + serviceName, account)
                .apply();
    }

    private void removeFavorite(String serviceName) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_FAVORITES, android.content.Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_FAV_SET, new HashSet<>()));
        favs.remove(serviceName);
        prefs.edit()
                .putStringSet(KEY_FAV_SET, favs)
                .remove(KEY_FAV_ACCOUNT_PREFIX + serviceName)
                .apply();
    }

    private void playEnterAnimations(View root) {
        View iconContainer = root.findViewById(R.id.pay_success_icon_container);
        View tvTitle       = root.findViewById(R.id.tv_pay_success_title);
        View tvSubtitle    = root.findViewById(R.id.tv_pay_success_subtitle);
        View tvAmount      = root.findViewById(R.id.tv_pay_success_amount);
        View receiptCard   = root.findViewById(R.id.pay_receipt_card);
        View balanceRow    = root.findViewById(R.id.container_add_favorite);
        View btnRow        = root.findViewById(R.id.btn_pay_home);

        View[] fadeViews = { tvTitle, tvSubtitle, tvAmount, receiptCard, balanceRow, btnRow };
        for (View v : fadeViews) {
            if (v != null) v.setAlpha(0f);
        }

        // 1. Icon bounces in
        if (iconContainer != null) {
            iconContainer.setAlpha(0f);
            iconContainer.postDelayed(() -> {
                if (!isAdded()) return;
                iconContainer.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.success_checkmark));
                iconContainer.setAlpha(1f);
            }, 100);
        }

        // 2. Content fades + slides up sequentially
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
