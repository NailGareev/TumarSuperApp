package com.digitalcompany.tumarsuperapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static final String PREFS_NAME = "NotifPrefs";
    public static final String KEY_ALL       = "all_enabled";
    public static final String KEY_TR_IN     = "transfer_in";
    public static final String KEY_TR_OUT    = "transfer_out";
    public static final String KEY_TR_CHAT   = "transfer_chat";
    public static final String KEY_MKT_ORDER = "market_order";
    public static final String KEY_MKT_CODE  = "market_code";
    public static final String KEY_MKT_PROMO = "market_promo";
    public static final String KEY_PAY_AUTO  = "pay_auto";
    public static final String KEY_PAY_SEC   = "pay_security";
    public static final String KEY_PAY_CREDIT= "pay_credit";
    public static final String KEY_SOUND     = "sound_enabled";
    public static final String KEY_VIBRATION = "vibration_enabled";
    public static final String KEY_LAST_TX_ID   = "last_tx_id";
    public static final String KEY_LAST_NOTIF_ID = "last_notif_id";

    public static final String CH_TRANSFERS = "ch_transfers";
    public static final String CH_MARKET    = "ch_market";
    public static final String CH_PROMO     = "ch_promo";
    public static final String CH_SECURITY  = "ch_security";

    private static int notifIdCounter = 1000;

    public static void createChannels(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH_TRANSFERS, "Переводы", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CH_MARKET, "Tumar Market", NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(CH_PROMO, "Акции", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(CH_SECURITY, "Безопасность", NotificationManager.IMPORTANCE_HIGH));
    }

    public static boolean isEnabled(Context ctx, String key) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!p.getBoolean(KEY_ALL, true)) return false;
        return p.getBoolean(key, true);
    }

    public static void showNotification(Context ctx, String channelId, String title, String body) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        Intent intent = new Intent(ctx, PinEntryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_bell_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(channelId.equals(CH_TRANSFERS) || channelId.equals(CH_SECURITY)
                        ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        nm.notify(notifIdCounter++, builder.build());
    }
}
