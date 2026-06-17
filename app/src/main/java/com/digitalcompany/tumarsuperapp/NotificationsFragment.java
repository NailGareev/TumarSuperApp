package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.ChatConversation;
import com.digitalcompany.tumarsuperapp.network.models.ChatConversationsResponse;
import com.digitalcompany.tumarsuperapp.network.models.MarketNotification;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

    // Tab containers
    private LinearLayout tabTransfers, tabMarket, tabCredit;
    private View         indicatorTransfers, indicatorMarket, indicatorCredit;
    private TextView     tabTextTransfers, tabTextMarket, tabTextCredit;
    private TextView     tabBadgeTransfers, tabBadgeMarket;

    // Content panes
    private View contentTransfers, contentMarket, contentCredit;

    // Transfers tab
    private RecyclerView rvTransfers;
    private ProgressBar  progressTransfers;
    private View         emptyTransfers;

    // Market tab
    private RecyclerView rvMarket;
    private ProgressBar  progressMarket;
    private View         emptyMarket;

    private ApiService  apiService;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int currentUserId = -1;
    private int activeTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Back button
        view.findViewById(R.id.btn_notifications_back).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // Tabs
        tabTransfers = view.findViewById(R.id.tab_transfers);
        tabMarket    = view.findViewById(R.id.tab_market);
        tabCredit    = view.findViewById(R.id.tab_credit);

        indicatorTransfers = view.findViewById(R.id.tab_indicator_transfers);
        indicatorMarket    = view.findViewById(R.id.tab_indicator_market);
        indicatorCredit    = view.findViewById(R.id.tab_indicator_credit);

        tabTextTransfers = view.findViewById(R.id.tab_text_transfers);
        tabTextMarket    = view.findViewById(R.id.tab_text_market);
        tabTextCredit    = view.findViewById(R.id.tab_text_credit);

        tabBadgeTransfers = view.findViewById(R.id.tab_badge_transfers);
        tabBadgeMarket    = view.findViewById(R.id.tab_badge_market);

        // Content
        contentTransfers = view.findViewById(R.id.content_transfers);
        contentMarket    = view.findViewById(R.id.content_market);
        contentCredit    = view.findViewById(R.id.content_credit);

        rvTransfers       = view.findViewById(R.id.rv_transfer_notifs);
        progressTransfers = view.findViewById(R.id.progress_transfers);
        emptyTransfers    = view.findViewById(R.id.empty_transfers);

        rvMarket       = view.findViewById(R.id.rv_market_notifs);
        progressMarket = view.findViewById(R.id.progress_market);
        emptyMarket    = view.findViewById(R.id.empty_market);

        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
            SharedPreferences prefs = getActivity().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
            currentUserId = prefs.getInt(KEY_USER_ID, -1);
        }

        rvTransfers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMarket.setLayoutManager(new LinearLayoutManager(getContext()));

        DividerItemDecoration div = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        rvTransfers.addItemDecoration(div);
        rvMarket.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        tabTransfers.setOnClickListener(v -> selectTab(0));
        tabMarket.setOnClickListener(v -> selectTab(1));
        tabCredit.setOnClickListener(v -> selectTab(2));

        selectTab(0);
    }

    private void selectTab(int tab) {
        activeTab = tab;
        int purple = 0xFF6200EE;
        int grey   = 0xFF9E9E9E;
        int transparentColor = Color.TRANSPARENT;

        // Reset all
        tabTextTransfers.setTextColor(grey);
        tabTextMarket.setTextColor(grey);
        tabTextCredit.setTextColor(grey);
        indicatorTransfers.setBackgroundColor(transparentColor);
        indicatorMarket.setBackgroundColor(transparentColor);
        indicatorCredit.setBackgroundColor(transparentColor);

        contentTransfers.setVisibility(View.GONE);
        contentMarket.setVisibility(View.GONE);
        contentCredit.setVisibility(View.GONE);

        switch (tab) {
            case 0:
                tabTextTransfers.setTextColor(purple);
                indicatorTransfers.setBackgroundColor(purple);
                contentTransfers.setVisibility(View.VISIBLE);
                loadChatConversations();
                break;
            case 1:
                tabTextMarket.setTextColor(purple);
                indicatorMarket.setBackgroundColor(purple);
                contentMarket.setVisibility(View.VISIBLE);
                loadMarketNotifications();
                break;
            case 2:
                tabTextCredit.setTextColor(purple);
                indicatorCredit.setBackgroundColor(purple);
                contentCredit.setVisibility(View.VISIBLE);
                break;
        }
    }

    // ── Transfers (messenger chat list) ───────────────────────────────────────

    private void loadChatConversations() {
        if (apiService == null) return;
        progressTransfers.setVisibility(View.VISIBLE);
        rvTransfers.setVisibility(View.GONE);
        emptyTransfers.setVisibility(View.GONE);

        apiService.getChatConversations().enqueue(new retrofit2.Callback<ChatConversationsResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ChatConversationsResponse> call,
                                   @NonNull retrofit2.Response<ChatConversationsResponse> response) {
                if (!isAdded() || getContext() == null) return;
                progressTransfers.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<ChatConversation> list = response.body().conversations;
                    if (list == null || list.isEmpty()) {
                        emptyTransfers.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Show unread badge on tab
                    int totalUnread = 0;
                    for (ChatConversation c : list) totalUnread += c.unreadCount;
                    if (totalUnread > 0) {
                        tabBadgeTransfers.setText(totalUnread > 9 ? "9+" : String.valueOf(totalUnread));
                        tabBadgeTransfers.setVisibility(View.VISIBLE);
                    }

                    rvTransfers.setVisibility(View.VISIBLE);
                    ChatConvAdapter adapter = new ChatConvAdapter(list, NotificationsFragment.this);
                    rvTransfers.setAdapter(adapter);
                } else {
                    emptyTransfers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ChatConversationsResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                progressTransfers.setVisibility(View.GONE);
                emptyTransfers.setVisibility(View.VISIBLE);
            }
        });
    }

    void openChat(ChatConversation conv) {
        TransferChatFragment chat = TransferChatFragment.newInstance(
                conv.otherUserId,
                conv.otherFirstName != null ? conv.otherFirstName : "",
                conv.otherLastName  != null ? conv.otherLastName  : "");
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chat)
                .addToBackStack("transfer_chat")
                .commit();
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
                        if (!token.isEmpty()) fetchMarketNotifs(token);
                        else showEmptyMarket();
                    } catch (Exception e) { showEmptyMarket(); }
                }
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) { showEmptyMarket(); }
            });
        } catch (Exception e) { showEmptyMarket(); }
    }

    private void fetchMarketNotifs(String marketToken) {
        Request req = new Request.Builder()
                .url(MARKET_URL + "/api/notifications")
                .header("Authorization", "Bearer " + marketToken)
                .get().build();
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
                } catch (Exception e) { list = new ArrayList<>(); }
                final List<MarketNotification> finalList = list != null ? list : new ArrayList<>();
                mainHandler.post(() -> {
                    if (!isAdded() || getContext() == null) return;
                    progressMarket.setVisibility(View.GONE);

                    // Tab badge for market
                    int unread = 0;
                    for (MarketNotification mn : finalList) { if (!mn.isRead) unread++; }
                    if (unread > 0) {
                        tabBadgeMarket.setText(unread > 9 ? "9+" : String.valueOf(unread));
                        tabBadgeMarket.setVisibility(View.VISIBLE);
                    }

                    if (finalList.isEmpty()) {
                        emptyMarket.setVisibility(View.VISIBLE);
                    } else {
                        rvMarket.setVisibility(View.VISIBLE);
                        rvMarket.setAdapter(new MarketNotifAdapter(finalList));
                    }
                });
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { showEmptyMarket(); }
        });
    }

    private void showEmptyMarket() {
        mainHandler.post(() -> {
            if (!isAdded() || getContext() == null) return;
            progressMarket.setVisibility(View.GONE);
            emptyMarket.setVisibility(View.VISIBLE);
        });
    }

    // ── ChatConvAdapter (messenger-style list) ────────────────────────────────

    static class ChatConvAdapter extends RecyclerView.Adapter<ChatConvAdapter.VH> {
        private static final int[] AVATAR_COLORS = {
            0xFF6B21A8, 0xFF1A4A8A, 0xFF1A8A4A, 0xFFC9A227, 0xFFD97222
        };
        private final List<ChatConversation>  items;
        private final NotificationsFragment   fragment;
        private final NumberFormat            fmt;

        ChatConvAdapter(List<ChatConversation> items, NotificationsFragment fragment) {
            this.items    = items;
            this.fragment = fragment;
            fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transfer_notif, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ChatConversation c = items.get(position);

            // Name
            String name = buildName(c.otherFirstName, c.otherLastName, c.otherPhone);
            h.tvName.setText(name);

            // Avatar
            String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            h.tvAvatar.setText(initial);
            tintAvatar(h.tvAvatar, c.otherUserId);

            // Last message preview
            if (c.lastMessage != null && !c.lastMessage.isEmpty()) {
                h.tvLastMsg.setText(c.lastMessage);
            } else if (c.lastAmount != null) {
                // transfer without comment
                h.tvLastMsg.setText(c.isIncoming ? "Входящий перевод" : "Исходящий перевод");
            } else {
                h.tvLastMsg.setText("");
            }

            // Amount
            if (c.lastAmount != null) {
                h.tvAmount.setVisibility(View.VISIBLE);
                String amtStr = c.isIncoming
                        ? ("+" + fmt.format(c.lastAmount))
                        : ("-" + fmt.format(c.lastAmount));
                h.tvAmount.setText(amtStr);
                h.tvAmount.setTextColor(c.isIncoming ? 0xFF2E7D32 : 0xFFC62828);
            } else {
                h.tvAmount.setVisibility(View.GONE);
            }

            // Unread badge
            if (c.unreadCount > 0) {
                h.tvUnread.setVisibility(View.VISIBLE);
                h.tvUnread.setText(c.unreadCount > 9 ? "9+" : String.valueOf(c.unreadCount));
            } else {
                h.tvUnread.setVisibility(View.GONE);
            }

            // Time
            h.tvTime.setText(formatRelativeTime(c.lastTime));

            // Click → open chat
            h.itemView.setOnClickListener(v -> fragment.openChat(c));
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String buildName(String first, String last, String phone) {
            if (first != null && !first.isEmpty()) {
                return (last != null && !last.isEmpty()) ? first + " " + last : first;
            }
            return phone != null ? phone : "Неизвестно";
        }

        private String formatRelativeTime(String raw) {
            if (raw == null || raw.isEmpty()) return "";
            try {
                SimpleDateFormat[] fmts = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                };
                Date d = null;
                for (SimpleDateFormat sdf : fmts) {
                    try { d = sdf.parse(raw); if (d != null) break; } catch (Exception ignored) {}
                }
                if (d == null) return raw.length() >= 10 ? raw.substring(0, 10) : raw;

                Calendar now  = Calendar.getInstance();
                Calendar then = Calendar.getInstance();
                then.setTime(d);

                if (now.get(Calendar.DATE) == then.get(Calendar.DATE) &&
                    now.get(Calendar.MONTH) == then.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
                    return new SimpleDateFormat("HH:mm", new Locale("ru")).format(d);
                }
                now.add(Calendar.DATE, -1);
                if (now.get(Calendar.DATE) == then.get(Calendar.DATE) &&
                    now.get(Calendar.MONTH) == then.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
                    return "Вчера";
                }
                return new SimpleDateFormat("d MMM", new Locale("ru")).format(d);
            } catch (Exception e) {
                return raw.length() >= 10 ? raw.substring(0, 10) : raw;
            }
        }

        static void tintAvatar(TextView tv, int userId) {
            int color = AVATAR_COLORS[Math.abs(userId) % AVATAR_COLORS.length];
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            tv.setBackground(gd);
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvLastMsg, tvTime, tvAmount, tvUnread;
            VH(View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tv_avatar_initial);
                tvName    = v.findViewById(R.id.tv_chat_name);
                tvLastMsg = v.findViewById(R.id.tv_chat_last_msg);
                tvTime    = v.findViewById(R.id.tv_chat_time);
                tvAmount  = v.findViewById(R.id.tv_chat_amount);
                tvUnread  = v.findViewById(R.id.tv_chat_unread);
            }
        }
    }

    // ── MarketNotifAdapter (redesigned) ───────────────────────────────────────

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

            if (!n.isRead) {
                h.unread.setVisibility(View.VISIBLE);
            } else {
                h.unread.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String formatDate(String raw) {
            if (raw == null) return "";
            try {
                SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                SimpleDateFormat out = new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"));
                Date d = in.parse(raw);
                return d != null ? out.format(d) : raw.substring(0, Math.min(16, raw.length()));
            } catch (Exception e) {
                return raw.length() >= 10 ? raw.substring(0, 10) : raw;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, message, time, unread;
            VH(View v) {
                super(v);
                title   = v.findViewById(R.id.tv_market_notif_title);
                message = v.findViewById(R.id.tv_market_notif_message);
                time    = v.findViewById(R.id.tv_market_notif_time);
                unread  = v.findViewById(R.id.tv_market_notif_unread);
            }
        }
    }
}
