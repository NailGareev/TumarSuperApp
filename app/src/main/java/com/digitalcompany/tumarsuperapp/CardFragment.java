package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.CardResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardFragment extends Fragment {

    private static final String PREFS_NAME     = "CardDataPrefs";
    private static final String KEY_CARD_EXISTS = "card_exists";
    private static final String KEY_CARD_NUMBER = "card_number";
    private static final String KEY_CARD_EXPIRY = "card_expiry";
    private static final String KEY_CARD_CVV    = "card_cvv";
    private static final String KEY_CARD_BLOCKED     = "card_blocked";
    private static final String KEY_CARD_CUSTOM_NAME = "card_custom_name";

    private static final long   CVV_VISIBILITY_DURATION_MS = 60 * 1000;
    private static final String CVV_PLACEHOLDER = "•••";
    private static final String TAG = "CardFragment";

    // Header
    private FrameLayout btnAddCard;
    private FrameLayout btnCardBell;

    // No-card state
    private LinearLayout layoutNoCard;

    // Card details state
    private LinearLayout layoutCardDetails;
    private CardView     cardViewClickableArea;
    private TextView     textCardCustomNameMain;
    private TextView     textCardNumber;
    private TextView     textCardExpiry;
    private TextView     textCardCvv;
    private View         textCardBlockedOverlayMain;  // LinearLayout acting as overlay

    // Stats
    private TextView tvStatBalance;

    // Quick actions
    private LinearLayout actionCardTopup;
    private LinearLayout actionCardTransfer;
    private LinearLayout actionCardBlock;
    private LinearLayout actionCardSettings;
    private TextView     tvBlockActionLabel;
    private ImageView    iconBlockAction;

    // Details strip
    private TextView    tvCardNumberMasked;
    private TextView    tvCardCvvStrip;
    private FrameLayout btnShowCvv;

    // All operations
    private LinearLayout btnAllOperations;

    // CVV state
    private String  actualCvv;
    private boolean isCvvVisible = false;

    private SharedPreferences sharedPreferences;
    private ApiService        apiService;

    private Handler  cvvHandler;
    private Runnable hideCvvRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cvvHandler = new Handler(Looper.getMainLooper());
        hideCvvRunnable = () -> {
            if (textCardCvv != null)      textCardCvv.setText(CVV_PLACEHOLDER);
            if (tvCardCvvStrip != null)   tvCardCvvStrip.setText(CVV_PLACEHOLDER);
            isCvvVisible = false;
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        btnAddCard  = view.findViewById(R.id.btn_add_card);
        btnCardBell = view.findViewById(R.id.btn_card_bell);

        layoutNoCard     = view.findViewById(R.id.layout_no_card);
        layoutCardDetails = view.findViewById(R.id.layout_card_details);

        view.findViewById(R.id.button_create_card).setOnClickListener(v -> createNewCard());

        cardViewClickableArea   = view.findViewById(R.id.card_view_clickable_area);
        textCardCustomNameMain  = view.findViewById(R.id.text_card_custom_name_main);
        textCardNumber          = view.findViewById(R.id.text_card_number);
        textCardExpiry          = view.findViewById(R.id.text_card_expiry);
        textCardCvv             = view.findViewById(R.id.text_card_cvv);
        textCardBlockedOverlayMain = view.findViewById(R.id.text_card_blocked_overlay_main);

        tvStatBalance = view.findViewById(R.id.tv_stat_balance);

        actionCardTopup    = view.findViewById(R.id.action_card_topup);
        actionCardTransfer = view.findViewById(R.id.action_card_transfer);
        actionCardBlock    = view.findViewById(R.id.action_card_block);
        actionCardSettings = view.findViewById(R.id.action_card_settings);
        tvBlockActionLabel = view.findViewById(R.id.tv_block_action_label);
        iconBlockAction    = view.findViewById(R.id.icon_block_action);

        tvCardNumberMasked = view.findViewById(R.id.tv_card_number_masked);
        tvCardCvvStrip     = view.findViewById(R.id.tv_card_cvv_strip);
        btnShowCvv         = view.findViewById(R.id.btn_show_cvv);

        btnAllOperations = view.findViewById(R.id.btn_all_operations);

        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }

        setupClickListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkCardStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences != null && sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            updateCardAppearance(
                    sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false),
                    sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, ""));
        } else {
            showCreateCardView();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cvvHandler.removeCallbacks(hideCvvRunnable);
    }

    // ── Click listeners ─────────────────────────────────────────────────────────

    private void setupClickListeners() {
        if (btnCardBell != null)
            btnCardBell.setOnClickListener(v -> navigate(new NotificationsFragment(), "notifications"));

        if (btnAddCard != null)
            btnAddCard.setOnClickListener(v -> {
                if (sharedPreferences != null && !sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
                    createNewCard();
                } else {
                    Toast.makeText(requireContext(), "Карта уже создана", Toast.LENGTH_SHORT).show();
                }
            });

        if (textCardCvv != null)
            textCardCvv.setOnClickListener(v -> toggleCvvVisibility());

        if (btnShowCvv != null)
            btnShowCvv.setOnClickListener(v -> toggleCvvVisibility());

        if (cardViewClickableArea != null)
            cardViewClickableArea.setOnClickListener(v -> navigateToCardManagement());

        if (actionCardTopup != null)
            actionCardTopup.setOnClickListener(v -> navigate(new TopUpFragment(), "topup"));

        if (actionCardTransfer != null)
            actionCardTransfer.setOnClickListener(v -> navigate(new TransferFragment(), "transfer"));

        if (actionCardBlock != null)
            actionCardBlock.setOnClickListener(v -> toggleBlockStatusInline());

        if (actionCardSettings != null)
            actionCardSettings.setOnClickListener(v -> navigateToCardManagement());

        if (btnAllOperations != null)
            btnAllOperations.setOnClickListener(v -> navigate(new HistoryFragment(), "history"));
    }

    // ── Card status ──────────────────────────────────────────────────────────────

    private void checkCardStatus() {
        if (sharedPreferences == null) { showCreateCardView(); return; }
        if (sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            displayCardDetails();
            if (apiService != null) loadUserBalance();
        } else {
            showCreateCardView();
        }
    }

    private void displayCardDetails() {
        if (sharedPreferences == null) return;

        String cardNumber = sharedPreferences.getString(KEY_CARD_NUMBER, "");
        String expiry     = sharedPreferences.getString(KEY_CARD_EXPIRY, "");
        actualCvv         = sharedPreferences.getString(KEY_CARD_CVV, "");
        boolean isBlocked = sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false);
        String customName = sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, "");

        if (textCardNumber != null)  textCardNumber.setText(formatCardNumber(cardNumber));
        if (textCardExpiry != null)  textCardExpiry.setText(expiry);
        if (textCardCvv != null)     textCardCvv.setText(CVV_PLACEHOLDER);
        if (tvCardCvvStrip != null)  tvCardCvvStrip.setText(CVV_PLACEHOLDER);
        if (tvCardNumberMasked != null) tvCardNumberMasked.setText(maskCardNumber(cardNumber));
        isCvvVisible = false;

        updateCardAppearance(isBlocked, customName);

        if (layoutNoCard != null)     layoutNoCard.setVisibility(View.GONE);
        if (layoutCardDetails != null) layoutCardDetails.setVisibility(View.VISIBLE);
        if (cardViewClickableArea != null) cardViewClickableArea.setClickable(true);
    }

    private void updateCardAppearance(boolean isBlocked, String customName) {
        if (getContext() == null) return;

        // Custom name
        if (textCardCustomNameMain != null) {
            if (customName != null && !customName.isEmpty()) {
                textCardCustomNameMain.setText(customName);
                textCardCustomNameMain.setVisibility(View.VISIBLE);
            } else {
                textCardCustomNameMain.setVisibility(View.GONE);
            }
        }

        // Blocked overlay
        if (textCardBlockedOverlayMain != null)
            textCardBlockedOverlayMain.setVisibility(isBlocked ? View.VISIBLE : View.GONE);

        // Block quick action label / icon
        if (tvBlockActionLabel != null)
            tvBlockActionLabel.setText(isBlocked ? "Разблокировать" : "Блокировка");
        if (iconBlockAction != null)
            iconBlockAction.setImageResource(isBlocked ? R.drawable.ic_lock_open_24dp : R.drawable.ic_lock_24dp);

        // CVV reset
        if (textCardCvv != null)    { textCardCvv.setText(CVV_PLACEHOLDER); }
        if (tvCardCvvStrip != null) { tvCardCvvStrip.setText(CVV_PLACEHOLDER); }
        isCvvVisible = false;
        cvvHandler.removeCallbacks(hideCvvRunnable);
    }

    private void showCreateCardView() {
        if (layoutNoCard != null)     layoutNoCard.setVisibility(View.VISIBLE);
        if (layoutCardDetails != null) layoutCardDetails.setVisibility(View.GONE);
        cvvHandler.removeCallbacks(hideCvvRunnable);
        isCvvVisible = false;
    }

    // ── CVV toggle ───────────────────────────────────────────────────────────────

    private void toggleCvvVisibility() {
        if (getContext() == null || sharedPreferences == null) return;
        if (sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false)) {
            Toast.makeText(requireContext(), "Карта заблокирована", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isCvvVisible && actualCvv != null && !actualCvv.isEmpty()) {
            if (textCardCvv != null)    textCardCvv.setText(actualCvv);
            if (tvCardCvvStrip != null) tvCardCvvStrip.setText(actualCvv);
            isCvvVisible = true;
            cvvHandler.removeCallbacks(hideCvvRunnable);
            cvvHandler.postDelayed(hideCvvRunnable, CVV_VISIBILITY_DURATION_MS);
        }
    }

    // ── Inline block toggle ──────────────────────────────────────────────────────

    private void toggleBlockStatusInline() {
        if (sharedPreferences == null || getContext() == null) return;
        boolean isBlocked = !sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false);
        sharedPreferences.edit().putBoolean(KEY_CARD_BLOCKED, isBlocked).apply();
        Toast.makeText(requireContext(),
                isBlocked ? R.string.card_blocked_toast : R.string.card_unblocked_toast,
                Toast.LENGTH_SHORT).show();
        updateCardAppearance(isBlocked, sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, ""));
    }

    // ── Balance from API ─────────────────────────────────────────────────────────

    private void loadUserBalance() {
        if (apiService == null) return;
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfileResponse p = response.body();
                    BigDecimal balance = p.getBalance() != null ? p.getBalance() : BigDecimal.ZERO;
                    String code = p.getCurrency() != null ? p.getCurrency().toUpperCase() : "KZT";
                    try {
                        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("kk", "KZ"));
                        fmt.setCurrency(Currency.getInstance(code));
                        if ("KZT".equals(code)) { fmt.setMaximumFractionDigits(0); fmt.setMinimumFractionDigits(0); }
                        if (tvStatBalance != null) tvStatBalance.setText(fmt.format(balance));
                    } catch (Exception e) {
                        if (tvStatBalance != null)
                            tvStatBalance.setText(String.format(Locale.US, "%.0f ₸", balance));
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Balance load error", t);
            }
        });
    }

    // ── Navigation ───────────────────────────────────────────────────────────────

    private void navigate(Fragment fragment, String tag) {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    private void navigateToCardManagement() {
        if (getParentFragmentManager() == null) return;
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left,  R.anim.slide_out_right);
        tx.replace(R.id.fragment_container, new CardManagementFragment());
        tx.addToBackStack("CardFragment");
        tx.commit();
    }

    // ── Card creation ────────────────────────────────────────────────────────────

    private void createNewCard() {
        if (getContext() == null || sharedPreferences == null) return;
        if (apiService != null) {
            apiService.issueCard().enqueue(new Callback<CardResponse>() {
                @Override
                public void onResponse(@NonNull Call<CardResponse> call,
                                       @NonNull Response<CardResponse> response) {
                    if (!isAdded() || getContext() == null) return;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        CardResponse.CardData card = response.body().getCard();
                        if (card != null) {
                            saveCardToPrefs(card.getCardNumber(), card.getExpiry(), card.getCvv());
                            Toast.makeText(requireContext(), R.string.card_created_toast, Toast.LENGTH_LONG).show();
                            displayCardDetails();
                            return;
                        }
                    }
                    createCardLocally();
                }
                @Override
                public void onFailure(@NonNull Call<CardResponse> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    createCardLocally();
                }
            });
        } else {
            createCardLocally();
        }
    }

    private void saveCardToPrefs(String cardNumber, String expiryDate, String cvv) {
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putBoolean(KEY_CARD_EXISTS, true);
        e.putString(KEY_CARD_NUMBER, cardNumber);
        e.putString(KEY_CARD_EXPIRY, expiryDate);
        e.putString(KEY_CARD_CVV, cvv);
        e.putBoolean(KEY_CARD_BLOCKED, false);
        e.putString(KEY_CARD_CUSTOM_NAME, getString(R.string.default_card_name_value));
        e.apply();
    }

    private void createCardLocally() {
        String cardNumber = generateCardNumber();
        String expiryDate = generateExpiryDate();
        String cvv        = generateCvv();
        saveCardToPrefs(cardNumber, expiryDate, cvv);
        Toast.makeText(requireContext(), R.string.card_created_toast, Toast.LENGTH_LONG).show();
        displayCardDetails();
        if (apiService != null) loadUserBalance();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String formatCardNumber(String n) {
        if (n == null || n.length() != 16 || n.equals("NOT_FOUND_IN_PREFS")) return "---- ---- ---- ----";
        try {
            return n.substring(0,4) + " " + n.substring(4,8) + " " + n.substring(8,12) + " " + n.substring(12,16);
        } catch (Exception e) { return "---- ---- ---- ----"; }
    }

    private String maskCardNumber(String n) {
        if (n == null || n.length() != 16) return "•••• •••• •••• ----";
        return "•••• •••• •••• " + n.substring(12);
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("772233");
        Random r = new Random();
        for (int i = 0; i < 10; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private String generateExpiryDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 2);
        return new SimpleDateFormat("MM/yy", Locale.getDefault()).format(c.getTime());
    }

    private String generateCvv() {
        return String.valueOf(100 + new Random().nextInt(900));
    }
}
