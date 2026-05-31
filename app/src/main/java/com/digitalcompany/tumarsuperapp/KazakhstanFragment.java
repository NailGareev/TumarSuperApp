package com.digitalcompany.tumarsuperapp;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class KazakhstanFragment extends Fragment {

    private static final String[][] DESTINATIONS = {
        {"Алматы",               "Almaty, Kazakhstan",         "🏙 Южная столица • горы",     "🏙"},
        {"Нур-Султан (Астана)",  "Astana, Kazakhstan",         "🏛 Столица • Байтерек",        "🏛"},
        {"Шымкент",              "Shymkent, Kazakhstan",       "☀️ Южный мегаполис",           "☀️"},
        {"Боровое (Бурабай)",    "Borovoye, Kazakhstan",       "🌲 Природный курорт",          "🌲"},
        {"Туркестан",            "Turkestan, Kazakhstan",      "🕌 Древний город • ЮНЕСКО",    "🕌"},
        {"Актау",                "Aktau, Kazakhstan",          "🌊 Каспийское море",           "🌊"},
        {"Чарын",                "Charyn Canyon, Kazakhstan",  "🏜 Великий каньон",            "🏜"},
        {"Шымбулак",             "Shymbulak, Kazakhstan",      "⛷ Горнолыжный курорт",        "⛷"},
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kazakhstan, container, false);

        view.findViewById(R.id.btn_kz_back).setOnClickListener(v ->
                requireActivity().onBackPressed());

        RecyclerView rv = view.findViewById(R.id.rv_kz_destinations);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new DestinationAdapter());

        return view;
    }

    private class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Build card programmatically to avoid a separate layout file
            CardView card = new CardView(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT);
            int mH = dp(4);
            int mV = dp(4);
            lp.setMargins(dp(16), mV, dp(16), mV);
            card.setLayoutParams(lp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(3));
            card.setCardBackgroundColor(0xFFFFFFFF);

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = dp(16);
            row.setPadding(pad, pad, pad, pad);
            row.setBackground(getContext() != null
                    ? getContext().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground})
                            .getDrawable(0)
                    : null);
            row.setClickable(true);
            row.setFocusable(true);
            card.addView(row, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));

            // Emoji circle
            FrameLayout circle = new FrameLayout(parent.getContext());
            int size = dp(56);
            LinearLayout.LayoutParams circleLP = new LinearLayout.LayoutParams(size, size);
            circle.setLayoutParams(circleLP);
            try {
                circle.setBackground(parent.getContext().getDrawable(R.drawable.bg_icon_circle_purple));
            } catch (Exception ignored) {}

            TextView tvEmoji = new TextView(parent.getContext());
            tvEmoji.setTextSize(22);
            tvEmoji.setGravity(android.view.Gravity.CENTER);
            FrameLayout.LayoutParams emojiLP = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            tvEmoji.setLayoutParams(emojiLP);
            circle.addView(tvEmoji);
            row.addView(circle);

            // Text block
            LinearLayout textBlock = new LinearLayout(parent.getContext());
            textBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLP = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textLP.setMarginStart(dp(14));
            textBlock.setLayoutParams(textLP);

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextSize(16);
            tvTitle.setTextColor(0xFF212121);
            tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);

            TextView tvDesc = new TextView(parent.getContext());
            tvDesc.setTextSize(13);
            tvDesc.setTextColor(0xFF9E9E9E);
            tvDesc.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            textBlock.addView(tvTitle);
            textBlock.addView(tvDesc);
            row.addView(textBlock);

            // Chevron
            ImageView chevron = new ImageView(parent.getContext());
            LinearLayout.LayoutParams chevLP = new LinearLayout.LayoutParams(dp(18), dp(18));
            chevLP.setMarginStart(dp(8));
            chevron.setLayoutParams(chevLP);
            try {
                chevron.setImageDrawable(parent.getContext().getDrawable(R.drawable.ic_chevron_right_24dp));
                chevron.setColorFilter(0xFF9E9E9E);
            } catch (Exception ignored) {}
            row.addView(chevron);

            return new VH(card, row, tvEmoji, tvTitle, tvDesc);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            String[] dest = DESTINATIONS[pos];
            String ruName  = dest[0];
            String engName = dest[1];
            String desc    = dest[2];
            String emoji   = dest[3];

            h.tvEmoji.setText(emoji);
            h.tvTitle.setText(ruName);
            h.tvDesc.setText(desc);

            h.row.setOnClickListener(v -> {
                String url = "https://www.booking.com/searchresults.ru.html?ss="
                        + Uri.encode(engName) + "&lang=ru";
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,
                                FlightWebFragment.newInstance(url, "Booking.com — " + ruName))
                        .addToBackStack("kz_booking")
                        .commit();
            });
        }

        @Override
        public int getItemCount() { return DESTINATIONS.length; }

        class VH extends RecyclerView.ViewHolder {
            final LinearLayout row;
            final TextView tvEmoji, tvTitle, tvDesc;

            VH(CardView card, LinearLayout row, TextView tvEmoji, TextView tvTitle, TextView tvDesc) {
                super(card);
                this.row = row;
                this.tvEmoji = tvEmoji;
                this.tvTitle = tvTitle;
                this.tvDesc = tvDesc;
            }
        }

        private int dp(int dp) {
            return Math.round(dp * getResources().getDisplayMetrics().density);
        }
    }
}
