package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BiometricSettingsFragment extends Fragment {

    private boolean biometricEnabled = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_biometric_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_bio_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Master toggle
        FrameLayout toggleTrack = view.findViewById(R.id.toggle_bio_track);
        View toggleThumb = view.findViewById(R.id.toggle_bio_thumb);

        view.findViewById(R.id.card_master_toggle).setOnClickListener(v -> {
            biometricEnabled = !biometricEnabled;
            applyToggle(toggleTrack, toggleThumb, biometricEnabled);
        });
        applyToggle(toggleTrack, toggleThumb, biometricEnabled);

        // Fingerprint row → Add fingerprint screen
        view.findViewById(R.id.row_fingerprint).setOnClickListener(v -> openAddFingerprint());

        // Face ID "+ Добавить" button
        view.findViewById(R.id.btn_add_face).setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Face ID не поддерживается на этом устройстве",
                        Toast.LENGTH_SHORT).show());

        // "+ Добавить" in section header
        view.findViewById(R.id.btn_add_fingerprint_header).setOnClickListener(v -> openAddFingerprint());

        // Add another fingerprint dashed row
        view.findViewById(R.id.row_add_fingerprint).setOnClickListener(v -> openAddFingerprint());

        // Delete fingerprint
        view.findViewById(R.id.btn_delete_fingerprint).setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Отпечаток «Палец 1» удалён",
                        Toast.LENGTH_SHORT).show());
    }

    private void applyToggle(FrameLayout track, View thumb, boolean enabled) {
        track.setBackgroundResource(enabled ? R.drawable.bg_bio_toggle_on : R.drawable.bg_bio_toggle_off);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        float dp = requireContext().getResources().getDisplayMetrics().density;
        int margin = (int) (3 * dp);
        if (enabled) {
            p.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            p.setMarginStart(0);
            p.setMarginEnd(margin);
        } else {
            p.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            p.setMarginStart(margin);
            p.setMarginEnd(0);
        }
        thumb.setLayoutParams(p);
    }

    private void openAddFingerprint() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, new AddFingerprintFragment())
                .addToBackStack(null)
                .commit();
    }
}
