package com.digitalcompany.tumarsuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.Promotion;
import com.digitalcompany.tumarsuperapp.network.models.PromotionsListResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PromotionsFragment extends Fragment {

    private static final String[] FILTER_TABS = {"Все", "Кэшбэк", "Кредиты", "Партнёры", "Новые"};
    private int activeFilterIndex = 0;
    private List<Promotion> allPromos = new ArrayList<>();
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_promotions, container, false);
        View promosBell = rootView.findViewById(R.id.btn_promos_bell);
        if (promosBell != null) {
            promosBell.setOnClickListener(v -> openNotifications());
        }
        buildFilterTabs();
        fetchPromotions();
        return rootView;
    }

    private void fetchPromotions() {
        ApiService api = ApiClient.getApiService(requireActivity().getApplicationContext());
        api.getPromotions().enqueue(new Callback<PromotionsListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PromotionsListResponse> call,
                                   @NonNull Response<PromotionsListResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allPromos = response.body().getPromotions();
                    if (allPromos == null) allPromos = new ArrayList<>();
                    buildPromoCards(FILTER_TABS[activeFilterIndex]);
                } else {
                    Toast.makeText(getContext(), "Не удалось загрузить акции", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PromotionsListResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Нет соединения с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildFilterTabs() {
        LinearLayout container = rootView.findViewById(R.id.filter_tabs_container);
        container.removeAllViews();

        for (int i = 0; i < FILTER_TABS.length; i++) {
            final int index = i;
            final String tab = FILTER_TABS[i];

            TextView chip = new TextView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(dpToPx(8));
            chip.setLayoutParams(params);
            chip.setText(tab);
            chip.setTextSize(13);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6));
            updateChipStyle(chip, i == activeFilterIndex);

            chip.setOnClickListener(v -> {
                activeFilterIndex = index;
                buildFilterTabs();
                buildPromoCards(tab);
            });

            container.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean active) {
        if (active) {
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_purple_active));
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_purple));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }
    }

    private void buildPromoCards(String filter) {
        LinearLayout container = rootView.findViewById(R.id.promos_container);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Promotion promo : allPromos) {
            boolean show = filter.equals("Все")
                    || (filter.equals("Кэшбэк")  && promo.getTag().equals("Кэшбэк"))
                    || (filter.equals("Кредиты")  && promo.getTag().equals("Кредит"))
                    || (filter.equals("Партнёры") && promo.getTag().equals("Партнёр"))
                    || (filter.equals("Новые")    && promo.isHot());
            if (!show) continue;

            View card = inflater.inflate(R.layout.item_promo_card, container, false);
            bindPromoCard(card, promo);
            card.setOnClickListener(v -> openPromoDetail(promo));
            container.addView(card);
        }
    }

    private void bindPromoCard(View card, Promotion promo) {
        TextView tvTag   = card.findViewById(R.id.text_promo_tag);
        TextView tvTitle = card.findViewById(R.id.text_promo_title);
        TextView tvSub   = card.findViewById(R.id.text_promo_subtitle);
        View badgeHot    = card.findViewById(R.id.badge_hot);
        View iconBox     = card.findViewById(R.id.promo_icon_box);

        tvTag.setText(promo.getTag());
        tvTitle.setText(promo.getTitle());
        tvSub.setText(promo.getSubtitle());
        badgeHot.setVisibility(promo.isHot() ? View.VISIBLE : View.GONE);

        int tagDrawable;
        int tagColor;
        switch (promo.getTag()) {
            case "Кредит":
                tagDrawable = R.drawable.bg_tag_credit;
                tagColor    = 0xFFC9A227;
                break;
            case "Партнёр":
                tagDrawable = R.drawable.bg_tag_partner;
                tagColor    = 0xFFD97222;
                break;
            default:
                tagDrawable = R.drawable.bg_tag_cashback;
                tagColor    = 0xFF6B21A8;
        }
        tvTag.setBackground(ContextCompat.getDrawable(requireContext(), tagDrawable));
        tvTag.setTextColor(tagColor);
        iconBox.setBackground(ContextCompat.getDrawable(requireContext(), tagDrawable));
    }

    private void openNotifications() {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, new NotificationsFragment())
                .addToBackStack(null)
                .commit();
    }

    private void openPromoDetail(Promotion promo) {
        if (getActivity() == null) return;
        Fragment detail = PromoDetailFragment.newInstance(
                promo.getTag(), promo.getTitle(), promo.getSubtitle(), promo.getBadge(),
                promo.getStat1Value(), promo.getStat1Label(),
                promo.getStat2Value(), promo.getStat2Label(),
                promo.getStat3Value(), promo.getStat3Label(),
                promo.getDescription(), promo.getTerms()
        );
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, detail)
                .addToBackStack(null)
                .commit();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
