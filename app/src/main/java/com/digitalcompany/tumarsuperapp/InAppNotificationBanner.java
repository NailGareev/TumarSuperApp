package com.digitalcompany.tumarsuperapp;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class InAppNotificationBanner {

    private static View currentBanner = null;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable dismissRunnable = null;

    public static void show(Activity activity, String appName, String title, String body,
                             String time, String primaryAction, String secondaryAction) {
        if (activity == null || activity.isFinishing()) return;

        // Dismiss existing banner if any
        dismissCurrent(activity);

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        View banner = LayoutInflater.from(activity).inflate(R.layout.view_inapp_notification_banner, root, false);

        TextView tvAppName = banner.findViewById(R.id.banner_app_name);
        TextView tvTitle = banner.findViewById(R.id.banner_title);
        TextView tvBody = banner.findViewById(R.id.banner_body);
        TextView tvTime = banner.findViewById(R.id.banner_time);
        View actionsRow = banner.findViewById(R.id.banner_actions);
        Button btnPrimary = banner.findViewById(R.id.banner_btn_primary);
        Button btnSecondary = banner.findViewById(R.id.banner_btn_secondary);

        if (tvAppName != null) tvAppName.setText(appName != null ? appName : "Tumar");
        if (tvTitle != null) tvTitle.setText(title != null ? title : "");
        if (tvBody != null) tvBody.setText(body != null ? body : "");
        if (tvTime != null) tvTime.setText(time != null ? time : "");

        if (primaryAction != null || secondaryAction != null) {
            if (actionsRow != null) actionsRow.setVisibility(View.VISIBLE);
            if (btnPrimary != null) {
                if (primaryAction != null) {
                    btnPrimary.setText(primaryAction);
                    btnPrimary.setOnClickListener(v -> dismiss(activity, banner));
                } else {
                    btnPrimary.setVisibility(View.GONE);
                }
            }
            if (btnSecondary != null) {
                if (secondaryAction != null) {
                    btnSecondary.setText(secondaryAction);
                    btnSecondary.setOnClickListener(v -> dismiss(activity, banner));
                } else {
                    btnSecondary.setVisibility(View.GONE);
                }
            }
        }

        banner.setOnClickListener(v -> dismiss(activity, banner));

        // Add to root
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(banner, params);
        currentBanner = banner;

        // Animate in: slide from top
        banner.setTranslationY(-600f);
        banner.animate()
                .translationY(0f)
                .setDuration(350)
                .start();

        // Auto-dismiss after 4 seconds
        dismissRunnable = () -> dismiss(activity, banner);
        handler.postDelayed(dismissRunnable, 4000);
    }

    private static void dismiss(Activity activity, View banner) {
        if (banner == null || banner.getParent() == null) return;
        if (dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
        }
        banner.animate()
                .translationY(-600f)
                .setDuration(300)
                .withEndAction(() -> {
                    ViewGroup parent = (ViewGroup) banner.getParent();
                    if (parent != null) parent.removeView(banner);
                    if (currentBanner == banner) currentBanner = null;
                })
                .start();
    }

    private static void dismissCurrent(Activity activity) {
        if (currentBanner != null) {
            dismiss(activity, currentBanner);
        }
    }
}
