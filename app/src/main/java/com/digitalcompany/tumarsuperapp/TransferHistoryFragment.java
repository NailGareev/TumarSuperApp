package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferHistoryFragment extends Fragment {

    private static final String USER_PREFS = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;
    private ApiService apiService;
    private int currentUserId = -1;

    public static TransferHistoryFragment newInstance() {
        return new TransferHistoryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
            currentUserId = getActivity()
                    .getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
                    .getInt(KEY_USER_ID, -1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_transfer_history_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        recyclerView = view.findViewById(R.id.rv_transfer_history);
        progressBar  = view.findViewById(R.id.progress_transfer_history);
        emptyView    = view.findViewById(R.id.empty_transfer_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadHistory();
    }

    private void loadHistory() {
        if (apiService == null) return;
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        apiService.getTransactionHistory().enqueue(new Callback<TransactionHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<TransactionHistoryResponse> call,
                                   @NonNull Response<TransactionHistoryResponse> response) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);

                List<Transaction> all = response.isSuccessful() && response.body() != null
                        ? response.body().getTransactions() : null;

                // Filter only TRANSFER transactions
                List<Transaction> transfers = new ArrayList<>();
                if (all != null) {
                    for (Transaction t : all) {
                        if ("TRANSFER".equals(t.getTransactionType())) transfers.add(t);
                    }
                }

                if (transfers.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(new TransferHistoryAdapter(transfers, currentUserId));
            }

            @Override
            public void onFailure(@NonNull Call<TransactionHistoryResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── Adapter with date group headers ──────────────────────────────────────

    static class TransferHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM   = 1;

        // List items: String = date header, Transaction = transfer row
        private final List<Object> rows = new ArrayList<>();
        private final int myUserId;
        private final NumberFormat fmt;
        private static final int[] AVATAR_COLORS = {
            0xFF6B21A8, 0xFF1A4A8A, 0xFF1A8A4A, 0xFFC9A227, 0xFFD97222
        };

        TransferHistoryAdapter(List<Transaction> transfers, int myUserId) {
            this.myUserId = myUserId;
            fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);
            buildRows(transfers);
        }

        private void buildRows(List<Transaction> transfers) {
            SimpleDateFormat dayFmt = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
            String lastDay = null;
            for (Transaction t : transfers) {
                String day = t.getTimestamp() != null ? dayFmt.format(t.getTimestamp()) : "Неизвестная дата";
                if (!day.equals(lastDay)) {
                    rows.add(day);
                    lastDay = day;
                }
                rows.add(t);
            }
        }

        @Override public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                return new HeaderVH(inf.inflate(R.layout.item_transfer_history_header, parent, false));
            }
            return new ItemVH(inf.inflate(R.layout.item_transfer_history_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).tvDate.setText((String) rows.get(position));
            } else {
                bindItem((ItemVH) holder, (Transaction) rows.get(position));
            }
        }

        private void bindItem(ItemVH h, Transaction t) {
            boolean isSender = t.getSenderId() == myUserId;

            // Counter-party info
            String firstName = isSender
                    ? (t.getRecipientFirstName() != null ? t.getRecipientFirstName() : "")
                    : (t.getSenderFirstName()    != null ? t.getSenderFirstName()    : "");
            String lastName = isSender
                    ? (t.getRecipientLastName() != null ? t.getRecipientLastName() : "")
                    : (t.getSenderLastName()    != null ? t.getSenderLastName()    : "");
            String phone = isSender
                    ? (t.getRecipientPhone() != null ? t.getRecipientPhone() : "")
                    : (t.getSenderPhone()    != null ? t.getSenderPhone()    : "");

            String name = firstName.isEmpty()
                    ? (phone.isEmpty() ? "Неизвестно" : phone)
                    : (lastName.isEmpty() ? firstName : firstName + " " + lastName);

            String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();

            h.tvName.setText(name);
            h.tvAvatar.setText(initial);

            int avatarColor = AVATAR_COLORS[Math.abs((isSender ? t.getRecipientId() : t.getSenderId())) % AVATAR_COLORS.length];
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(avatarColor);
            h.tvAvatar.setBackground(gd);

            // Load real avatar photo if available, otherwise show colored initial
            String avatarUrl = isSender ? t.getRecipientAvatarUrl() : t.getSenderAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = ApiClient.BASE_URL.replaceAll("/$", "") + avatarUrl;
                h.ivAvatar.setVisibility(View.VISIBLE);
                h.tvAvatar.setVisibility(View.INVISIBLE); // keep as background placeholder
                Glide.with(h.itemView.getContext())
                        .load(fullUrl)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(h.tvAvatar.getBackground())
                        .error(h.tvAvatar.getBackground())
                        .into(h.ivAvatar);
            } else {
                h.ivAvatar.setVisibility(View.GONE);
                h.tvAvatar.setVisibility(View.VISIBLE);
            }

            // Direction label
            h.tvDirection.setText(isSender ? "Исходящий перевод" : "Входящий перевод");
            h.tvDirection.setTextColor(isSender ? 0xFF9E9E9E : 0xFF9E9E9E);

            // Amount
            String amtStr = t.getAmount() != null ? fmt.format(t.getAmount()) : "—";
            h.tvAmount.setText(isSender ? "- " + amtStr : "+ " + amtStr);
            h.tvAmount.setTextColor(isSender ? 0xFFC62828 : 0xFF2E7D32);

            // Time
            if (t.getTimestamp() != null) {
                h.tvTime.setText(new SimpleDateFormat("HH:mm", new Locale("ru")).format(t.getTimestamp()));
            } else {
                h.tvTime.setText("");
            }

            // Description
            if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                h.tvDesc.setVisibility(View.VISIBLE);
                h.tvDesc.setText(t.getDescription());
            } else {
                h.tvDesc.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return rows.size(); }

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView tvDate;
            HeaderVH(View v) { super(v); tvDate = v.findViewById(R.id.tv_history_date_header); }
        }

        static class ItemVH extends RecyclerView.ViewHolder {
            TextView  tvAvatar, tvName, tvDirection, tvAmount, tvTime, tvDesc;
            ImageView ivAvatar;
            ItemVH(View v) {
                super(v);
                tvAvatar    = v.findViewById(R.id.tv_th_avatar);
                ivAvatar    = v.findViewById(R.id.iv_th_avatar);
                tvName      = v.findViewById(R.id.tv_th_name);
                tvDirection = v.findViewById(R.id.tv_th_direction);
                tvAmount    = v.findViewById(R.id.tv_th_amount);
                tvTime      = v.findViewById(R.id.tv_th_time);
                tvDesc      = v.findViewById(R.id.tv_th_desc);
            }
        }
    }
}
