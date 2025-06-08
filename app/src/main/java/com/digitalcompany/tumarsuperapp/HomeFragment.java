package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Импорт
import androidx.fragment.app.FragmentTransaction; // Импорт
import androidx.lifecycle.Lifecycle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// Импорты для сети и моделей
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;

// Импорты для форматирования
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

// Импорты Retrofit
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Реализуем MenuProvider для современного управления меню
public class HomeFragment extends Fragment implements MenuProvider {

    private static final String TAG = "HomeFragment";

    // Ключи SharedPreferences для проверки статуса входа и получения токена
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    // --- UI Элементы ---
    private TextView tvPhoneNumber;
    private TextView tvBalance;
    private LinearLayout buttonTopUp, buttonHistory, buttonTransfer, buttonPayments;

    // --- Сетевые компоненты ---
    private ApiService apiService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        } else {
            Log.e(TAG, "Activity is null in onCreate, cannot initialize ApiService");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Находим View элементы по их ID из fragment_home.xml
        tvPhoneNumber = view.findViewById(R.id.userIdTextView);
        tvBalance = view.findViewById(R.id.balanceTextView);
        buttonTopUp = view.findViewById(R.id.buttonTopUp);
        buttonHistory = view.findViewById(R.id.buttonHistory);
        buttonTransfer = view.findViewById(R.id.buttonTransfer);
        buttonPayments = view.findViewById(R.id.buttonPayments);

        // Устанавливаем временные плейсхолдеры
        tvPhoneNumber.setText("+7 ...");
        tvBalance.setText("---.-- ???");

        // Устанавливаем слушатели кликов
        setupActionButtons();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Настройка MenuProvider
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // Загружаем данные пользователя
        if (apiService != null) {
            loadUserProfileData();
        } else {
            Log.e(TAG, "apiService is null in onViewCreated, cannot load profile data.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Ошибка инициализации сети", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Метод для установки слушателей на кнопки действий
    private void setupActionButtons() {
        buttonTopUp.setOnClickListener(v -> showToast("Нажата кнопка Пополнить"));

        // --- ИЗМЕНЕНИЕ: Навигация на HistoryFragment ---
        buttonHistory.setOnClickListener(v -> {
            Log.d(TAG, "Кнопка История нажата, переход на HistoryFragment");
            if (getActivity() != null) {
                // Создаем экземпляр HistoryFragment (код для него был предоставлен ранее)
                HistoryFragment historyFragment = new HistoryFragment();
                navigateToFragment(historyFragment, "history"); // Используем вспомогательный метод
            } else {
                Log.e(TAG, "Activity is null when History button clicked.");
            }
        });
        // --- КОНЕЦ ИЗМЕНЕНИЯ ---

        buttonTransfer.setOnClickListener(v -> {
            Log.d(TAG, "Кнопка Перевод нажата");
            if (getActivity() != null) {
                TransferFragment transferFragment = new TransferFragment();
                navigateToFragment(transferFragment, "transfer");
            } else {
                Log.e(TAG, "Activity is null when Transfer button clicked.");
            }
        });

        buttonPayments.setOnClickListener(v -> showToast("Нажата кнопка Платежи"));
    }

    // Вспомогательный метод для навигации
    private void navigateToFragment(Fragment fragment, String tag) {
        if (getActivity() != null) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // ВАЖНО: Замените R.id.fragment_container на ID вашего контейнера в MainActivity
            transaction.replace(R.id.fragment_container, fragment, tag);
            transaction.addToBackStack(tag); // Добавляем в стек для кнопки "назад"
            transaction.commit();
        } else {
            Log.e(TAG, "Cannot navigate to fragment, activity is null (inside navigateToFragment).");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Не удалось открыть экран", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // --- Метод для загрузки данных пользователя с бэкенда ---
    private void loadUserProfileData() {
        // ... (код без изменений) ...
        if (getContext() == null) { Log.w(TAG, "Context is null, cannot load profile data."); return; }
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);

        if (!isLoggedIn || token == null) {
            Log.w(TAG, "Пользователь не вошел в систему, не могу загрузить профиль.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Требуется вход в систему", Toast.LENGTH_SHORT).show();
            }
            // LoginActivity.logout(requireActivity());
            return;
        }

        Log.d(TAG, "Запрос данных профиля к /api/profile...");
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!isAdded() || getContext() == null) { Log.w(TAG, "Fragment detached or context is null after API response."); return; }

                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    Log.d(TAG, "Профиль успешно загружен: " + profile);
                    if (profile.isSuccess()) {
                        updateUI(profile);
                    } else {
                        Log.w(TAG, "API вернул success=false при загрузке профиля.");
                        if (getContext() != null) Toast.makeText(getContext(), "Не удалось загрузить данные профиля", Toast.LENGTH_SHORT).show();
                        // LoginActivity.logout(requireActivity());
                    }
                } else {
                    Log.e(TAG, "Ошибка ответа сервера при загрузке профиля: " + response.code());
                    if (getContext() != null) Toast.makeText(getContext(), "Ошибка сервера (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    if (response.code() == 401 || response.code() == 403) {
                        Log.w(TAG, "Ошибка авторизации (401/403), возможно, токен истек.");
                        // LoginActivity.logout(requireActivity());
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) { Log.w(TAG, "Fragment detached or context is null on API failure."); return; }
                Log.e(TAG, "Ошибка сети при загрузке профиля", t);
                if (getContext() != null) Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Метод для обновления UI данными из профиля ---
    private void updateUI(UserProfileResponse profile) {
        // ... (код без изменений) ...
        if (profile == null) return;
        if (tvPhoneNumber != null) {
            tvPhoneNumber.setText(formatPhoneNumber(profile.getPhone()));
        }
        if (tvBalance != null) {
            BigDecimal balance = profile.getBalance() != null ? profile.getBalance() : BigDecimal.ZERO;
            String currencyCode = profile.getCurrency() != null ? profile.getCurrency().toUpperCase() : "KZT";
            try {
                Locale localeKZ = new Locale("kk", "KZ");
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(localeKZ);
                currencyFormat.setCurrency(Currency.getInstance(currencyCode));
                if ("KZT".equals(currencyCode)) {
                    currencyFormat.setMaximumFractionDigits(0);
                    currencyFormat.setMinimumFractionDigits(0);
                } else {
                    currencyFormat.setMaximumFractionDigits(2);
                    currencyFormat.setMinimumFractionDigits(2);
                }
                tvBalance.setText(currencyFormat.format(balance));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка форматирования валюты: " + e.getMessage());
                tvBalance.setText(String.format(Locale.US, "%.2f %s", balance, currencyCode));
            }
        }
    }

    // --- Вспомогательный метод для форматирования номера телефона ---
    private String formatPhoneNumber(String phone) {
        // ... (код без изменений) ...
        if (phone == null) { return "+7 ??? ??? ?? ??"; }
        String digits = phone.replaceAll("[^\\d+]", "");
        if (digits.startsWith("+7") && digits.length() == 12) {
            return String.format("+7 (%s) %s-%s-%s",
                    digits.substring(2, 5),
                    digits.substring(5, 8),
                    digits.substring(8, 10),
                    digits.substring(10, 12));
        }
        return phone;
    }


    // Вспомогательный метод для показа Toast
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // --- Методы MenuProvider ---
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // ... (код без изменений) ...
        menuInflater.inflate(R.menu.top_app_bar_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // ... (код без изменений) ...
        if (menuItem.getItemId() == R.id.action_notifications) {
            showToast("Нажаты Уведомления (HomeFragment Listener)");
            // TODO: Открыть экран уведомлений
            return true;
        }
        return false;
    }
}