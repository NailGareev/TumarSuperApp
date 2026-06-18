package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AddFingerprintFragment extends Fragment {

    private static final int TOTAL_DOTS = 6;
    private int touchCount = 3; // starts at 3/6 (matching the wireframe)

    private final int[] dotIds = {
            R.id.dot_1, R.id.dot_2, R.id.dot_3,
            R.id.dot_4, R.id.dot_5, R.id.dot_6
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_fingerprint, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_add_fp_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btn_skip_fingerprint).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Update dots to match initial touchCount
        updateDots(view);

        // Each tap on the scanner advances progress
        view.findViewById(R.id.scanner_container).setOnClickListener(v -> {
            if (touchCount >= TOTAL_DOTS) {
                Toast.makeText(getContext(),
                        "Отпечаток «Палец 2» успешно добавлен!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }
            touchCount++;
            updateDots(view);
            if (touchCount == TOTAL_DOTS) {
                view.postDelayed(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Отпечаток «Палец 2» успешно добавлен!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }, 600);
            }
        });
    }

    private void updateDots(View root) {
        for (int i = 0; i < TOTAL_DOTS; i++) {
            View dot = root.findViewById(dotIds[i]);
            if (dot == null) continue;
            boolean filled = i < touchCount;
            dot.setBackgroundResource(filled ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            ViewGroup.LayoutParams lp = dot.getLayoutParams();
            lp.width = dpToPx(filled ? 10 : 8);
            lp.height = dpToPx(filled ? 10 : 8);
            dot.setLayoutParams(lp);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
