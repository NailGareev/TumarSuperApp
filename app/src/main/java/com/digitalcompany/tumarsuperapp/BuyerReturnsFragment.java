package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.MarketReturn;
import com.digitalcompany.tumarsuperapp.network.models.MarketReturnsResponse;
import com.digitalcompany.tumarsuperapp.network.models.ReturnActionResponse;
import com.digitalcompany.tumarsuperapp.network.models.ReturnStatusUpdateRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerReturnsFragment extends Fragment {

    private RecyclerView            rvReturns;
    private ProgressBar             progressBar;
    private View                    layoutEmpty;
    private ApiService              apiService;
    private BuyerReturnAdapter      adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_returns, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvReturns   = view.findViewById(R.id.rv_buyer_returns);
        progressBar = view.findViewById(R.id.progress_buyer_returns);
        layoutEmpty = view.findViewById(R.id.layout_buyer_returns_empty);

        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }

        adapter = new BuyerReturnAdapter(this);
        rvReturns.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReturns.setAdapter(adapter);

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_create_return);
        fab.setOnClickListener(v -> openCreateReturn());

        loadReturns();
    }

    private void openCreateReturn() {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, CreateReturnFragment.newInstance("", 0))
                .addToBackStack(null)
                .commit();
    }

    void loadReturns() {
        if (apiService == null) return;
        progressBar.setVisibility(View.VISIBLE);
        rvReturns.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        apiService.getMyReturns().enqueue(new Callback<MarketReturnsResponse>() {
            @Override
            public void onResponse(@NonNull Call<MarketReturnsResponse> call,
                                   @NonNull Response<MarketReturnsResponse> response) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<MarketReturn> list = response.body().returns;
                    if (list == null || list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvReturns.setVisibility(View.VISIBLE);
                        adapter.setItems(list);
                    }
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarketReturnsResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void markCourierSent(int returnId) {
        if (apiService == null) return;
        apiService.updateReturnStatus(new ReturnStatusUpdateRequest(returnId, "IN_TRANSIT"))
                .enqueue(new Callback<ReturnActionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ReturnActionResponse> call,
                                           @NonNull Response<ReturnActionResponse> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            Toast.makeText(getContext(), "Статус обновлён: возврат в пути к продавцу", Toast.LENGTH_SHORT).show();
                            loadReturns();
                        } else {
                            String msg = (response.body() != null && response.body().message != null)
                                    ? response.body().message : "Ошибка обновления статуса";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ReturnActionResponse> call, @NonNull Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    static class BuyerReturnAdapter extends RecyclerView.Adapter<BuyerReturnAdapter.VH> {

        private List<MarketReturn>       items = new ArrayList<>();
        private final BuyerReturnsFragment host;

        BuyerReturnAdapter(BuyerReturnsFragment host) { this.host = host; }

        void setItems(List<MarketReturn> list) {
            items = new ArrayList<>(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_buyer_return, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MarketReturn r = items.get(position);

            h.tvOrderRef.setText("Заказ " + r.orderRef);
            h.tvDate.setText(formatDate(r.createdAt));
            h.tvStatus.setText(statusLabel(r.status));
            h.tvReason.setText(r.reason);

            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);
            h.tvAmount.setText(fmt.format(r.amount));

            if ("COURIER_PENDING".equals(r.status)) {
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Передать курьеру");
                h.btnAction.setOnClickListener(v -> host.markCourierSent(r.id));
            } else {
                h.btnAction.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String statusLabel(String status) {
            if (status == null) return "";
            switch (status) {
                case "CREATED":          return "Создана заявка на возврат";
                case "COURIER_PENDING":  return "Передать возврат курьеру";
                case "IN_TRANSIT":       return "Возврат в пути к продавцу";
                case "PENDING_DECISION": return "Ожидаем решения продавца";
                case "REFUNDED":         return "Возврат успешно совершён";
                default:                 return status;
            }
        }

        private String formatDate(String raw) {
            if (raw == null) return "";
            try {
                SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                SimpleDateFormat out = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                Date d = in.parse(raw);
                return d != null ? out.format(d) : raw.substring(0, Math.min(10, raw.length()));
            } catch (Exception e) {
                return raw.length() >= 10 ? raw.substring(0, 10) : raw;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView       tvOrderRef, tvDate, tvAmount, tvStatus, tvReason;
            MaterialButton btnAction;

            VH(View v) {
                super(v);
                tvOrderRef = v.findViewById(R.id.tv_return_order_ref);
                tvDate     = v.findViewById(R.id.tv_return_date);
                tvAmount   = v.findViewById(R.id.tv_return_amount);
                tvStatus   = v.findViewById(R.id.tv_return_status);
                tvReason   = v.findViewById(R.id.tv_return_reason);
                btnAction  = v.findViewById(R.id.btn_return_action);
            }
        }
    }
}
