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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter.TourCard;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.models.Tour;
import com.digitalcompany.tumarsuperapp.network.models.TourListResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        loadTours();

        handler.postDelayed(rotateBanner, 3000);
        return view;
    }

    private void setupCategories(View root) {
        root.findViewById(R.id.cat_avia).setOnClickListener(v ->
                navigateTo(new AviationFragment(), "aviation"));

        root.findViewById(R.id.cat_tours).setOnClickListener(v ->
                navigateTo(new TourSearchFragment(), "tour_search"));

        View[] stubs = {
                root.findViewById(R.id.cat_railway),
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

    private void loadTours() {
        progressTours.setVisibility(View.VISIBLE);
        tvToursEmpty.setVisibility(View.GONE);

        ApiClient.getApiService(requireContext()).getTours().enqueue(new Callback<TourListResponse>() {
            @Override
            public void onResponse(@NonNull Call<TourListResponse> call,
                                   @NonNull Response<TourListResponse> response) {
                if (!isAdded()) return;
                progressTours.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().tours != null) {
                    allTours.clear();
                    for (Tour t : response.body().tours) {
                        allTours.add(TourCard.from(t));
                    }
                }
                if (allTours.isEmpty()) {
                    tvToursEmpty.setVisibility(View.VISIBLE);
                } else {
                    adapter.setItems(allTours);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TourListResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressTours.setVisibility(View.GONE);
                tvToursEmpty.setVisibility(View.VISIBLE);
                tvToursEmpty.setText("Не удалось загрузить туры");
            }
        });
    }

    private List<TourCard> hotTours() {
        List<TourCard> hot = new ArrayList<>();
        for (TourCard c : allTours) {
            if (c.isHot) hot.add(c);
        }
        return hot;
    }

    private void openTourDetail(TourCard card) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(rotateBanner);
        ivBanner = null;
    }
}
