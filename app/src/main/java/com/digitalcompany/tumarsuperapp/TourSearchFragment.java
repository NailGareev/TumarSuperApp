package com.digitalcompany.tumarsuperapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter.TourCard;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.models.Tour;
import com.digitalcompany.tumarsuperapp.network.models.TourListResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TourSearchFragment extends Fragment {

    private EditText etFrom, etTo;
    private TextView tvDateFrom, tvDateTo;
    private TextView tvAdultsCount, tvChildrenCount;
    private TextView tvResultsLabel, tvEmpty;
    private ProgressBar progressBar;
    private TourCardAdapter adapter;

    private int adults = 2;
    private int children = 0;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tour_search, container, false);

        etFrom          = view.findViewById(R.id.et_from);
        etTo            = view.findViewById(R.id.et_to);
        tvDateFrom      = view.findViewById(R.id.tv_date_from);
        tvDateTo        = view.findViewById(R.id.tv_date_to);
        tvAdultsCount   = view.findViewById(R.id.tv_adults_count);
        tvChildrenCount = view.findViewById(R.id.tv_children_count);
        tvResultsLabel  = view.findViewById(R.id.tv_results_label);
        tvEmpty         = view.findViewById(R.id.tv_search_empty);
        progressBar     = view.findViewById(R.id.progress_search);

        setupDatePickers();
        setupPersonsControls(view);
        setupRecyclerView(view);

        view.findViewById(R.id.btn_search_tours).setOnClickListener(v -> searchTours());
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        return view;
    }

    private void setupDatePickers() {
        tvDateFrom.setOnClickListener(v -> showDatePicker(true));
        tvDateTo.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
            cal.set(year, month, day);
            String formatted = dateFmt.format(cal.getTime());
            if (isFrom) {
                tvDateFrom.setText(formatted);
            } else {
                tvDateTo.setText(formatted);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupPersonsControls(View root) {
        tvAdultsCount.setText(String.valueOf(adults));
        tvChildrenCount.setText(String.valueOf(children));

        root.findViewById(R.id.btn_adults_minus).setOnClickListener(v -> {
            if (adults > 1) { adults--; tvAdultsCount.setText(String.valueOf(adults)); }
        });
        root.findViewById(R.id.btn_adults_plus).setOnClickListener(v -> {
            if (adults < 9) { adults++; tvAdultsCount.setText(String.valueOf(adults)); }
        });
        root.findViewById(R.id.btn_children_minus).setOnClickListener(v -> {
            if (children > 0) { children--; tvChildrenCount.setText(String.valueOf(children)); }
        });
        root.findViewById(R.id.btn_children_plus).setOnClickListener(v -> {
            if (children < 6) { children++; tvChildrenCount.setText(String.valueOf(children)); }
        });
    }

    private void setupRecyclerView(View root) {
        RecyclerView rv = root.findViewById(R.id.rv_search_results);
        adapter = new TourCardAdapter();
        adapter.setOnCardClickListener(card ->
                TourDetailBottomSheet.newInstance(card)
                        .show(getChildFragmentManager(), "tour_detail"));
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(adapter);
        rv.setNestedScrollingEnabled(false);
    }

    private void searchTours() {
        String destination = etTo.getText().toString().trim();
        String destParam = destination.isEmpty() ? null : destination;

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        tvResultsLabel.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        ApiClient.getApiService(requireContext())
                .searchTours(destParam, adults, children)
                .enqueue(new Callback<TourListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TourListResponse> call,
                                           @NonNull Response<TourListResponse> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        List<TourCard> results = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().tours != null) {
                            for (Tour t : response.body().tours) {
                                results.add(TourCard.from(t));
                            }
                        }
                        if (results.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("Туры не найдены.\nПопробуйте изменить параметры поиска.");
                        } else {
                            tvResultsLabel.setVisibility(View.VISIBLE);
                            tvResultsLabel.setText("НАЙДЕНО ТУРОВ: " + results.size());
                            adapter.setItems(results);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TourListResponse> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Ошибка подключения.\nПроверьте интернет-соединение.");
                    }
                });
    }
}
