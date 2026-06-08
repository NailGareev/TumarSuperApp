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
import com.digitalcompany.tumarsuperapp.network.models.CurrencyRate;
import com.digitalcompany.tumarsuperapp.network.models.CurrencyRatesResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements MenuProvider {

    private static final String TAG = "HomeFragment";

    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN    = "auth_token";
    private static final String KEY_USER_NAME     = "user_name";

    // Header views
    private TextView tvGreeting;
    private TextView tvDate;

    // Balance card
    private TextView tvPhoneNumber;
    private TextView tvBalance;

    // Quick actions
    private LinearLayout buttonTopUp, buttonHistory, buttonTransfer, buttonPayments;

    // Buttons
    private FrameLayout btnQrPay;
    private FrameLayout btnBell;

    // Currency rate views
    private TextView tvUsdRate, tvUsdChange;
    private TextView tvEurRate, tvEurChange;
    private TextView tvRubRate, tvRubChange;

    private ApiService apiService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
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

        tvUsdRate   = view.findViewById(R.id.tvUsdRate);
        tvUsdChange = view.findViewById(R.id.tvUsdChange);
        tvEurRate   = view.findViewById(R.id.tvEurRate);
        tvEurChange = view.findViewById(R.id.tvEurChange);
        tvRubRate   = view.findViewById(R.id.tvRubRate);
        tvRubChange = view.findViewById(R.id.tvRubChange);

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
            loadCurrencyRates();
        }
    }

    // ── Greeting ────────────────────────────────────────────────────────────────

    private void setGreeting() {
        if (getContext() == null || tvGreeting == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_USER_NAME, null);
        tvGreeting.setText((name != null && !name.isEmpty())
                ? "Привет, " + name + "! 👋"
                : "Привет! 👋");
    }

    private void setCurrentDate() {
        if (tvDate == null) return;
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EE, d MMMM yyyy", new Locale("ru"));
            String s = today.format(fmt);
            if (!s.isEmpty()) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
            tvDate.setText(s);
        } catch (Exception e) {
            Log.e(TAG, "Date format error", e);
        }
    }

    // ── Buttons ─────────────────────────────────────────────────────────────────

    private void setupActionButtons() {
        buttonTopUp.setOnClickListener(v    -> navigateToFragment(new TopUpFragment(),    "topup"));
        buttonTransfer.setOnClickListener(v -> navigateToFragment(new TransferFragment(), "transfer"));
        buttonPayments.setOnClickListener(v -> navigateToFragment(new PaymentsFragment(), "payments"));
        buttonHistory.setOnClickListener(v  -> navigateToFragment(new HistoryFragment(),  "history"));

        if (btnQrPay != null) {
            btnQrPay.setOnClickListener(v ->
                    navigateToFragment(new QrScanFragment(), "qr_scan"));
        }

        if (btnBell != null) {
            btnBell.setOnClickListener(v ->
                    navigateToFragment(new NotificationsFragment(), "notifications"));
        }
    }

    private void navigateToFragment(Fragment fragment, String tag) {
        if (getActivity() == null) return;
        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    // ── Profile data ─────────────────────────────────────────────────────────────

    private void loadUserProfileData() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                || prefs.getString(KEY_AUTH_TOKEN, null) == null) return;

        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    updateBalanceUI(response.body());
                } else if (response.code() == 401 || response.code() == 403) {
                    LoginActivity.logout(requireActivity());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Profile load error", t);
            }
        });
    }

    private void updateBalanceUI(UserProfileResponse profile) {
        if (tvPhoneNumber != null)
            tvPhoneNumber.setText(formatPhone(profile.getPhone()));

        if (tvBalance != null) {
            BigDecimal balance = profile.getBalance() != null ? profile.getBalance() : BigDecimal.ZERO;
            String code = profile.getCurrency() != null ? profile.getCurrency().toUpperCase() : "KZT";
            try {
                NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("kk", "KZ"));
                fmt.setCurrency(Currency.getInstance(code));
                if ("KZT".equals(code)) { fmt.setMaximumFractionDigits(0); fmt.setMinimumFractionDigits(0); }
                tvBalance.setText(fmt.format(balance));
            } catch (Exception e) {
                tvBalance.setText(String.format(Locale.US, "%.0f ₸", balance));
            }
        }
    }

    private String formatPhone(String phone) {
        if (phone == null) return "+7 ???";
        String d = phone.replaceAll("[^\\d+]", "");
        if (d.startsWith("+7") && d.length() == 12) {
            return String.format("+7 (%s) %s-%s-%s",
                    d.substring(2, 5), d.substring(5, 8),
                    d.substring(8, 10), d.substring(10, 12));
        }
        return phone;
    }

    // ── Currency rates ───────────────────────────────────────────────────────────

    private void loadCurrencyRates() {
        apiService.getCurrencyRates().enqueue(new Callback<CurrencyRatesResponse>() {
            @Override
            public void onResponse(@NonNull Call<CurrencyRatesResponse> call,
                                   @NonNull Response<CurrencyRatesResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    updateRatesUI(response.body().getRates());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CurrencyRatesResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Rates load error", t);
            }
        });
    }

    private void updateRatesUI(List<CurrencyRate> rates) {
        if (rates == null) return;
        for (CurrencyRate r : rates) {
            String code = r.getCurrencyCode();
            String rateStr = String.format(Locale.US, "%.2f", r.getRate());
            if ("USD".equalsIgnoreCase(code)) {
                if (tvUsdRate != null) tvUsdRate.setText(rateStr);
            } else if ("EUR".equalsIgnoreCase(code)) {
                if (tvEurRate != null) tvEurRate.setText(rateStr);
            } else if ("RUB".equalsIgnoreCase(code)) {
                if (tvRubRate != null) tvRubRate.setText(rateStr);
            }
        }
    }

    // ── Menu ─────────────────────────────────────────────────────────────────────

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }
}
