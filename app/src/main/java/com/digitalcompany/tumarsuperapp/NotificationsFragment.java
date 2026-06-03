package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.MarketNotification;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationsFragment extends Fragment {

    private static final String MARKET_URL = "http://10.0.2.2:8080";
    private static final String APP_SECRET = "tumar_app_secret_2024";
    private static final String USER_PREFS = "UserPrefs";
    private static final String KEY_TOKEN  = "auth_token";
    private static final String KEY_USER_ID = "user_id";

    // Tab buttons
    private TextView tabTransfers;
    private TextView tabMarket;
    private TextView tabCredit;

    // Content panes
    private View contentTransfers;
    private View contentMarket;
    private View contentCredit;

    // Transfers tab
    private RecyclerView  rvTransfers;
    private ProgressBar   progressTransfers;
    private View          emptyTransfers;

    // Market tab
    private RecyclerView  rvMarket;
    private ProgressBar   progressMarket;
    private View          emptyMarket;

    // Credit tab
    private RecyclerView  rvCredit;
    private ProgressBar   progressCredit;
    private View          emptyCredit;

    private ApiService apiService;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int currentUserId = -1;

    // Which tab is active: 0=transfers, 1=market, 2=credit
    private int activeTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabTransfers    = view.findViewById(R.id.tab_transfers);
        tabMarket       = view.findViewById(R.id.tab_market);
        tabCredit       = view.findViewById(R.id.tab_credit);

        contentTransfers = view.findViewById(R.id.content_transfers);
        contentMarket    = view.findViewById(R.id.content_market);
        contentCredit    = view.findViewById(R.id.content_credit);

        rvTransfers       = view.findViewById(R.id.rv_transfer_notifs);
        progressTransfers = view.findViewById(R.id.progress_transfers);
        emptyTransfers    = view.findViewById(R.id.empty_transfers);

        rvMarket       = view.findViewById(R.id.rv_market_notifs);
        progressMarket = view.findViewById(R.id.progress_market);
        emptyMarket    = view.findViewById(R.id.empty_market);

        rvCredit       = view.findViewById(R.id.rv_credit_notifs);
        progressCredit = view.findViewById(R.id.progress_credit);
        emptyCredit    = view.findViewById(R.id.empty_credit);

        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
            SharedPreferences prefs = getActivity().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
            currentUserId = prefs.getInt(KEY_USER_ID, -1);
        }

        rvTransfers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMarket.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCredit.setLayoutManager(new LinearLayoutManager(getContext()));

        tabTransfers.setOnClickListener(v -> selectTab(0));
        tabMarket.setOnClickListener(v -> selectTab(1));
        tabCredit.setOnClickListener(v -> selectTab(2));

        selectTab(0);
    }

    private void selectTab(int tab) {
        activeTab = tab;

        // Reset all tabs to inactive style
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.card_bg);
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        tabTransfers.setBackgroundColor(inactiveColor);
        tabTransfers.setTextColor(inactiveTextColor);
        tabMarket.setBackgroundColor(inactiveColor);
        tabMarket.setTextColor(inactiveTextColor);
        tabCredit.setBackgroundColor(inactiveColor);
        tabCredit.setTextColor(inactiveTextColor);

        contentTransfers.setVisibility(View.GONE);
        contentMarket.setVisibility(View.GONE);
        contentCredit.setVisibility(View.GONE);

        // Activate selected tab
        switch (tab) {
            case 0:
                tabTransfers.setBackgroundResource(R.drawable.bg_chip_purple_active);
                tabTransfers.setTextColor(0xFFFFFFFF);
                contentTransfers.setVisibility(View.VISIBLE);
                loadTransfers();
                break;
            case 1:
                tabMarket.setBackgroundResource(R.drawable.bg_chip_purple_active);
                tabMarket.setTextColor(0xFFFFFFFF);
                contentMarket.setVisibility(View.VISIBLE);
                loadMarketNotifications();
                break;
            case 2:
                tabCredit.setBackgroundResource(R.drawable.bg_chip_purple_active);
                tabCredit.setTextColor(0xFFFFFFFF);
                contentCredit.setVisibility(View.VISIBLE);
                loadCreditNotifications();
                break;
        }
    }

    // ── Transfers ─────────────────────────────────────────────────────────────

    private void loadTransfers() {
        if (apiService == null) return;
        progressTransfers.setVisibility(View.VISIBLE);
        rvTransfers.setVisibility(View.GONE);
        emptyTransfers.setVisibility(View.GONE);

        apiService.getTransactionHistory().enqueue(new retrofit2.Callback<TransactionHistoryResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<TransactionHistoryResponse> call,
                                   @NonNull retrofit2.Response<TransactionHistoryResponse> response) {
                if (!isAdded() || getContext() == null) return;
                progressTransfers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Transaction> all = response.body().getTransactions();
                    List<Transaction> transfers = new ArrayList<>();
                    if (all != null) {
                        for (Transaction t : all) {
                            String type = t.getTransactionType();
                            if ("TRANSFER".equals(type)) transfers.add(t);
                        }
                    }
                    if (transfers.isEmpty()) {
                        emptyTransfers.setVisibility(View.VISIBLE);
                    } else {
                        rvTransfers.setVisibility(View.VISIBLE);
                        rvTransfers.setAdapter(new TransferNotifAdapter(transfers, currentUserId));
                    }
                } else {
                    emptyTransfers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<TransactionHistoryResponse> call,
                                  @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                progressTransfers.setVisibility(View.GONE);
                emptyTransfers.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── Market Notifications ──────────────────────────────────────────────────

    private void loadMarketNotifications() {
        if (!isAdded() || getContext() == null) return;
        progressMarket.setVisibility(View.VISIBLE);
        rvMarket.setVisibility(View.GONE);
        emptyMarket.setVisibility(View.GONE);

        if (apiService == null) { showEmptyMarket(); return; }

        SharedPreferences prefs = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        String appToken = prefs.getString(KEY_TOKEN, null);
        if (appToken == null) { showEmptyMarket(); return; }

        apiService.getUserProfile().enqueue(new retrofit2.Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<UserProfileResponse> call,
                                   @NonNull retrofit2.Response<UserProfileResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (!response.isSuccessful() || response.body() == null) { showEmptyMarket(); return; }
                String phone = response.body().getPhone();
                if (phone == null || phone.isEmpty()) { showEmptyMarket(); return; }
                autoLoginMarket(phone);
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<UserProfileResponse> call, @NonNull Throwable t) {
                showEmptyMarket();
            }
        });
    }

    private void autoLoginMarket(String phone) {
        try {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            body.put("app_secret", APP_SECRET);

            Request req = new Request.Builder()
                    .url(MARKET_URL + "/api/auth/app-auto-login")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                    if (!isAdded()) return;
                    String bodyStr = resp.body() != null ? resp.body().string() : "";
                    resp.close();
                    try {
                        JSONObject json = new JSONObject(bodyStr);
                        String token = json.optString("token", "");
                        if (!token.isEmpty()) {
                            fetchMarketNotifs(token);
                        } else {
                            showEmptyMarket();
                        }
                    } catch (Exception e) {
                        showEmptyMarket();
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    showEmptyMarket();
                }
            });
        } catch (Exception e) {
            showEmptyMarket();
        }
    }

    private void fetchMarketNotifs(String marketToken) {
        Request req = new Request.Builder()
                .url(MARKET_URL + "/api/notifications")
                .header("Authorization", "Bearer " + marketToken)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                if (!isAdded()) return;
                String bodyStr = resp.body() != null ? resp.body().string() : "[]";
                resp.close();

                List<MarketNotification> list;
                try {
                    list = new Gson().fromJson(bodyStr,
                            new TypeToken<List<MarketNotification>>(){}.getType());
                } catch (Exception e) {
                    list = new ArrayList<>();
                }

                final List<MarketNotification> finalList = list != null ? list : new ArrayList<>();
                mainHandler.post(() -> {
                    if (!isAdded() || getContext() == null) return;
                    progressMarket.setVisibility(View.GONE);
                    if (finalList.isEmpty()) {
                        emptyMarket.setVisibility(View.VISIBLE);
                    } else {
                        rvMarket.setVisibility(View.VISIBLE);
                        rvMarket.setAdapter(new MarketNotifAdapter(finalList));
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showEmptyMarket();
            }
        });
    }

    private void showEmptyMarket() {
        mainHandler.post(() -> {
            if (!isAdded() || getContext() == null) return;
            progressMarket.setVisibility(View.GONE);
            emptyMarket.setVisibility(View.VISIBLE);
        });
    }

    // ── Credit Notifications ──────────────────────────────────────────────────

    private void loadCreditNotifications() {
        if (!isAdded() || getContext() == null) return;
        progressCredit.setVisibility(View.GONE);
        rvCredit.setVisibility(View.GONE);
        emptyCredit.setVisibility(View.VISIBLE);
    }

    // ── TransferNotifAdapter ──────────────────────────────────────────────────

    static class TransferNotifAdapter extends RecyclerView.Adapter<TransferNotifAdapter.VH> {
        private final List<Transaction> items;
        private final int myUserId;

        TransferNotifAdapter(List<Transaction> items, int myUserId) {
            this.items    = items;
            this.myUserId = myUserId;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transfer_notif, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Transaction t = items.get(position);
            boolean isIncoming = t.getRecipientId() == myUserId;

            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);

            if (isIncoming) {
                h.icon.setText("📥");
                String from = buildName(t.getSenderFirstName(), t.getSenderLastName(), t.getSenderPhone());
                h.title.setText("Получен перевод от " + from);
                h.amount.setText("+" + fmt.format(t.getAmount().abs()));
                h.amount.setTextColor(0xFF2E7D32);
            } else {
                h.icon.setText("📤");
                String to = buildName(t.getRecipientFirstName(), t.getRecipientLastName(), t.getRecipientPhone());
                h.title.setText("Перевод → " + to);
                h.amount.setText("-" + fmt.format(t.getAmount().abs()));
                h.amount.setTextColor(0xFFD32F2F);
            }

            String desc = t.getDescription();
            if (desc != null && !desc.isEmpty()) {
                h.desc.setVisibility(View.VISIBLE);
                h.desc.setText(desc);
            } else {
                h.desc.setVisibility(View.GONE);
            }

            h.time.setText(formatDate(t.getTimestamp()));
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String buildName(String first, String last, String phone) {
            if (first != null && !first.isEmpty()) {
                String name = first;
                if (last != null && !last.isEmpty()) name += " " + last;
                return name;
            }
            return phone != null ? phone : "Неизвестно";
        }

        private String formatDate(Date d) {
            if (d == null) return "";
            return new SimpleDateFormat("dd.MM.yyyy  HH:mm", new Locale("ru")).format(d);
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView icon, title, desc, time, amount;
            VH(View v) {
                super(v);
                icon   = v.findViewById(R.id.tv_transfer_icon);
                title  = v.findViewById(R.id.tv_transfer_title);
                desc   = v.findViewById(R.id.tv_transfer_desc);
                time   = v.findViewById(R.id.tv_transfer_time);
                amount = v.findViewById(R.id.tv_transfer_amount);
            }
        }
    }

    // ── MarketNotifAdapter ────────────────────────────────────────────────────

    static class MarketNotifAdapter extends RecyclerView.Adapter<MarketNotifAdapter.VH> {
        private final List<MarketNotification> items;

        MarketNotifAdapter(List<MarketNotification> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_market_notif, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MarketNotification n = items.get(position);
            h.title.setText(n.title);
            h.message.setText(n.message);
            h.time.setText(formatDate(n.createdAt));
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String formatDate(String raw) {
            if (raw == null) return "";
            try {
                SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                SimpleDateFormat out = new SimpleDateFormat("dd.MM.yyyy  HH:mm", new Locale("ru"));
                Date d = in.parse(raw);
                return d != null ? out.format(d) : raw.substring(0, Math.min(16, raw.length()));
            } catch (Exception e) {
                return raw.length() >= 10 ? raw.substring(0, 10) : raw;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, message, time;
            VH(View v) {
                super(v);
                title   = v.findViewById(R.id.tv_market_notif_title);
                message = v.findViewById(R.id.tv_market_notif_message);
                time    = v.findViewById(R.id.tv_market_notif_time);
            }
        }
    }
}
