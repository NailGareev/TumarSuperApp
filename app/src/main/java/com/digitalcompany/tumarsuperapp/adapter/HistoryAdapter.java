package com.digitalcompany.tumarsuperapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
    private static final int TYPE_TRANSACTION = 1;

    private List<Object> items = new ArrayList<>();
    private final int currentUserId;

    public HistoryAdapter(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setItems(List<Object> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_DATE_HEADER : TYPE_TRANSACTION;
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
            ((DateHeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof TransactionViewHolder) {
            ((TransactionViewHolder) holder).bind((Transaction) items.get(position), currentUserId);
        }
    }

    // --- Date Header ViewHolder ---

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDateHeader;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tv_date_header);
        }

        void bind(String label) {
            tvDateHeader.setText(label);
        }
    }

    // --- Transaction ViewHolder (copied bind logic from TransactionAdapter) ---

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvDescription;
        private final TextView tvTimestamp;
        private final TextView tvAmount;
        private final Context context;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            ivIcon = itemView.findViewById(R.id.iv_transaction_icon);
            tvDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvTimestamp = itemView.findViewById(R.id.tv_transaction_timestamp);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
        }

        void bind(Transaction transaction, int currentUserId) {
            String type = transaction.getTransactionType();
            boolean isIncoming = transaction.getRecipientId() == currentUserId;

            String description;
            String amountPrefix;
            int amountColor;

            // Reset card background
            if (itemView instanceof CardView) {
                ((CardView) itemView).setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.card_bg));
            }

            if ("MARKET_REFUND".equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_payment);
                description = "Возврат — Tumar Market";
                amountPrefix = "+";
                amountColor = ContextCompat.getColor(context, R.color.red_error);
                if (itemView instanceof CardView) {
                    ((CardView) itemView).setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                }
                tvTimestamp.setTextColor(ContextCompat.getColor(context, R.color.red_error));

            } else if ("PAYMENT".equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_payment);
                String raw = transaction.getDescription();
                description = (raw != null && !raw.isEmpty()) ? raw : "Оплата услуг";
                amountPrefix = "-";
                amountColor = ContextCompat.getColor(context, R.color.red_error);

            } else if ("TOPUP".equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_add_circle_outline);
                description = "Пополнение баланса";
                amountPrefix = "+";
                amountColor = ContextCompat.getColor(context, R.color.green_success);

            } else {
                // TRANSFER — incoming or outgoing
                if (isIncoming) {
                    ivIcon.setImageResource(R.drawable.ic_arrow_downward);
                    String name = formatName(transaction.getSenderFirstName(), transaction.getSenderLastName());
                    description = "Перевод от " + (name != null ? name : nvl(transaction.getSenderPhone(), "—"));
                    amountPrefix = "+";
                    amountColor = ContextCompat.getColor(context, R.color.green_success);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_arrow_upward);
                    String name = formatName(transaction.getRecipientFirstName(), transaction.getRecipientLastName());
                    description = "Перевод " + (name != null ? name : nvl(transaction.getRecipientPhone(), "—"));
                    amountPrefix = "-";
                    amountColor = ContextCompat.getColor(context, R.color.red_error);
                }
            }

            tvDescription.setText(description);

            // Format amount
            BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
            String currencyCode = transaction.getCurrency() != null ? transaction.getCurrency().toUpperCase() : "KZT";
            String formattedAmount;

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
            try {
                currencyFormat.setCurrency(Currency.getInstance(currencyCode));
                if ("KZT".equals(currencyCode)) {
                    currencyFormat.setMaximumFractionDigits(0);
                    currencyFormat.setMinimumFractionDigits(0);
                } else {
                    currencyFormat.setMaximumFractionDigits(2);
                    currencyFormat.setMinimumFractionDigits(2);
                }
                formattedAmount = currencyFormat.format(amount);
            } catch (Exception e) {
                formattedAmount = String.format(Locale.US, "%.2f %s", amount, currencyCode);
            }

            tvAmount.setText(amountPrefix + formattedAmount);
            tvAmount.setTextColor(amountColor);

            // Format timestamp
            TimeZone tz = TimeZone.getTimeZone("Asia/Almaty");
            if ("MARKET_REFUND".equals(type)) {
                String timeStr = "";
                if (transaction.getTimestamp() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"));
                    sdf.setTimeZone(tz);
                    timeStr = sdf.format(transaction.getTimestamp());
                }
                tvTimestamp.setText("Возвращено" + (timeStr.isEmpty() ? "" : " • " + timeStr));
            } else if (transaction.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
                sdf.setTimeZone(tz);
                tvTimestamp.setText(sdf.format(transaction.getTimestamp()));
                tvTimestamp.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                tvTimestamp.setText("");
                tvTimestamp.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        private String formatName(String firstName, String lastName) {
            if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
                return firstName + " " + lastName.substring(0, 1).toUpperCase() + ".";
            } else if (firstName != null && !firstName.isEmpty()) {
                return firstName;
            } else if (lastName != null && !lastName.isEmpty()) {
                return lastName;
            }
            return null;
        }

        private String nvl(String value, String fallback) {
            return (value != null && !value.isEmpty()) ? value : fallback;
        }
    }
}
