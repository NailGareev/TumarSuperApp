package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter.TourCard;
import java.util.ArrayList;
import java.util.List;

public class TravelFragment extends Fragment {

    private static final int[] BANNER_RES = {
            R.drawable.banner1, R.drawable.banner2, R.drawable.banner3
    };

    private ImageView ivBanner;
    private Button tabRecommended, tabHot;
    private ProgressBar progressTours;
    private TextView tvToursEmpty;
    private TourCardAdapter adapter;

    private final List<TourCard> allTours = new ArrayList<>();

    private int currentBanner = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable rotateBanner = new Runnable() {
        @Override public void run() {
            if (ivBanner == null) return;
            currentBanner = (currentBanner + 1) % BANNER_RES.length;
            ivBanner.setImageResource(BANNER_RES[currentBanner]);
            handler.postDelayed(this, 3000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_travel, container, false);

        ivBanner      = view.findViewById(R.id.iv_travel_banner);
        tabRecommended = view.findViewById(R.id.tab_recommended);
        tabHot        = view.findViewById(R.id.tab_hot);
        progressTours = view.findViewById(R.id.progress_tours);
        tvToursEmpty  = view.findViewById(R.id.tv_tours_empty);

        setupCategories(view);
        setupTabs();
        setupRecyclerView(view);
        loadHardcodedTours();

        handler.postDelayed(rotateBanner, 3000);
        return view;
    }

    private void setupCategories(View root) {
        root.findViewById(R.id.cat_avia).setOnClickListener(v ->
                navigateTo(new AviationFragment(), "aviation"));

        root.findViewById(R.id.cat_tours).setOnClickListener(v ->
                navigateTo(new TourSearchFragment(), "tour_search"));

        root.findViewById(R.id.cat_railway).setOnClickListener(v ->
                navigateTo(new TrainSearchFragment(), "train_search"));

        root.findViewById(R.id.cat_kazakhstan).setOnClickListener(v ->
                navigateTo(new KazakhstanFragment(), "kz"));
    }

    private void setupTabs() {
        tabRecommended.setOnClickListener(v -> {
            setTabActive(tabRecommended, tabHot);
            adapter.setItems(allTours);
        });
        tabHot.setOnClickListener(v -> {
            setTabActive(tabHot, tabRecommended);
            adapter.setItems(hotTours());
        });
    }

    private void setTabActive(Button active, Button inactive) {
        active.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFE53935));
        active.setTextColor(0xFFFFFFFF);
        inactive.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFEEEEEE));
        inactive.setTextColor(0xFF757575);
    }

    private void setupRecyclerView(View root) {
        RecyclerView rv = root.findViewById(R.id.rv_tours);
        adapter = new TourCardAdapter();
        adapter.setOnCardClickListener(this::openTourDetail);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(adapter);
        rv.setNestedScrollingEnabled(false);
    }

    private void loadHardcodedTours() {
        progressTours.setVisibility(View.GONE);
        tvToursEmpty.setVisibility(View.GONE);

        allTours.clear();
        allTours.add(new TourCard("Анталья, Турция", "Concorde De Luxe Resort", 5,
                450000, 37500, 12, 30, 642000,
                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400&h=300&fit=crop", false,
                "https://ht.kz/search?adults=2&nights_from=7&nights_to=14&arrival_country_id=19&meal=all_inclusive"));
        allTours.add(new TourCard("Дубай, ОАЭ", "Atlantis The Palm", 5,
                850000, 70833, 12, 15, 1000000,
                "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=400&h=300&fit=crop", false,
                "https://ht.kz/search?adults=2&nights_from=5&nights_to=10&arrival_country_id=170&meal=all_inclusive"));
        allTours.add(new TourCard("Пхукет, Таиланд", "Sala Phuket Resort", 5,
                380000, 31666, 12, 40, 633000,
                "https://images.unsplash.com/photo-1552733407-5d5c46c3bb3b?w=400&h=300&fit=crop", true,
                "https://ht.kz/search?adults=2&nights_from=10&nights_to=14&arrival_country_id=17&meal=all_inclusive"));
        allTours.add(new TourCard("Мальдивы", "COMO Cocoa Island", 5,
                1200000, 100000, 12, 10, 1333000,
                "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=400&h=300&fit=crop", false,
                "https://ht.kz/search?adults=2&nights_from=7&nights_to=14&arrival_country_id=86&meal=all_inclusive"));
        allTours.add(new TourCard("Хургада, Египет", "Steigenberger Al Dau", 5,
                280000, 23333, 12, 45, 509000,
                "https://images.unsplash.com/photo-1573843981267-be1480dde014?w=400&h=300&fit=crop", true,
                "https://ht.kz/search?adults=2&nights_from=7&nights_to=14&arrival_country_id=39&meal=all_inclusive"));
        allTours.add(new TourCard("Бали, Индонезия", "Four Seasons Bali", 5,
                490000, 40833, 12, 25, 653000,
                "https://images.unsplash.com/photo-1604999333679-b86d54738315?w=400&h=300&fit=crop", true,
                "https://ht.kz/search?adults=2&nights_from=10&nights_to=14&arrival_country_id=54&meal=all_inclusive"));
        allTours.add(new TourCard("Барселона, Испания", "Hotel Arts Barcelona", 5,
                620000, 51666, 12, 20, 775000,
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&h=300&fit=crop", false,
                "https://ht.kz/search?adults=2&nights_from=7&nights_to=10&arrival_country_id=70&meal=all_inclusive"));
        allTours.add(new TourCard("Паттайя, Таиланд", "Dusit Thani Pattaya", 5,
                270000, 22500, 12, 35, 415000,
                "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400&h=300&fit=crop", true,
                "https://ht.kz/search?adults=2&nights_from=10&nights_to=14&arrival_country_id=17&meal=all_inclusive"));
        allTours.add(new TourCard("Амальфи, Италия", "Belmond Hotel Caruso", 5,
                580000, 48333, 12, 18, 707000,
                "https://images.unsplash.com/photo-1534445867742-43195f401b6c?w=400&h=300&fit=crop", false,
                "https://ht.kz/search?adults=2&nights_from=7&nights_to=10&arrival_country_id=55&meal=all_inclusive"));
        allTours.add(new TourCard("Шарм-эш-Шейх", "Four Seasons Sharm", 5,
                310000, 25833, 12, 38, 500000,
                "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=400&h=300&fit=crop", true,
                "https://ht.kz/search?adults=2&nights_from=7&nights_to=14&arrival_country_id=39&meal=all_inclusive"));

        adapter.setItems(allTours);
    }

    private List<TourCard> hotTours() {
        List<TourCard> hot = new ArrayList<>();
        for (TourCard c : allTours) {
            if (c.isHot) hot.add(c);
        }
        return hot;
    }

    private void openTourDetail(TourCard card) {
        String url = !card.bookingUrl.isEmpty() ? card.bookingUrl : "https://ht.kz/";
        navigateTo(FlightWebFragment.newInstance(url, "ht.kz — Туры"), "tour_web");
    }

    private void navigateTo(Fragment fragment, String tag) {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(rotateBanner);
        ivBanner = null;
    }
}
