package com.digitalcompany.tumarsuperapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.digitalcompany.tumarsuperapp.network.models.ReturnIdRequest;

import org.json.JSONArray;

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

public class SellerReturnsFragment extends Fragment {

    private RecyclerView       rvReturns;
    private ProgressBar        progressBar;
    private View               layoutEmpty;
    private ApiService         apiService;
    private SellerReturnAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_returns, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvReturns   = view.findViewById(R.id.rv_seller_returns);
        progressBar = view.findViewById(R.id.progress_seller_returns);
        layoutEmpty = view.findViewById(R.id.layout_seller_returns_empty);

        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }

        adapter = new SellerReturnAdapter(this);
        rvReturns.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReturns.setAdapter(adapter);

        loadReturns();
    }

    private void loadReturns() {
        if (apiService == null) return;
        progressBar.setVisibility(View.VISIBLE);
        rvReturns.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        apiService.getAllReturns().enqueue(new Callback<MarketReturnsResponse>() {
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

    void acceptReturn(int returnId) {
        if (apiService == null) return;
        apiService.acceptReturn(new ReturnIdRequest(returnId)).enqueue(new Callback<ReturnActionResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReturnActionResponse> call,
                                   @NonNull Response<ReturnActionResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(getContext(), "Заявка принята. Покупатель передаст товар курьеру.", Toast.LENGTH_SHORT).show();
                    loadReturns();
                } else {
                    String msg = (response.body() != null && response.body().message != null)
                            ? response.body().message : "Ошибка";
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

    void refundReturn(int returnId, double amount) {
        if (apiService == null || getContext() == null) return;
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
        fmt.setCurrency(Currency.getInstance("KZT"));
        fmt.setMaximumFractionDigits(0);
        String amtStr = fmt.format(amount);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Подтверждение возврата")
                .setMessage("Вернуть " + amtStr + " покупателю?")
                .setPositiveButton("Вернуть", (d, w) -> doRefund(returnId))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void doRefund(int returnId) {
        if (apiService == null) return;
        apiService.refundReturn(new ReturnIdRequest(returnId)).enqueue(new Callback<ReturnActionResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReturnActionResponse> call,
                                   @NonNull Response<ReturnActionResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(getContext(), "Деньги успешно возвращены покупателю", Toast.LENGTH_SHORT).show();
                    loadReturns();
                } else {
                    String msg = (response.body() != null && response.body().message != null)
                            ? response.body().message : "Ошибка возврата";
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

    static class SellerReturnAdapter extends RecyclerView.Adapter<SellerReturnAdapter.VH> {

        private List<MarketReturn>  items = new ArrayList<>();
        private final SellerReturnsFragment host;

        SellerReturnAdapter(SellerReturnsFragment host) { this.host = host; }

        void setItems(List<MarketReturn> list) {
            items = new ArrayList<>(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_seller_return, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MarketReturn r = items.get(position);

            h.tvOrder.setText("Заказ " + r.orderRef);
            h.tvDate.setText(formatDate(r.createdAt));
            h.tvPhone.setText(r.phoneNumber != null ? r.phoneNumber : "");
            h.tvReason.setText(r.reason);
            h.tvStatus.setText(statusLabel(r.status));

            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);
            h.tvAmount.setText("-" + fmt.format(r.amount));

            // Photos
            bindPhotos(h, r.photosJson);

            // Buttons
            boolean canAccept = "CREATED".equals(r.status);
            boolean canRefund = !"REFUNDED".equals(r.status);

            h.btnAccept.setVisibility(canAccept ? View.VISIBLE : View.GONE);
            h.btnRefund.setVisibility(canRefund ? View.VISIBLE : View.GONE);

            if (canAccept) {
                h.btnAccept.setOnClickListener(v -> host.acceptReturn(r.id));
            }
            if (canRefund) {
                h.btnRefund.setOnClickListener(v -> host.refundReturn(r.id, r.amount));
            }
        }

        private void bindPhotos(VH h, String photosJson) {
            h.llPhotos.removeAllViews();
            if (photosJson == null || photosJson.isEmpty() || photosJson.equals("[]")) {
                h.scrollPhotos.setVisibility(View.GONE);
                return;
            }
            try {
                JSONArray arr = new JSONArray(photosJson);
                if (arr.length() == 0) { h.scrollPhotos.setVisibility(View.GONE); return; }
                h.scrollPhotos.setVisibility(View.VISIBLE);
                int dp = (int)(80 * h.itemView.getContext().getResources().getDisplayMetrics().density);
                int dp4 = (int)(4 * h.itemView.getContext().getResources().getDisplayMetrics().density);
                for (int i = 0; i < arr.length(); i++) {
                    String b64 = arr.getString(i);
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ImageView iv = new ImageView(h.itemView.getContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp, dp);
                    lp.setMargins(dp4, 0, dp4, 0);
                    iv.setLayoutParams(lp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap(bmp);
                    h.llPhotos.addView(iv);
                }
            } catch (Exception e) {
                h.scrollPhotos.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String statusLabel(String status) {
            if (status == null) return "";
            switch (status) {
                case "CREATED":           return "Новая заявка";
                case "COURIER_PENDING":   return "Ожидает курьера";
                case "IN_TRANSIT":        return "В пути к продавцу";
                case "PENDING_DECISION":  return "Ожидает решения";
                case "REFUNDED":          return "Возврат совершён";
                default:                  return status;
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
            android.widget.TextView tvOrder, tvDate, tvPhone, tvAmount, tvStatus, tvReason;
            com.google.android.material.button.MaterialButton btnAccept, btnRefund;
            android.widget.HorizontalScrollView scrollPhotos;
            LinearLayout llPhotos;

            VH(View v) {
                super(v);
                tvOrder      = v.findViewById(R.id.tv_seller_return_order);
                tvDate       = v.findViewById(R.id.tv_seller_return_date);
                tvPhone      = v.findViewById(R.id.tv_seller_return_phone);
                tvAmount     = v.findViewById(R.id.tv_seller_return_amount);
                tvStatus     = v.findViewById(R.id.tv_seller_return_status);
                tvReason     = v.findViewById(R.id.tv_seller_return_reason);
                btnAccept    = v.findViewById(R.id.btn_seller_accept);
                btnRefund    = v.findViewById(R.id.btn_seller_refund);
                scrollPhotos = v.findViewById(R.id.scroll_seller_photos);
                llPhotos     = v.findViewById(R.id.ll_seller_photos);
            }
        }
    }
}
