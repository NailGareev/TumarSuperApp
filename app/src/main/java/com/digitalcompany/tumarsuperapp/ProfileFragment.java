package com.digitalcompany.tumarsuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // <<< Добавлен импорт Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <<< Добавлен импорт Button
import android.widget.Toast; // <<< Добавлен импорт Toast (опционально)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment"; // <<< Тег для логирования

    private CardView actionChangePin;
    private CardView actionMyReturns;
    private CardView actionSellerCabinet;
    private Button btnLogout; // <<< Добавлена переменная для кнопки выхода

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        actionChangePin = view.findViewById(R.id.action_change_pin);
        actionMyReturns = view.findViewById(R.id.action_my_returns);
        actionSellerCabinet = view.findViewById(R.id.action_seller_cabinet);
        btnLogout = view.findViewById(R.id.btn_logout); // <<< Находим кнопку выхода по ID

        actionChangePin.setOnClickListener(v -> {
            Log.d(TAG, "Change PIN clicked");
            Intent intent = new Intent(getActivity(), PinSetupActivity.class);
            intent.putExtra("IS_CHANGE_MODE", true);
            startActivity(intent);
        });

        actionMyReturns.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new BuyerReturnsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        actionSellerCabinet.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SellerReturnsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // --- НАЧАЛО: Обработчик нажатия кнопки Выхода ---
        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            // Вызываем статический метод logout из LoginActivity
            // Передаем контекст Activity
            if (getActivity() != null) {
                // Дополнительно можно показать диалог подтверждения
                // new AlertDialog.Builder(requireContext())... show();
                LoginActivity.logout(requireActivity());
            } else {
                Log.e(TAG, "Cannot logout, activity is null.");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Не удалось выйти из системы", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // --- КОНЕЦ: Обработчик нажатия кнопки Выхода ---

        return view;
    }

    // Код для ActivityResultLauncher оставлен закомментированным, как и был
     /*
      private ActivityResultLauncher<Intent> pinChangeLauncher;
      @Override
      public void onCreate(@Nullable Bundle savedInstanceState) { ... }
      */
}