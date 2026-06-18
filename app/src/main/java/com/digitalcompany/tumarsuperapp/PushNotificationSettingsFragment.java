package com.digitalcompany.tumarsuperapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class PushNotificationSettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // All toggle pairs: track view id name, thumb view id name, pref key
    private static final String[][] TOGGLES = {
        { "toggle_push_track", "toggle_push_thumb", NotificationHelper.KEY_ALL },
        { "toggle_tr_in_track", "toggle_tr_in_thumb", NotificationHelper.KEY_TR_IN },
        { "toggle_tr_out_track", "toggle_tr_out_thumb", NotificationHelper.KEY_TR_OUT },
        { "toggle_tr_chat_track", "toggle_tr_chat_thumb", NotificationHelper.KEY_TR_CHAT },
        { "toggle_mkt_order_track", "toggle_mkt_order_thumb", NotificationHelper.KEY_MKT_ORDER },
        { "toggle_mkt_code_track", "toggle_mkt_code_thumb", NotificationHelper.KEY_MKT_CODE },
        { "toggle_mkt_promo_track", "toggle_mkt_promo_thumb", NotificationHelper.KEY_MKT_PROMO },
        { "toggle_pay_auto_track", "toggle_pay_auto_thumb", NotificationHelper.KEY_PAY_AUTO },
        { "toggle_pay_sec_track", "toggle_pay_sec_thumb", NotificationHelper.KEY_PAY_SEC },
        { "toggle_pay_credit_track", "toggle_pay_credit_thumb", NotificationHelper.KEY_PAY_CREDIT },
        { "toggle_sound_track", "toggle_sound_thumb", NotificationHelper.KEY_SOUND },
        { "toggle_vibration_track", "toggle_vibration_thumb", NotificationHelper.KEY_VIBRATION },
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {});
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_push_notification_settings, container, false);
        prefs = requireContext().getSharedPreferences(NotificationHelper.PREFS_NAME, android.content.Context.MODE_PRIVATE);

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Back button
        view.findViewById(R.id.btn_push_back).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Load all toggle states
        loadAllToggles(view);

        // Set up click listeners for all toggles
        setupToggleListeners(view);

        return view;
    }

    private void loadAllToggles(View root) {
        for (String[] toggle : TOGGLES) {
            int trackId = root.getContext().getResources().getIdentifier(toggle[0], "id", root.getContext().getPackageName());
            int thumbId = root.getContext().getResources().getIdentifier(toggle[1], "id", root.getContext().getPackageName());
            View track = root.findViewById(trackId);
            View thumb = root.findViewById(thumbId);
            if (track == null || thumb == null) continue;
            boolean enabled = prefs.getBoolean(toggle[2], true);
            applyToggle(track, thumb, enabled);
        }
        updateSubTogglesEnabled(root);
    }

    private void setupToggleListeners(View root) {
        for (int i = 0; i < TOGGLES.length; i++) {
            final int idx = i;
            final String[] toggle = TOGGLES[i];
            int trackId = root.getContext().getResources().getIdentifier(toggle[0], "id", root.getContext().getPackageName());
            View track = root.findViewById(trackId);
            if (track == null) continue;
            track.setOnClickListener(v -> {
                int tId = root.getContext().getResources().getIdentifier(toggle[0], "id", root.getContext().getPackageName());
                int thId = root.getContext().getResources().getIdentifier(toggle[1], "id", root.getContext().getPackageName());
                View tr = root.findViewById(tId);
                View th = root.findViewById(thId);
                if (tr == null || th == null) return;
                boolean current = prefs.getBoolean(toggle[2], true);
                boolean newVal = !current;
                prefs.edit().putBoolean(toggle[2], newVal).apply();
                applyToggle(tr, th, newVal);
                if (idx == 0) {
                    updateSubTogglesEnabled(root);
                }
            });
        }
    }

    private void applyToggle(View track, View thumb, boolean enabled) {
        if (enabled) {
            track.setBackgroundResource(R.drawable.bg_bio_toggle_on);
            thumb.setTranslationX(0f);
        } else {
            track.setBackgroundResource(R.drawable.bg_bio_toggle_off);
            // Move thumb to start
            track.post(() -> {
                float offset = -(track.getWidth() - thumb.getWidth() - 6);
                thumb.setTranslationX(offset);
            });
        }
    }

    private void updateSubTogglesEnabled(View root) {
        boolean masterOn = prefs.getBoolean(NotificationHelper.KEY_ALL, true);
        float alpha = masterOn ? 1f : 0.4f;
        // Apply alpha to all sub-toggles (skip index 0 which is master)
        for (int i = 1; i < TOGGLES.length; i++) {
            String[] toggle = TOGGLES[i];
            int trackId = root.getContext().getResources().getIdentifier(toggle[0], "id", root.getContext().getPackageName());
            View track = root.findViewById(trackId);
            if (track != null) {
                track.setAlpha(alpha);
                track.setEnabled(masterOn);
            }
        }
    }
}
