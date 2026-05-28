package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.models.MarketPurchase;
import com.digitalcompany.tumarsuperapp.network.models.MarketPurchasesResponse;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MarketPurchasesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market_purchases, container, false);

        ProgressBar progress = view.findViewById(R.id.progress_purchases);
        TextView tvEmpty      = view.findViewById(R.id.tv_purchases_empty);
        RecyclerView rv       = view.findViewById(R.id.rv_purchases);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        progress.setVisibility(View.VISIBLE);
        ApiClient.getApiService(requireContext()).getMarketOrders()
                .enqueue(new Callback<MarketPurchasesResponse>() {
            @Override
            public void onResponse(@NonNull Call<MarketPurchasesResponse> call,
                                   @NonNull Response<MarketPurchasesResponse> response) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                List<MarketPurchase> orders = null;
                if (response.isSuccessful() && response.body() != null) {
                    orders = response.body().orders;
                }
                if (orders == null || orders.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    rv.setAdapter(new PurchasesAdapter(orders));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarketPurchasesResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvEmpty.setText("Не удалось загрузить покупки");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────

    private static class PurchasesAdapter
            extends RecyclerView.Adapter<PurchasesAdapter.VH> {

        private final List<MarketPurchase> items;
        private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));

        PurchasesAdapter(List<MarketPurchase> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_market_purchase, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            MarketPurchase p = items.get(pos);

            h.tvOrderRef.setText("Заказ #" + p.orderRef);
            h.tvAmount.setText(fmt.format((long) p.amount) + " ₸");
            h.tvAddress.setText(p.address);

            // Статус
            String statusLabel;
            int statusColor;
            switch (p.status == null ? "shipping" : p.status) {
                case "delivered":
                    statusLabel = "Доставлен";
                    statusColor = 0xFF2E7D32;
                    break;
                case "cancelled":
                    statusLabel = "Отменён";
                    statusColor = 0xFFB71C1C;
                    break;
                case "processing":
                    statusLabel = "В обработке";
                    statusColor = 0xFFE65100;
                    break;
                default:
                    statusLabel = "На доставке";
                    statusColor = 0xFF1565C0;
                    break;
            }
            h.tvStatus.setText(statusLabel);
            h.tvStatus.setTextColor(statusColor);

            // Дата
            if (p.createdAt != null && !p.createdAt.isEmpty()) {
                try {
                    SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    SimpleDateFormat out = new SimpleDateFormat("d MMM yyyy", new Locale("ru"));
                    Date date = in.parse(p.createdAt);
                    if (date == null) throw new Exception();
                    h.tvDate.setText(out.format(date));
                } catch (Exception e) {
                    // fallback: trim to first 10 chars "yyyy-MM-dd"
                    h.tvDate.setText(p.createdAt.length() > 10
                            ? p.createdAt.substring(0, 10) : p.createdAt);
                }
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvOrderRef, tvAmount, tvAddress, tvStatus, tvDate;
            VH(View v) {
                super(v);
                tvOrderRef = v.findViewById(R.id.tv_purchase_ref);
                tvAmount   = v.findViewById(R.id.tv_purchase_amount);
                tvAddress  = v.findViewById(R.id.tv_purchase_address);
                tvStatus   = v.findViewById(R.id.tv_purchase_status);
                tvDate     = v.findViewById(R.id.tv_purchase_date);
            }
        }
    }
}
