package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.ChatItem;
import com.digitalcompany.tumarsuperapp.network.models.ChatMessagesResponse;
import com.digitalcompany.tumarsuperapp.network.models.SendMessageRequest;
import com.digitalcompany.tumarsuperapp.network.models.SendMessageResponse;

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

public class TransferChatFragment extends Fragment {

    private static final String ARG_OTHER_USER_ID   = "other_user_id";
    private static final String ARG_OTHER_FIRST_NAME = "other_first_name";
    private static final String ARG_OTHER_LAST_NAME  = "other_last_name";
    private static final String USER_PREFS           = "UserPrefs";
    private static final String KEY_USER_ID          = "user_id";

    private int    otherUserId;
    private String otherFirstName;
    private String otherLastName;
    private int    myUserId;

    private RecyclerView    rvMessages;
    private EditText        etInput;
    private TextView        btnSend;
    private ChatMsgAdapter  adapter;
    private List<ChatItem>  items = new ArrayList<>();
    private ApiService      api;

    // Avatar color palette
    private static final int[] AVATAR_COLORS = {
        0xFF6B21A8, 0xFF1A4A8A, 0xFF1A8A4A, 0xFFC9A227, 0xFFD97222
    };

    public static TransferChatFragment newInstance(int otherUserId, String firstName, String lastName) {
        TransferChatFragment f = new TransferChatFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_OTHER_USER_ID, otherUserId);
        args.putString(ARG_OTHER_FIRST_NAME, firstName);
        args.putString(ARG_OTHER_LAST_NAME, lastName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            otherUserId   = getArguments().getInt(ARG_OTHER_USER_ID);
            otherFirstName = getArguments().getString(ARG_OTHER_FIRST_NAME, "");
            otherLastName  = getArguments().getString(ARG_OTHER_LAST_NAME, "");
        }
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
            myUserId = prefs.getInt(KEY_USER_ID, -1);
            api = ApiClient.getApiService(getActivity().getApplicationContext());
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
        return inflater.inflate(R.layout.fragment_transfer_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMessages = view.findViewById(R.id.rv_chat_messages);
        etInput    = view.findViewById(R.id.et_chat_input);
        btnSend    = view.findViewById(R.id.btn_chat_send);

        // Header
        TextView tvName     = view.findViewById(R.id.tv_chat_header_name);
        TextView tvStatus   = view.findViewById(R.id.tv_chat_header_status);
        TextView tvAvatar   = view.findViewById(R.id.tv_chat_header_avatar);
        View btnBack    = view.findViewById(R.id.btn_chat_back);

        String displayName = buildName(otherFirstName, otherLastName);
        tvName.setText(displayName);
        tvStatus.setText("Чат по переводам");

        String initial = displayName.isEmpty() ? "?" : displayName.substring(0, 1).toUpperCase();
        tvAvatar.setText(initial);
        tintAvatar(tvAvatar, otherUserId);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // RecyclerView
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        adapter = new ChatMsgAdapter(items, myUserId, otherFirstName, otherUserId);
        rvMessages.setAdapter(adapter);

        // Send
        btnSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });

        loadMessages();
    }

    private void loadMessages() {
        if (api == null) return;
        api.getChatMessages(otherUserId).enqueue(new Callback<ChatMessagesResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessagesResponse> call,
                                   @NonNull Response<ChatMessagesResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<ChatItem> loaded = response.body().items;
                    items.clear();
                    if (loaded != null) items.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    if (!items.isEmpty()) rvMessages.scrollToPosition(items.size() - 1);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatMessagesResponse> call, @NonNull Throwable t) {}
        });
    }

    private void sendMessage() {
        if (api == null) return;
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");

        api.sendChatMessage(new SendMessageRequest(otherUserId, text))
           .enqueue(new Callback<SendMessageResponse>() {
               @Override
               public void onResponse(@NonNull Call<SendMessageResponse> call,
                                      @NonNull Response<SendMessageResponse> response) {
                   if (!isAdded() || getContext() == null) return;
                   if (response.isSuccessful() && response.body() != null && response.body().success) {
                       // Optimistically add message and reload
                       loadMessages();
                   } else {
                       Toast.makeText(getContext(), "Не удалось отправить", Toast.LENGTH_SHORT).show();
                   }
               }

               @Override
               public void onFailure(@NonNull Call<SendMessageResponse> call, @NonNull Throwable t) {
                   if (!isAdded() || getContext() == null) return;
                   Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
               }
           });
    }

    private String buildName(String first, String last) {
        if (first == null || first.isEmpty()) return last != null ? last : "";
        return last != null && !last.isEmpty() ? first + " " + last : first;
    }

    static void tintAvatar(TextView tv, int userId) {
        int color = AVATAR_COLORS[Math.abs(userId) % AVATAR_COLORS.length];
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        tv.setBackground(gd);
    }

    // ── Chat message adapter (handles TRANSFER cards + SMS bubbles) ───────────

    static class ChatMsgAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_TRANSFER_MINE  = 0;
        private static final int TYPE_TRANSFER_OTHER = 1;
        private static final int TYPE_SMS_MINE       = 2;
        private static final int TYPE_SMS_OTHER      = 3;

        private final List<ChatItem> items;
        private final int    myUserId;
        private final String otherName;
        private final int    otherUserId;

        private final NumberFormat fmt;

        ChatMsgAdapter(List<ChatItem> items, int myUserId, String otherName, int otherUserId) {
            this.items       = items;
            this.myUserId    = myUserId;
            this.otherName   = otherName;
            this.otherUserId = otherUserId;

            fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            fmt.setCurrency(Currency.getInstance("KZT"));
            fmt.setMaximumFractionDigits(0);
        }

        @Override
        public int getItemViewType(int position) {
            ChatItem item = items.get(position);
            boolean isMine = item.senderId == myUserId;
            if ("TRANSFER".equals(item.type)) {
                return isMine ? TYPE_TRANSFER_MINE : TYPE_TRANSFER_OTHER;
            } else {
                return isMine ? TYPE_SMS_MINE : TYPE_SMS_OTHER;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case TYPE_TRANSFER_MINE:
                case TYPE_TRANSFER_OTHER:
                    return new TransferVH(inf.inflate(R.layout.item_transfer_card, parent, false));
                case TYPE_SMS_MINE:
                    return new MsgVH(inf.inflate(R.layout.item_message_mine, parent, false));
                default:
                    return new MsgOtherVH(inf.inflate(R.layout.item_message_other, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatItem item = items.get(position);
            int type = getItemViewType(position);

            if (type == TYPE_TRANSFER_MINE || type == TYPE_TRANSFER_OTHER) {
                bindTransfer((TransferVH) holder, item, type == TYPE_TRANSFER_MINE);
            } else if (type == TYPE_SMS_MINE) {
                bindMsgMine((MsgVH) holder, item);
            } else {
                bindMsgOther((MsgOtherVH) holder, item);
            }
        }

        private void bindTransfer(TransferVH h, ChatItem item, boolean isMine) {
            LinearLayout container = h.itemView.findViewById(R.id.ll_transfer_card_container);

            // Align: mine = end, other = start
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) container.getLayoutParams();
            lp.gravity = isMine ? android.view.Gravity.END : android.view.Gravity.START;
            container.setLayoutParams(lp);

            if (isMine) {
                container.setBackgroundResource(R.drawable.bg_transfer_card_mine);
                h.tvTitle.setTextColor(Color.WHITE);
                h.tvTitle.setText("Перевод → " + otherName);
                if (item.amount != null) {
                    h.tvAmount.setText("- " + fmt.format(item.amount));
                    h.tvAmount.setTextColor(0xFFFFCDD2);
                }
            } else {
                container.setBackgroundResource(R.drawable.bg_transfer_card_other);
                h.tvTitle.setTextColor(0xFF212121);
                h.tvTitle.setText("Перевод от " + otherName);
                if (item.amount != null) {
                    h.tvAmount.setText("+ " + fmt.format(item.amount));
                    h.tvAmount.setTextColor(0xFF2E7D32);
                }
            }

            if (item.description != null && !item.description.isEmpty()) {
                h.tvDesc.setVisibility(View.VISIBLE);
                h.tvDesc.setText(item.description);
                h.tvDesc.setTextColor(isMine ? 0xCCFFFFFF : 0xFF757575);
            } else {
                h.tvDesc.setVisibility(View.GONE);
            }

            h.tvTime.setText(formatTime(item.timestamp));
            h.tvTime.setTextColor(isMine ? 0x66FFFFFF : 0xFFBDBDBD);
        }

        private void bindMsgMine(MsgVH h, ChatItem item) {
            h.tvText.setText(item.message);
            h.tvTime.setText(formatTime(item.timestamp));
        }

        private void bindMsgOther(MsgOtherVH h, ChatItem item) {
            h.tvText.setText(item.message);
            h.tvTime.setText(formatTime(item.timestamp));
            // Set avatar initial
            String initial = otherName.isEmpty() ? "?" : otherName.substring(0, 1).toUpperCase();
            h.tvAvatar.setText(initial);
            tintAvatar(h.tvAvatar, otherUserId);
        }

        private String formatTime(String raw) {
            if (raw == null || raw.isEmpty()) return "";
            try {
                SimpleDateFormat[] fmts = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                };
                for (SimpleDateFormat sdf : fmts) {
                    try {
                        Date d = sdf.parse(raw);
                        if (d != null) return new SimpleDateFormat("HH:mm", new Locale("ru")).format(d);
                    } catch (Exception ignored) {}
                }
                return raw.length() >= 16 ? raw.substring(11, 16) : raw;
            } catch (Exception e) { return ""; }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class TransferVH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAmount, tvDesc, tvTime;
            TransferVH(View v) {
                super(v);
                tvTitle  = v.findViewById(R.id.tv_card_title);
                tvAmount = v.findViewById(R.id.tv_card_amount);
                tvDesc   = v.findViewById(R.id.tv_card_desc);
                tvTime   = v.findViewById(R.id.tv_card_time);
            }
        }

        static class MsgVH extends RecyclerView.ViewHolder {
            TextView tvText, tvTime;
            MsgVH(View v) {
                super(v);
                tvText = v.findViewById(R.id.tv_msg_text);
                tvTime = v.findViewById(R.id.tv_msg_time);
            }
        }

        static class MsgOtherVH extends RecyclerView.ViewHolder {
            TextView tvText, tvTime, tvAvatar;
            MsgOtherVH(View v) {
                super(v);
                tvText   = v.findViewById(R.id.tv_msg_text);
                tvTime   = v.findViewById(R.id.tv_msg_time);
                tvAvatar = v.findViewById(R.id.tv_msg_avatar);
            }
        }
    }
}
