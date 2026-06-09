package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.CardResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardFragment extends Fragment {

    private static final String PREFS_NAME     = "CardDataPrefs";
    private static final String KEY_CARD_COUNT = "card_count";
    private static final int    MAX_CARDS      = 3;
    private static final String TAG            = "CardFragment";

    // Header
    private FrameLayout  btnAddCard;
    private FrameLayout  btnCardBell;

    // No-card state
    private LinearLayout layoutNoCard;

    // Card details state
    private LinearLayout layoutCardDetails;
    private ViewPager2   cardViewPager;
    private LinearLayout carouselDotsContainer;

    // Stats
    private TextView tvStatBalance;

    // Quick actions
    private LinearLayout actionCardTopup;
    private LinearLayout actionCardTransfer;
    private LinearLayout actionCardBlock;
    private LinearLayout actionCardSettings;
    private TextView     tvBlockActionLabel;
    private ImageView    iconBlockAction;

    // All operations
    private LinearLayout btnAllOperations;

    // Data
    private final List<CardPagerAdapter.CardEntry> cardList = new ArrayList<>();
    private CardPagerAdapter cardPagerAdapter;
    private boolean          callbackRegistered = false;

    private SharedPreferences sharedPreferences;
    private ApiService        apiService;

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (position >= 0 && position < cardList.size()) {
                updateDots(position);
                updateBlockActionForPage(position);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        btnAddCard  = view.findViewById(R.id.btn_add_card);
        btnCardBell = view.findViewById(R.id.btn_card_bell);

        layoutNoCard      = view.findViewById(R.id.layout_no_card);
        layoutCardDetails = view.findViewById(R.id.layout_card_details);

        view.findViewById(R.id.button_create_card).setOnClickListener(v -> createNewCard());

        cardViewPager         = view.findViewById(R.id.card_view_pager);
        carouselDotsContainer = view.findViewById(R.id.carousel_dots_container);

        tvStatBalance      = view.findViewById(R.id.tv_stat_balance);
        actionCardTopup    = view.findViewById(R.id.action_card_topup);
        actionCardTransfer = view.findViewById(R.id.action_card_transfer);
        actionCardBlock    = view.findViewById(R.id.action_card_block);
        actionCardSettings = view.findViewById(R.id.action_card_settings);
        tvBlockActionLabel = view.findViewById(R.id.tv_block_action_label);
        iconBlockAction    = view.findViewById(R.id.icon_block_action);
        btnAllOperations   = view.findViewById(R.id.btn_all_operations);

        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }

        setupClickListeners();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cardViewPager != null && callbackRegistered) {
            cardViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        callbackRegistered = false;
        cardPagerAdapter   = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCards();
    }

    // ── Click listeners ─────────────────────────────────────────────────────────

    private void setupClickListeners() {
        if (btnCardBell != null)
            btnCardBell.setOnClickListener(v -> navigate(new NotificationsFragment(), "notifications"));

        if (btnAddCard != null)
            btnAddCard.setOnClickListener(v -> createNewCard());

        if (actionCardTopup != null)
            actionCardTopup.setOnClickListener(v -> navigate(new TopUpFragment(), "topup"));

        if (actionCardTransfer != null)
            actionCardTransfer.setOnClickListener(v -> navigate(new TransferFragment(), "transfer"));

        if (actionCardBlock != null)
            actionCardBlock.setOnClickListener(v -> toggleBlockStatusInline());

        if (actionCardSettings != null)
            actionCardSettings.setOnClickListener(v -> {
                int page = (cardViewPager != null && !cardList.isEmpty())
                        ? cardViewPager.getCurrentItem() : 0;
                navigateToCardManagement(page);
            });

        if (btnAllOperations != null)
            btnAllOperations.setOnClickListener(v -> navigate(new HistoryFragment(), "history"));
    }

    // ── Refresh all card state ───────────────────────────────────────────────────

    private void refreshCards() {
        if (sharedPreferences == null) { showCreateCardView(); return; }

        migrateIfNeeded();

        int savedPage = (cardViewPager != null && cardPagerAdapter != null)
                ? cardViewPager.getCurrentItem() : 0;

        reloadCardsIntoList();

        if (cardList.isEmpty()) {
            showCreateCardView();
            return;
        }

        if (layoutNoCard != null)      layoutNoCard.setVisibility(View.GONE);
        if (layoutCardDetails != null) layoutCardDetails.setVisibility(View.VISIBLE);

        if (cardViewPager != null) {
            if (callbackRegistered) {
                cardViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
                callbackRegistered = false;
            }
            cardPagerAdapter = new CardPagerAdapter(cardList, this::navigateToCardManagement);
            cardViewPager.setAdapter(cardPagerAdapter);
            cardViewPager.registerOnPageChangeCallback(pageChangeCallback);
            callbackRegistered = true;

            int page = Math.max(0, Math.min(savedPage, cardList.size() - 1));
            cardViewPager.setCurrentItem(page, false);
            updateDots(page);
            updateBlockActionForPage(page);
        }

        if (btnAddCard != null)
            btnAddCard.setAlpha(cardList.size() >= MAX_CARDS ? 0.4f : 1.0f);

        if (apiService != null) loadUserBalance();
    }

    private void showCreateCardView() {
        if (layoutNoCard != null)      layoutNoCard.setVisibility(View.VISIBLE);
        if (layoutCardDetails != null) layoutCardDetails.setVisibility(View.GONE);
    }

    // ── Migration from old single-card keys ──────────────────────────────────────

    private void migrateIfNeeded() {
        if (!sharedPreferences.contains(KEY_CARD_COUNT)
                && sharedPreferences.getBoolean("card_exists", false)) {
            SharedPreferences.Editor e = sharedPreferences.edit();
            e.putString("card_0_number",      sharedPreferences.getString("card_number", ""));
            e.putString("card_0_expiry",      sharedPreferences.getString("card_expiry", ""));
            e.putString("card_0_cvv",         sharedPreferences.getString("card_cvv", ""));
            e.putBoolean("card_0_blocked",    sharedPreferences.getBoolean("card_blocked", false));
            e.putString("card_0_custom_name", sharedPreferences.getString("card_custom_name", ""));
            e.putInt(KEY_CARD_COUNT, 1);
            e.apply();
        }
    }

    // ── Load cards from prefs into list ──────────────────────────────────────────

    private void reloadCardsIntoList() {
        cardList.clear();
        if (sharedPreferences == null) return;
        int count = sharedPreferences.getInt(KEY_CARD_COUNT, 0);
        for (int i = 0; i < count; i++) {
            CardPagerAdapter.CardEntry entry = new CardPagerAdapter.CardEntry();
            entry.index      = i;
            entry.number     = sharedPreferences.getString("card_" + i + "_number", "");
            entry.expiry     = sharedPreferences.getString("card_" + i + "_expiry", "");
            entry.cvv        = sharedPreferences.getString("card_" + i + "_cvv", "");
            entry.blocked    = sharedPreferences.getBoolean("card_" + i + "_blocked", false);
            entry.customName = sharedPreferences.getString("card_" + i + "_custom_name", "");
            cardList.add(entry);
        }
    }

    // ── Carousel dots ────────────────────────────────────────────────────────────

    private void updateDots(int activePage) {
        if (carouselDotsContainer == null || getContext() == null) return;
        carouselDotsContainer.removeAllViews();
        int count = cardList.size();
        if (count <= 1) return;
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams params;
            if (i == activePage) {
                params = new LinearLayout.LayoutParams(
                        Math.round(18 * density), Math.round(7 * density));
                dot.setBackgroundResource(R.drawable.bg_dot_active);
            } else {
                params = new LinearLayout.LayoutParams(
                        Math.round(7 * density), Math.round(7 * density));
                dot.setBackgroundResource(R.drawable.bg_dot_inactive);
            }
            if (i > 0) params.setMarginStart(Math.round(6 * density));
            dot.setLayoutParams(params);
            carouselDotsContainer.addView(dot);
        }
    }

    // ── Block quick action ────────────────────────────────────────────────────────

    private void updateBlockActionForPage(int page) {
        if (page < 0 || page >= cardList.size()) return;
        boolean isBlocked = cardList.get(page).blocked;
        if (tvBlockActionLabel != null)
            tvBlockActionLabel.setText(isBlocked ? "Разблокировать" : "Блокировка");
        if (iconBlockAction != null)
            iconBlockAction.setImageResource(isBlocked
                    ? R.drawable.ic_lock_open_24dp : R.drawable.ic_lock_24dp);
    }

    // ── Inline block toggle ──────────────────────────────────────────────────────

    private void toggleBlockStatusInline() {
        if (sharedPreferences == null || getContext() == null || cardList.isEmpty()) return;
        int page = (cardViewPager != null) ? cardViewPager.getCurrentItem() : 0;
        if (page >= cardList.size()) return;
        CardPagerAdapter.CardEntry card = cardList.get(page);
        card.blocked = !card.blocked;
        sharedPreferences.edit()
                .putBoolean("card_" + card.index + "_blocked", card.blocked)
                .apply();
        Toast.makeText(requireContext(),
                card.blocked ? R.string.card_blocked_toast : R.string.card_unblocked_toast,
                Toast.LENGTH_SHORT).show();
        if (cardPagerAdapter != null) cardPagerAdapter.notifyItemChanged(page);
        updateBlockActionForPage(page);
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
                        if ("KZT".equals(code)) {
                            fmt.setMaximumFractionDigits(0);
                            fmt.setMinimumFractionDigits(0);
                        }
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

    private void navigateToCardManagement(int cardIndex) {
        if (getParentFragmentManager() == null) return;
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left,  R.anim.slide_out_right);
        tx.replace(R.id.fragment_container, CardManagementFragment.newInstance(cardIndex));
        tx.addToBackStack("CardFragment");
        tx.commit();
    }

    // ── Card creation ────────────────────────────────────────────────────────────

    private void createNewCard() {
        if (getContext() == null || sharedPreferences == null) return;
        int count = sharedPreferences.getInt(KEY_CARD_COUNT, 0);
        if (count >= MAX_CARDS) {
            Toast.makeText(requireContext(), "Максимум 3 карты", Toast.LENGTH_SHORT).show();
            return;
        }
        addCardLocally();
    }

    private void addCardLocally() {
        addCardToPrefs(generateCardNumber(), generateExpiryDate(), generateCvv());
    }

    private void addCardToPrefs(String number, String expiry, String cvv) {
        int count = sharedPreferences.getInt(KEY_CARD_COUNT, 0);
        int newIndex = count;
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putString("card_" + newIndex + "_number",      number);
        e.putString("card_" + newIndex + "_expiry",      expiry);
        e.putString("card_" + newIndex + "_cvv",         cvv);
        e.putBoolean("card_" + newIndex + "_blocked",    false);
        e.putString("card_" + newIndex + "_custom_name", "");
        e.putInt(KEY_CARD_COUNT, newIndex + 1);
        e.apply();

        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), R.string.card_created_toast, Toast.LENGTH_LONG).show();
        }

        reloadCardsIntoList();
        if (cardList.isEmpty()) return;

        if (layoutNoCard != null)      layoutNoCard.setVisibility(View.GONE);
        if (layoutCardDetails != null) layoutCardDetails.setVisibility(View.VISIBLE);

        if (cardViewPager != null) {
            if (callbackRegistered) {
                cardViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
                callbackRegistered = false;
            }
            cardPagerAdapter = new CardPagerAdapter(cardList, this::navigateToCardManagement);
            cardViewPager.setAdapter(cardPagerAdapter);
            cardViewPager.registerOnPageChangeCallback(pageChangeCallback);
            callbackRegistered = true;
            cardViewPager.setCurrentItem(newIndex, newIndex > 0);
            updateDots(newIndex);
            updateBlockActionForPage(newIndex);
        }

        if (btnAddCard != null)
            btnAddCard.setAlpha(cardList.size() >= MAX_CARDS ? 0.4f : 1.0f);

        if (apiService != null) loadUserBalance();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

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
