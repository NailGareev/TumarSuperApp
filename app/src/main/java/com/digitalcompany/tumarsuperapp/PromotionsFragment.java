package com.digitalcompany.tumarsuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class PromotionsFragment extends Fragment {

    private static final String[] FILTER_TABS = {"Все", "Кэшбэк", "Кредиты", "Партнёры", "Новые"};
    private int activeFilterIndex = 0;

    private static class PromoItem {
        final String tag, title, subtitle, badge;
        final boolean hot;
        final String stat1V, stat1L, stat2V, stat2L, stat3V, stat3L;
        final String description, terms;

        PromoItem(String tag, String title, String subtitle, String badge, boolean hot,
                  String s1v, String s1l, String s2v, String s2l, String s3v, String s3l,
                  String desc, String terms) {
            this.tag = tag; this.title = title; this.subtitle = subtitle;
            this.badge = badge; this.hot = hot;
            this.stat1V = s1v; this.stat1L = s1l; this.stat2V = s2v; this.stat2L = s2l;
            this.stat3V = s3v; this.stat3L = s3l;
            this.description = desc; this.terms = terms;
        }
    }

    private static final PromoItem[] ALL_PROMOS = {
        new PromoItem(
            "Кэшбэк", "До 7% кэшбэк",
            "На все покупки в Tumar Market до 30 июня",
            "Кэшбэк · До 30 июня", true,
            "7%", "Кэшбэк", "30 июня", "До", "₸ 50К", "Лимит",
            "Получайте до 7% кэшбэка на все покупки в Tumar Market при оплате картой Tumar. " +
            "Кэшбэк начисляется в течение 3 рабочих дней после совершения покупки.",
            "• Акция действует для всех держателей карты Tumar\n" +
            "• Максимальная сумма кэшбэка — ₸ 50 000 в месяц\n" +
            "• Кэшбэк не начисляется на покупки, оплаченные бонусами"
        ),
        new PromoItem(
            "Кредит", "0% на переводы",
            "Бесплатные переводы внутри Tumar Bank",
            "Кредит · Без ограничений", false,
            "0%", "Комиссия", "∞", "Лимит", "30 дней", "Период",
            "Переводите средства другим клиентам Tumar Bank абсолютно бесплатно. " +
            "Никаких скрытых комиссий и лимитов на количество переводов в сутки.",
            "• Акция действует для переводов между клиентами Tumar\n" +
            "• Одна транзакция — от ₸ 1 до ₸ 5 000 000\n" +
            "• Переводы в другие банки тарифицируются стандартно"
        ),
        new PromoItem(
            "Партнёр", "Скидка 15% в AirAstana",
            "При оплате картой Tumar",
            "Партнёр · До 31 декабря", false,
            "15%", "Скидка", "31 дек", "До", "Рейсы", "Тип",
            "Покупайте авиабилеты AirAstana со скидкой 15% при оплате картой Tumar. " +
            "Скидка применяется автоматически на сайте и в приложении AirAstana.",
            "• Скидка действует на все направления AirAstana\n" +
            "• Максимальная скидка — ₸ 30 000 на один билет\n" +
            "• Нельзя совмещать с другими промокодами"
        )
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_promotions, container, false);

        buildFilterTabs(view);
        buildPromoCards(view, "Все");

        return view;
    }

    private void buildFilterTabs(View root) {
        LinearLayout container = root.findViewById(R.id.filter_tabs_container);
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
                buildFilterTabs(root);
                buildPromoCards(root, tab);
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

    private void buildPromoCards(View root, String filter) {
        LinearLayout container = root.findViewById(R.id.promos_container);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (PromoItem promo : ALL_PROMOS) {
            boolean show = filter.equals("Все")
                    || (filter.equals("Кэшбэк") && promo.tag.equals("Кэшбэк"))
                    || (filter.equals("Кредиты") && promo.tag.equals("Кредит"))
                    || (filter.equals("Партнёры") && promo.tag.equals("Партнёр"))
                    || (filter.equals("Новые") && promo.hot);
            if (!show) continue;

            View card = inflater.inflate(R.layout.item_promo_card, container, false);

            TextView tvTag   = card.findViewById(R.id.text_promo_tag);
            TextView tvTitle = card.findViewById(R.id.text_promo_title);
            TextView tvSub   = card.findViewById(R.id.text_promo_subtitle);
            View badgeHot    = card.findViewById(R.id.badge_hot);
            View iconBox     = card.findViewById(R.id.promo_icon_box);

            tvTag.setText(promo.tag);
            tvTitle.setText(promo.title);
            tvSub.setText(promo.subtitle);
            badgeHot.setVisibility(promo.hot ? View.VISIBLE : View.GONE);

            int tagDrawable;
            int tagColor;
            switch (promo.tag) {
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

            final PromoItem item = promo;
            card.setOnClickListener(v -> openPromoDetail(item));

            container.addView(card);
        }
    }

    private void openPromoDetail(PromoItem promo) {
        if (getActivity() == null) return;
        Fragment detail = PromoDetailFragment.newInstance(
                promo.tag, promo.title, promo.subtitle, promo.badge,
                promo.stat1V, promo.stat1L, promo.stat2V, promo.stat2L,
                promo.stat3V, promo.stat3L, promo.description, promo.terms
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
