package com.digitalcompany.tumarsuperapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.R;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_TRANSACTION  = 1;

    public static class DateGroup {
        public final String label;
        public final BigDecimal totalExpense;

        public DateGroup(String label, BigDecimal totalExpense) {
            this.label = label;
            this.totalExpense = totalExpense;
        }
    }

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction tx);
    }

    private List<Object> items = new ArrayList<>();
    private final int currentUserId;
    private OnTransactionClickListener clickListener;

    public HistoryAdapter(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.clickListener = listener;
    }

    public void setItems(List<Object> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof DateGroup) ? TYPE_DATE_HEADER : TYPE_TRANSACTION;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_history_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind((DateGroup) items.get(position));
        } else if (holder instanceof TransactionViewHolder) {
            boolean prevIsHeader = position == 0 || items.get(position - 1) instanceof DateGroup;
            boolean nextIsHeader = position == items.size() - 1
                    || items.get(position + 1) instanceof DateGroup;
            Transaction tx = (Transaction) items.get(position);
            ((TransactionViewHolder) holder).bind(tx, currentUserId, prevIsHeader, nextIsHeader);
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onTransactionClick(tx);
            });
        }
    }

    // --- Date Header ViewHolder ---

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDateHeader;
        private final TextView tvDateTotal;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tv_date_header);
            tvDateTotal  = itemView.findViewById(R.id.tv_date_total);
        }

        void bind(DateGroup group) {
            tvDateHeader.setText(group.label);
            if (group.totalExpense != null && group.totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
                fmt.setGroupingUsed(true);
                fmt.setMaximumFractionDigits(0);
                tvDateTotal.setText("-" + fmt.format(group.totalExpense.longValue()) + " ₸");
                tvDateTotal.setVisibility(View.VISIBLE);
            } else {
                tvDateTotal.setVisibility(View.GONE);
            }
        }
    }

    // --- Transaction ViewHolder ---

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout flIconBg;
        private final ImageView ivIcon;
        private final TextView tvDescription;
        private final TextView tvTimestamp;
        private final TextView tvAmount;
        private final TextView tvBadge;
        private final View viewDivider;
        private final Context context;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            context     = itemView.getContext();
            flIconBg    = itemView.findViewById(R.id.fl_tx_icon_bg);
            ivIcon      = itemView.findViewById(R.id.iv_transaction_icon);
            tvDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvTimestamp   = itemView.findViewById(R.id.tv_transaction_timestamp);
            tvAmount      = itemView.findViewById(R.id.tv_transaction_amount);
            tvBadge       = itemView.findViewById(R.id.tv_transaction_badge);
            viewDivider   = itemView.findViewById(R.id.view_item_divider);
        }

        void bind(Transaction tx, int currentUserId, boolean prevIsHeader, boolean nextIsHeader) {
            String type = tx.getTransactionType();
            boolean isIncoming = tx.getRecipientId() == currentUserId;

            String description;
            String amountPrefix;
            int amountColor;
            int iconRes;
            int iconBgRes;
            int iconTintColor;
            String badgeText;
            int badgeBgRes;
            int badgeTextColor;

            if ("MARKET_REFUND".equals(type)) {
                iconRes       = R.drawable.ic_payment;
                iconBgRes     = R.drawable.bg_icon_circle_red;
                iconTintColor = 0xFFCC2222;
                description   = "Возврат — Tumar Market";
                amountPrefix  = "+";
                amountColor   = ContextCompat.getColor(context, R.color.green_success);
                badgeText     = "Market";
                badgeBgRes    = R.drawable.bg_icon_circle_purple;
                badgeTextColor = 0xFF6B21A8;

            } else if ("PAYMENT".equals(type)) {
                iconRes       = R.drawable.ic_payment;
                iconBgRes     = R.drawable.bg_icon_circle_orange;
                iconTintColor = 0xFFD97222;
                String raw    = tx.getDescription();
                description   = (raw != null && !raw.isEmpty()) ? raw : "Оплата услуг";
                amountPrefix  = "-";
                amountColor   = ContextCompat.getColor(context, R.color.expense_color);
                badgeText     = "Оплата";
                badgeBgRes    = R.drawable.bg_icon_circle_orange;
                badgeTextColor = 0xFFD97222;

            } else if ("TOPUP".equals(type)) {
                iconRes       = R.drawable.ic_add_circle_outline;
                iconBgRes     = R.drawable.bg_icon_circle_green;
                iconTintColor = 0xFF1A8A4A;
                description   = "Пополнение баланса";
                amountPrefix  = "+";
                amountColor   = ContextCompat.getColor(context, R.color.income_color);
                badgeText     = "Доход";
                badgeBgRes    = R.drawable.bg_icon_circle_green;
                badgeTextColor = 0xFF1A8A4A;

            } else {
                // TRANSFER
                if (isIncoming) {
                    iconRes       = R.drawable.ic_arrow_downward;
                    iconBgRes     = R.drawable.bg_icon_circle_green;
                    iconTintColor = 0xFF1A8A4A;
                    String name   = formatName(tx.getSenderFirstName(), tx.getSenderLastName());
                    description   = "Перевод от " + (name != null ? name : nvl(tx.getSenderPhone(), "—"));
                    amountPrefix  = "+";
                    amountColor   = ContextCompat.getColor(context, R.color.income_color);
                    badgeText     = "Доход";
                    badgeBgRes    = R.drawable.bg_icon_circle_green;
                    badgeTextColor = 0xFF1A8A4A;
                } else {
                    iconRes       = R.drawable.ic_arrow_upward;
                    iconBgRes     = R.drawable.bg_icon_circle_blue;
                    iconTintColor = 0xFF1A4A8A;
                    String name   = formatName(tx.getRecipientFirstName(), tx.getRecipientLastName());
                    description   = "Перевод " + (name != null ? name : nvl(tx.getRecipientPhone(), "—"));
                    amountPrefix  = "-";
                    amountColor   = ContextCompat.getColor(context, R.color.expense_color);
                    badgeText     = "Перевод";
                    badgeBgRes    = R.drawable.bg_icon_circle_blue;
                    badgeTextColor = 0xFF1A4A8A;
                }
            }

            flIconBg.setBackgroundResource(iconBgRes);
            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(iconTintColor, android.graphics.PorterDuff.Mode.SRC_IN);
            tvDescription.setText(description);
            tvBadge.setText(badgeText);
            tvBadge.setBackgroundResource(badgeBgRes);
            tvBadge.setTextColor(badgeTextColor);

            // Format amount
            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            String currencyCode = tx.getCurrency() != null ? tx.getCurrency().toUpperCase() : "KZT";
            String formattedAmount;
            try {
                NumberFormat fmt = NumberFormat.getInstance(new Locale("ru"));
                fmt.setGroupingUsed(true);
                if ("KZT".equals(currencyCode)) {
                    fmt.setMaximumFractionDigits(0);
                    fmt.setMinimumFractionDigits(0);
                    formattedAmount = fmt.format(amount.abs()) + " ₸";
                } else {
                    fmt.setMaximumFractionDigits(2);
                    fmt.setMinimumFractionDigits(2);
                    formattedAmount = fmt.format(amount.abs()) + " " + currencyCode;
                }
            } catch (Exception e) {
                formattedAmount = amount.abs().toPlainString() + " ₸";
            }

            tvAmount.setText(amountPrefix + formattedAmount);
            tvAmount.setTextColor(amountColor);

            // Format timestamp — compact: "d MMM, HH:mm"
            TimeZone tz = TimeZone.getTimeZone("Asia/Almaty");
            if (tx.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"));
                sdf.setTimeZone(tz);
                tvTimestamp.setText(sdf.format(tx.getTimestamp()));
            } else {
                tvTimestamp.setText("");
            }
            tvTimestamp.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

            // Group card background based on position in group
            int bgRes;
            if (prevIsHeader && nextIsHeader) bgRes = R.drawable.bg_history_item_single;
            else if (prevIsHeader)            bgRes = R.drawable.bg_history_item_top;
            else if (nextIsHeader)            bgRes = R.drawable.bg_history_item_bottom;
            else                              bgRes = R.drawable.bg_history_item_middle;
            itemView.setBackgroundResource(bgRes);

            // Divider: show only for non-last items in group
            viewDivider.setVisibility(nextIsHeader ? View.GONE : View.VISIBLE);
        }

        private static String formatName(String firstName, String lastName) {
            if (firstName != null && !firstName.isEmpty()
                    && lastName != null && !lastName.isEmpty()) {
                return firstName + " " + lastName.substring(0, 1).toUpperCase() + ".";
            } else if (firstName != null && !firstName.isEmpty()) {
                return firstName;
            } else if (lastName != null && !lastName.isEmpty()) {
                return lastName;
            }
            return null;
        }

        private static String nvl(String value, String fallback) {
            return (value != null && !value.isEmpty()) ? value : fallback;
        }
    }
}
