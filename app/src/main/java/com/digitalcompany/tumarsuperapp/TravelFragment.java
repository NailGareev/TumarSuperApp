package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter.TourCard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TravelFragment extends Fragment {

    private static final int[] BANNER_RES = {
            R.drawable.banner1, R.drawable.banner2, R.drawable.banner3
    };

    private ImageView ivBanner;
    private Button tabRecommended, tabHot;
    private TourCardAdapter adapter;

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

        ivBanner = view.findViewById(R.id.iv_travel_banner);
        tabRecommended = view.findViewById(R.id.tab_recommended);
        tabHot = view.findViewById(R.id.tab_hot);

        setupCategories(view);
        setupTabs();
        setupRecyclerView(view);

        handler.postDelayed(rotateBanner, 3000);
        return view;
    }

    private void setupCategories(View root) {
        root.findViewById(R.id.cat_avia).setOnClickListener(v ->
                navigateTo(new AviationFragment(), "aviation"));

        View[] stubs = {
                root.findViewById(R.id.cat_railway),
                root.findViewById(R.id.cat_tours),
                root.findViewById(R.id.cat_kazakhstan)
        };
        for (View stub : stubs) {
            stub.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Раздел в разработке", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupTabs() {
        tabRecommended.setOnClickListener(v -> {
            setTabActive(tabRecommended, tabHot);
            adapter.setItems(allCards());
        });
        tabHot.setOnClickListener(v -> {
            setTabActive(tabHot, tabRecommended);
            adapter.setItems(hotCards());
        });
    }

    private void setTabActive(Button active, Button inactive) {
        active.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFE53935));
        active.setTextColor(0xFFFFFFFF);
        inactive.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF2C2C2E));
        inactive.setTextColor(0xFF9E9E9E);
    }

    private void setupRecyclerView(View root) {
        RecyclerView rv = root.findViewById(R.id.rv_tours);
        adapter = new TourCardAdapter();
        adapter.setOnCardClickListener(this::openTourDetail);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(adapter);
        rv.setNestedScrollingEnabled(false);
        adapter.setItems(allCards());
    }

    private void openTourDetail(TourCardAdapter.TourCard card) {
        if (getChildFragmentManager() == null) return;
        TourDetailBottomSheet.newInstance(card).show(getChildFragmentManager(), "tour_detail");
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

    private List<TourCard> allCards() {
        return Arrays.asList(
            new TourCard("Вьетнам, Нячанг",  "Premier Havana Nha Trang Ho...", 5,
                    657780, 54815, 12, 25, 884923,  R.drawable.banner1, false),
            new TourCard("Вьетнам, Нячанг",  "Best Western Premier Marvell...", 5,
                    648869, 54073, 12, 27, 897484,  R.drawable.banner2, true),
            new TourCard("Вьетнам, Нячанг",  "The Signature Hotel Nha Trang", 5,
                    815352, 67946, 12,  7, 884294,  R.drawable.banner3, false),
            new TourCard("Вьетнам, Нячанг",  "The Westin Resort & Spa Cam...",  5,
                    811220, 67602, 12, 30, 1161047, R.drawable.banner1, true),
            new TourCard("Турция, Анталья",   "Rixos Premium Belek",            5,
                    890000, 74167, 12, 15, 1047058, R.drawable.banner2, false),
            new TourCard("ОАЭ, Дубай",        "Atlantis The Palm",              5,
                    1200000, 100000, 12, 10, 1333333, R.drawable.banner3, false),
            new TourCard("Таиланд, Паттайя",  "Amari Pattaya",                  4,
                    480000, 40000, 12, 20, 600000,  R.drawable.banner1, true),
            new TourCard("Мальдивы",           "Baros Maldives",                 5,
                    1350000, 112500, 12, 25, 1800000, R.drawable.banner2, true)
        );
    }

    private List<TourCard> hotCards() {
        List<TourCard> hot = new ArrayList<>();
        for (TourCard c : allCards()) {
            if (c.isHot) hot.add(c);
        }
        return hot;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(rotateBanner);
        ivBanner = null;
    }
}
