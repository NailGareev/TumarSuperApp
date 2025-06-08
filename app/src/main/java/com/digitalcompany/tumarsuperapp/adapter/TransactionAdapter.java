package com.digitalcompany.tumarsuperapp.adapter; // Создайте пакет adapter, если нужно

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color; // Пример использования стандартных цветов
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Для получения цветов из ресурсов
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
            boolean isIncoming = transaction.getRecipientId() == currentUserId;
            boolean isOutgoing = transaction.getSenderId() == currentUserId;

            // 1. Устанавливаем иконку и описание
            String description;
            if (isIncoming) {
                ivIcon.setImageResource(R.drawable.ic_arrow_downward); // Иконка входящего
                String senderName = formatName(transaction.getSenderFirstName(), transaction.getSenderLastName());
                description = "Перевод от " + (senderName != null ? senderName : transaction.getSenderPhone());
            } else if (isOutgoing) {
                ivIcon.setImageResource(R.drawable.ic_arrow_upward); // Иконка исходящего
                String recipientName = formatName(transaction.getRecipientFirstName(), transaction.getRecipientLastName());
                description = "Перевод " + (recipientName != null ? recipientName : transaction.getRecipientPhone());
            } else {
                // Другие типы транзакций (пополнение, оплата) - пока просто иконка по умолчанию
                ivIcon.setImageResource(R.drawable.ic_history); // Пример иконки по умолчанию
                description = transaction.getTransactionType(); // Отображаем тип
            }
            tvDescription.setText(description);

            // 2. Форматируем и устанавливаем сумму и цвет
            BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
            String currencyCode = transaction.getCurrency() != null ? transaction.getCurrency().toUpperCase() : "KZT";
            String formattedAmount;
            int amountColor;

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ")); // Используем локаль
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
                // Запасной вариант
                formattedAmount = String.format(Locale.US, "%.2f %s", amount, currencyCode);
            }


            if (isIncoming) {
                formattedAmount = "+" + formattedAmount;
                // Используем цвет из ресурсов, если определен, иначе стандартный зеленый
                amountColor = ContextCompat.getColor(context, R.color.green_success); // Замените на ваш цвет
            } else if (isOutgoing) {
                formattedAmount = "-" + formattedAmount;
                // Используем цвет из ресурсов, если определен, иначе стандартный красный/черный
                amountColor = ContextCompat.getColor(context, R.color.red_error); // Замените на ваш цвет
            } else {
                // Для других типов - цвет по умолчанию
                amountColor = ContextCompat.getColor(context, R.color.grey_text); // Замените на ваш цвет
            }
            tvAmount.setText(formattedAmount);
            tvAmount.setTextColor(amountColor);


            // 3. Форматируем и устанавливаем дату/время
            if (transaction.getTimestamp() != null) {
                // Пример форматирования: 5 мая 2025, 08:15
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
                // sdf.setTimeZone(TimeZone.getDefault()); // Можно установить часовой пояс
                tvTimestamp.setText(sdf.format(transaction.getTimestamp()));
            } else {
                tvTimestamp.setText(""); // Пусто, если даты нет
            }
        }

        // Вспомогательный метод для форматирования имени
        private String formatName(String firstName, String lastName) {
            if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
                // Возвращаем "Имя Ф." (первая буква фамилии)
                return firstName + " " + lastName.substring(0, 1).toUpperCase() + ".";
            } else if (firstName != null && !firstName.isEmpty()) {
                return firstName;
            } else if (lastName != null && !lastName.isEmpty()) {
                return lastName;
            }
            return null; // Возвращаем null, если имени/фамилии нет
        }
    }
}