package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements MenuProvider {

    private static final String TAG = "HomeFragment";

    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_NAME = "user_name";

    private TextView tvGreeting;
    private TextView tvDate;
    private TextView tvPhoneNumber;
    private TextView tvBalance;
    private LinearLayout buttonTopUp, buttonHistory, buttonTransfer, buttonPayments;
    private FrameLayout btnQrPay;
    private FrameLayout btnBell;

    private ApiService apiService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        } else {
            Log.e(TAG, "Activity is null in onCreate");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvGreeting     = view.findViewById(R.id.tvGreeting);
        tvDate         = view.findViewById(R.id.tvDate);
        tvPhoneNumber  = view.findViewById(R.id.userIdTextView);
        tvBalance      = view.findViewById(R.id.balanceTextView);
        buttonTopUp    = view.findViewById(R.id.buttonTopUp);
        buttonHistory  = view.findViewById(R.id.buttonHistory);
        buttonTransfer = view.findViewById(R.id.buttonTransfer);
        buttonPayments = view.findViewById(R.id.buttonPayments);
        btnQrPay       = view.findViewById(R.id.btnQrPay);
        btnBell        = view.findViewById(R.id.btnBell);

        tvPhoneNumber.setText("+7 ...");
        tvBalance.setText("---.-- ₸");

        setGreeting();
        setCurrentDate();

        view.findViewById(R.id.ll_travel).setOnClickListener(v ->
                navigateToFragment(new TravelFragment(), "travel"));
        view.findViewById(R.id.ll_market).setOnClickListener(v ->
                navigateToFragment(new TumarMarketFragment(), "market"));

        setupActionButtons();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        if (apiService != null) {
            loadUserProfileData();
        } else {
            Log.e(TAG, "apiService is null in onViewCreated");
        }
    }

    private void setGreeting() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_USER_NAME, null);
        if (tvGreeting == null) return;
        if (name != null && !name.isEmpty()) {
            tvGreeting.setText("Привет, " + name + "! 👋");
        } else {
            tvGreeting.setText("Привет! 👋");
        }
    }

    private void setCurrentDate() {
        if (tvDate == null) return;
        try {
            LocalDate today = LocalDate.now();
            Locale ruLocale = new Locale("ru");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EE, d MMMM yyyy", ruLocale);
            String dateStr = today.format(formatter);
            // Capitalise first letter
            if (!dateStr.isEmpty()) {
                dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
            }
            tvDate.setText(dateStr);
        } catch (Exception e) {
            Log.e(TAG, "Date formatting error", e);
        }
    }

    private void setupActionButtons() {
        buttonTopUp.setOnClickListener(v -> navigateToFragment(new TopUpFragment(), "topup"));

        buttonHistory.setOnClickListener(v -> {
            if (getActivity() != null) {
                navigateToFragment(new HistoryFragment(), "history");
            }
        });

        buttonTransfer.setOnClickListener(v -> {
            if (getActivity() != null) {
                navigateToFragment(new TransferFragment(), "transfer");
            }
        });

        buttonPayments.setOnClickListener(v ->
                navigateToFragment(new PaymentsFragment(), "payments"));

        if (btnQrPay != null) {
            btnQrPay.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "QR-оплата", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnBell != null) {
            btnBell.setOnClickListener(v ->
                    navigateToFragment(new NotificationsFragment(), "notifications"));
        }
    }

    private void navigateToFragment(Fragment fragment, String tag) {
        if (getActivity() != null) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, tag);
            transaction.addToBackStack(tag);
            transaction.commit();
        } else {
            Log.e(TAG, "Cannot navigate, activity is null");
        }
    }

    private void loadUserProfileData() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);

        if (!isLoggedIn || token == null) {
            Log.w(TAG, "User not logged in");
            return;
        }

        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    if (profile.isSuccess()) {
                        updateUI(profile);
                    } else {
                        Toast.makeText(getContext(), "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Server error: " + response.code());
                    if (response.code() == 401 || response.code() == 403) {
                        Toast.makeText(requireContext(), "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                        LoginActivity.logout(requireActivity());
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Network error loading profile", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(UserProfileResponse profile) {
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
                Log.e(TAG, "Currency format error: " + e.getMessage());
                tvBalance.setText(String.format(Locale.US, "%.2f %s", balance, currencyCode));
            }
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return "+7 ??? ??? ?? ??";
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

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }
}
