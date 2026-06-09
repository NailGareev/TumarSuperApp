package com.digitalcompany.tumarsuperapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.BarcodeView;

public class QrScanFragment extends Fragment {

    private static final int CAMERA_PERM_CODE = 1001;

    private BarcodeView barcodeView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scan, container, false);

        barcodeView = view.findViewById(R.id.barcodeView);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> {
            if (getActivity() != null)
                getActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startDecoding();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) barcodeView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (barcodeView != null) barcodeView.pause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    private void startDecoding() {
        barcodeView.resume();
        barcodeView.decodeContinuous(result -> {
            if (result.getText() == null) return;
            barcodeView.pause();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // TODO: handle QR payment link
                    Toast.makeText(getContext(), "QR: " + result.getText(), Toast.LENGTH_LONG).show();
                    getActivity().getSupportFragmentManager().popBackStack();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDecoding();
            } else {
                Toast.makeText(getContext(), "Для сканирования QR нужен доступ к камере",
                        Toast.LENGTH_SHORT).show();
                if (getActivity() != null)
                    getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }
}
