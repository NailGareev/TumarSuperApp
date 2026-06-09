package com.digitalcompany.tumarsuperapp;

import android.content.Context; // <-- Добавлен импорт Context
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private static final String TAG = "MainActivity";

    // --- ДОБАВЛЕНЫ КОНСТАНТЫ (ДОЛЖНЫ СОВПАДАТЬ С LoginActivity!) ---
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    // -------------------------------------------------------------

    // UI Элементы
    private BottomNavigationView navView;
    private FrameLayout bottomNavContainer;
    private FrameLayout btnQrCenter;
    private MaterialToolbar topAppBar;
    private AppBarLayout appBarLayout;

    private boolean uiInitialized = false;
    private int currentTabId = R.id.navigation_home;

    // Слушатель для BottomNavigationView
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.navigation_qr) {
            openQrScanner();
            return false;
        }
        currentTabId = itemId; // update BEFORE popBackStackImmediate fires onBackStackChanged
        boolean loadSuccess = false;

        if (itemId == R.id.navigation_home) {
            selectedFragment = new HomeFragment(); loadSuccess = true;
        } else if (itemId == R.id.navigation_card) {
            selectedFragment = new CardFragment(); loadSuccess = true;
        } else if (itemId == R.id.navigation_promotions) {
            selectedFragment = new PromotionsFragment(); loadSuccess = true;
        } else if (itemId == R.id.navigation_profile) {
            selectedFragment = new ProfileFragment(); loadSuccess = true;
        }

        if (selectedFragment != null && loadSuccess) {
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            loadFragment(selectedFragment);
            updateToolbarTitle();
            updateAppBarVisibility();
            return true;
        }
        Log.w(TAG, "Фрагмент не выбран или загрузка не предполагалась для itemId: " + itemId);
        return false;
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "--- MainActivity onCreate STARTED ---"); // Лог старта
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate запущен");

        // ================= Проверка авторизации (с исправленными константами) =================
        if (!isUserLoggedIn()) {
            // Пользователь НЕ авторизован
            Log.w(TAG, "onCreate: Пользователь НЕ авторизован! Переход на LoginActivity."); // Логируем как предупреждение
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Закрываем текущую MainActivity
            return; // ВАЖНО: Прекращаем выполнение onCreate здесь
        }
        // ====================================================================================

        // Если пользователь авторизован, продолжаем инициализацию UI
        Log.d(TAG, "onCreate: Пользователь авторизован. Инициализация UI MainActivity.");
        try { // Обертка для отлова ошибок инициализации
            getSupportFragmentManager().addOnBackStackChangedListener(this);
            initializeUI(); // Вызываем инициализацию UI ТОЛЬКО если пользователь авторизован
            Log.d(TAG, "onCreate: Инициализация UI успешно вызвана.");
        } catch(Exception e) {
            Log.e(TAG, "!!! КРИТИЧЕСКАЯ ОШИБКА во время initializeUI() в onCreate !!!", e);
            Toast.makeText(this, "Ошибка инициализации главного экрана!", Toast.LENGTH_LONG).show();
            // Здесь можно решить, что делать дальше - например, разлогинить пользователя или закрыть приложение
            finish(); // Пока просто закроем Activity
        }
    }

    // ========== ИСПРАВЛЕННЫЙ МЕТОД: Используем правильные константы ==========
    private boolean isUserLoggedIn() {
        // Используем константы USER_PREFS_NAME и KEY_IS_LOGGED_IN
        SharedPreferences prefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        boolean loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false); // Используем константу ключа
        // Добавляем в лог используемые имя файла и ключ для ясности
        Log.d(TAG, "isUserLoggedIn() checked SharedPreferences ('" + USER_PREFS_NAME + "', '" + KEY_IS_LOGGED_IN + "'), result: " + loggedIn);
        return loggedIn;
    }
    // =======================================================================

    // Метод для инициализации основного пользовательского интерфейса
    private void initializeUI() {
        if (uiInitialized) {
            Log.w(TAG, "initializeUI: UI уже инициализирован, пропуск.");
            return;
        }
        Log.d(TAG, "initializeUI: Начало инициализации...");
        setContentView(R.layout.activity_main); // Проверьте валидность макета R.layout.activity_main

        Log.d(TAG, "initializeUI: Поиск View...");
        navView = findViewById(R.id.nav_view);           // Проверьте ID в макете
        bottomNavContainer = findViewById(R.id.bottom_nav_container);
        btnQrCenter = findViewById(R.id.btn_qr_center);
        topAppBar = findViewById(R.id.topAppBar);         // Проверьте ID в макете
        appBarLayout = findViewById(R.id.appBarLayout);   // Проверьте ID в макете

        // Проверка, что основные элементы найдены (Важно!)
        if (navView == null || bottomNavContainer == null || btnQrCenter == null || topAppBar == null || appBarLayout == null) {
            // Логируем, какой именно элемент не найден
            String missingViews = "";
            if (navView == null) missingViews += "navView ";
            if (bottomNavContainer == null) missingViews += "bottomNavContainer ";
            if (btnQrCenter == null) missingViews += "btnQrCenter ";
            if (topAppBar == null) missingViews += "topAppBar ";
            if (appBarLayout == null) missingViews += "appBarLayout ";
            Log.e(TAG, "!!! initializeUI: КРИТИЧЕСКАЯ ОШИБКА - View не найдены: [" + missingViews.trim() + "] !!!");
            // Выбрасываем исключение, чтобы точно увидеть сбой в Logcat
            throw new NullPointerException("Критические UI элементы [" + missingViews.trim() + "] не найдены в R.layout.activity_main. Проверьте ID в XML!");
        }
        Log.d(TAG, "initializeUI: View найдены успешно.");

        // Устанавливаем слушателей
        Log.d(TAG, "initializeUI: Установка слушателей...");
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        btnQrCenter.setOnClickListener(v -> openQrScanner());
        navView.setOnNavigationItemReselectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_qr) {
                openQrScanner();
                return;
            }
            currentTabId = id;
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            boolean isRoot = (id == R.id.navigation_home && current instanceof HomeFragment)
                    || (id == R.id.navigation_card && current instanceof CardFragment)
                    || (id == R.id.navigation_promotions && current instanceof PromotionsFragment)
                    || (id == R.id.navigation_profile && current instanceof ProfileFragment);
            if (!isRoot) {
                Fragment root = null;
                if (id == R.id.navigation_home)            { root = new HomeFragment(); }
                else if (id == R.id.navigation_card)       { root = new CardFragment(); }
                else if (id == R.id.navigation_promotions) { root = new PromotionsFragment(); }
                else if (id == R.id.navigation_profile)    { root = new ProfileFragment(); }
                if (root != null) {
                    getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    loadFragment(root);
                    updateToolbarTitle();
                    updateAppBarVisibility();
                }
            }
        });

        Log.d(TAG, "initializeUI: Настройка Toolbar...");
        setupToolbar();
        Log.d(TAG, "initializeUI: Обновление стрелки Назад...");
        shouldDisplayHomeUp();

        // Загружаем начальный фрагмент (HomeFragment)
        try { // Дополнительная обертка для отладки фрагментов
            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) { // Проверьте ID fragment_container
                Log.d(TAG, "initializeUI: Загрузка начального HomeFragment...");
                Fragment initialFragment = new HomeFragment(); // Проверьте конструктор HomeFragment
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, initialFragment) // Проверьте, что HomeFragment.onCreateView работает
                        .commit(); // Используем commit() для асинхронной загрузки
                Log.d(TAG, "initializeUI: HomeFragment commit(). Установка элемента навигации через post()...");
                // Выделяем элемент "Главная" и обновляем видимость AppBar
                navView.post(() -> {
                    if (navView != null) {
                        navView.setSelectedItemId(R.id.navigation_home);
                        updateToolbarTitle();
                        updateAppBarVisibility();
                    } else { Log.e(TAG,"Ошибка в navView.post: navView == null"); }
                });
            } else {
                Log.d(TAG, "initializeUI: Фрагмент уже существует. Обновление AppBar через post()...");
                navView.post(() -> {
                    if (navView != null) {
                        updateAppBarVisibility();
                    } else { Log.e(TAG,"Ошибка в navView.post: navView == null"); }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "!!! EXCEPTION при работе с FragmentManager или начальным фрагментом !!!", e);
            Toast.makeText(this, "Ошибка загрузки начального экрана!", Toast.LENGTH_LONG).show();
        }

        uiInitialized = true; // Ставим флаг, что UI инициализирован
        Log.d(TAG, "initializeUI: Инициализация UI завершена.");
    }

    private void setupToolbar() {
        setSupportActionBar(topAppBar);
        topAppBar.inflateMenu(R.menu.top_app_bar_menu);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_notifications) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, new NotificationsFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return false;
        });
    }

    private void updateToolbarTitle() {
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        boolean isHome = currentTabId == R.id.navigation_home
                && getSupportFragmentManager().getBackStackEntryCount() == 0;
        ab.setTitle(isHome ? "Tumar SuperApp" : "");
        if (topAppBar != null) {
            MenuItem bellItem = topAppBar.getMenu().findItem(R.id.action_notifications);
            if (bellItem != null) bellItem.setVisible(isHome);
        }
    }

    public void navigateToHome() {
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment());
    }

    private void openQrScanner() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, new QrScanFragment())
                .addToBackStack(null)
                .commit();
    }

    // Метод для загрузки фрагментов в контейнер (без изменений)
    private void loadFragment(Fragment fragment) {
        Log.d(TAG, "Загрузка фрагмента: " + fragment.getClass().getSimpleName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out
        );
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
        updateToolbarTitle();
        updateAppBarVisibility();
    }

    private void updateAppBarVisibility() {
        if (appBarLayout == null) return;
        boolean isProfileTab = currentTabId == R.id.navigation_profile;
        boolean isHomeRoot = currentTabId == R.id.navigation_home
                && getSupportFragmentManager().getBackStackEntryCount() == 0;
        boolean isCardRoot = currentTabId == R.id.navigation_card
                && getSupportFragmentManager().getBackStackEntryCount() == 0;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        boolean isPromotionsRoot = currentTabId == R.id.navigation_promotions
                && getSupportFragmentManager().getBackStackEntryCount() == 0;
        boolean isFullScreen = current instanceof TumarMarketFragment
                || current instanceof QrScanFragment
                || current instanceof CardManagementFragment
                || current instanceof PromoDetailFragment
                || current instanceof TopUpFragment
                || current instanceof TopUpSuccessFragment;
        appBarLayout.setVisibility((isProfileTab || isHomeRoot || isCardRoot || isPromotionsRoot || isFullScreen) ? View.GONE : View.VISIBLE);
    }

    public void shouldDisplayHomeUp() {
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Log.d(TAG, "Установка display home as up enabled: " + canGoBack); // Можно раскомментировать для отладки
            actionBar.setDisplayHomeAsUpEnabled(canGoBack);
        } else {
            Log.w(TAG, "ActionBar равен null в shouldDisplayHomeUp");
        }
    }

    @Override
    public void onBackPressed() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof TumarMarketFragment && ((TumarMarketFragment) f).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof PaymentsFragment && ((PaymentsFragment) f).handleNavigateUp()) {
            return true;
        }
        getSupportFragmentManager().popBackStack();
        return true;
    }
    // --- Конец обработки стека возврата ---


    public void setSystemNavVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        if (appBarLayout != null) appBarLayout.setVisibility(vis);
        if (bottomNavContainer != null) bottomNavContainer.setVisibility(vis);
    }

    public void restoreNavBars() {
        if (bottomNavContainer != null) bottomNavContainer.setVisibility(View.VISIBLE);
        updateAppBarVisibility();
    }

    // Внутренний статический класс PlaceholderFragment (оставлен без изменений)
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_TEXT = "fragment_text";
        public static PlaceholderFragment newInstance(String text) { /*...*/ PlaceholderFragment fragment = new PlaceholderFragment(); Bundle args = new Bundle(); args.putString(ARG_TEXT, text); fragment.setArguments(args); return fragment; }
        @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { /*...*/ TextView textView = new TextView(getActivity()); textView.setTextSize(20); textView.setGravity(android.view.Gravity.CENTER); if (getArguments() != null) { textView.setText(getArguments().getString(ARG_TEXT)); } setHasOptionsMenu(false); return textView; }
    }
}