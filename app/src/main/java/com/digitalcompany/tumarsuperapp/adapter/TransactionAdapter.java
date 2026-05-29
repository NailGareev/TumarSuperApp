package com.digitalcompany.tumarsuperapp.adapter; // Создайте пакет adapter, если нужно

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.R; // Убедитесь, что R импортирован правильно
import com.digitalcompany.tumarsuperapp.network.models.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone; // Для форматирования времени

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    private final int currentUserId; // ID текущего пользователя для определения типа транзакции

    // Ключи SharedPreferences для получения ID пользователя
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";

    public TransactionAdapter(@NonNull Context context) {
        super(DIFF_CALLBACK);
        // Получаем ID текущего пользователя при создании адаптера
        SharedPreferences prefs = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        this.currentUserId = prefs.getInt(KEY_USER_ID, -1); // -1 как индикатор ошибки
    }

    // DiffUtil для эффективного обновления списка
    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getId() == newItem.getId(); // Сравниваем по уникальному ID
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            // Сравниваем содержимое, если ID одинаковые
            // Можно добавить больше полей, если нужно точнее определять изменения
            return oldItem.getAmount().equals(newItem.getAmount()) &&
                    oldItem.getTimestamp().equals(newItem.getTimestamp());
        }
    };

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction currentTransaction = getItem(position);
        holder.bind(currentTransaction, currentUserId);
    }

    // ViewHolder для хранения ссылок на View элемента списка
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvDescription;
        private final TextView tvTimestamp;
        private final TextView tvAmount;
        private final Context context; // Для доступа к ресурсам (цветам, строкам)

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext(); // Получаем контекст из View
            ivIcon = itemView.findViewById(R.id.iv_transaction_icon);
            tvDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvTimestamp = itemView.findViewById(R.id.tv_transaction_timestamp);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
        }

        // Метод для заполнения View данными транзакции
        public void bind(Transaction transaction, int currentUserId) {
            String type = transaction.getTransactionType();
            boolean isIncoming = transaction.getRecipientId() == currentUserId;
            boolean isOutgoing = transaction.getSenderId() == currentUserId;

            // 1. Иконка, описание и знак суммы определяются по типу транзакции
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
                // Highlight the whole card red
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
                // TRANSFER — входящий или исходящий
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

            // 2. Форматируем сумму
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


            // 3. Форматируем и устанавливаем дату/время
            if ("MARKET_REFUND".equals(type)) {
                String timeStr = "";
                if (transaction.getTimestamp() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"));
                    timeStr = sdf.format(transaction.getTimestamp());
                }
                tvTimestamp.setText("Возвращено" + (timeStr.isEmpty() ? "" : " • " + timeStr));
            } else if (transaction.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
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