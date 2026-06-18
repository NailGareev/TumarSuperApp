package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.Transaction;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import retrofit2.Response;

public class NotificationPollWorker extends Worker {

    public NotificationPollWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(NotificationHelper.KEY_ALL, true)) return Result.success();

        SharedPreferences userPrefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int myUserId = userPrefs.getInt("user_id", -1);
        if (myUserId < 0) return Result.success();

        try {
            ApiService api = ApiClient.getApiService(ctx);
            Response<TransactionHistoryResponse> resp = api.getTransactionHistory().execute();
            if (resp.isSuccessful() && resp.body() != null) {
                List<Transaction> txs = resp.body().getTransactions();
                if (txs != null && !txs.isEmpty()) {
                    long lastId = prefs.getLong(NotificationHelper.KEY_LAST_TX_ID, 0);
                    long maxId = lastId;
                    NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("ru", "KZ"));
                    fmt.setCurrency(Currency.getInstance("KZT"));
                    fmt.setMaximumFractionDigits(0);
                    for (Transaction t : txs) {
                        long txId = t.getId();
                        if (txId <= lastId) continue;
                        if (txId > maxId) maxId = txId;
                        boolean incoming = t.getRecipientId() == myUserId;
                        if (incoming && NotificationHelper.isEnabled(ctx, NotificationHelper.KEY_TR_IN)) {
                            String sender = t.getSenderFirstName() != null ? t.getSenderFirstName() : "Кто-то";
                            String amt = t.getAmount() != null ? fmt.format(t.getAmount()) : "";
                            NotificationHelper.showNotification(ctx, NotificationHelper.CH_TRANSFERS,
                                    "Входящий перевод +" + amt,
                                    sender + (t.getDescription() != null && !t.getDescription().isEmpty()
                                            ? ": «" + t.getDescription() + "»" : " отправил вам деньги"));
                        } else if (!incoming && NotificationHelper.isEnabled(ctx, NotificationHelper.KEY_TR_OUT)) {
                            String recipient = t.getRecipientFirstName() != null ? t.getRecipientFirstName() : "получателю";
                            String amt = t.getAmount() != null ? fmt.format(t.getAmount()) : "";
                            NotificationHelper.showNotification(ctx, NotificationHelper.CH_TRANSFERS,
                                    "Перевод отправлен -" + amt,
                                    "Вы перевели деньги: " + recipient);
                        }
                    }
                    if (maxId > lastId) prefs.edit().putLong(NotificationHelper.KEY_LAST_TX_ID, maxId).apply();
                }
            }
        } catch (Exception ignored) {}

        return Result.success();
    }
}
