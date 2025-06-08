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
    private MaterialToolbar topAppBar;
    private AppBarLayout appBarLayout;

    private boolean uiInitialized = false;

    // Слушатель для BottomNavigationView (без изменений)
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();
        boolean shouldShowAppBar = true;
        boolean loadSuccess = false;

        if (itemId == R.id.navigation_home) {
            selectedFragment = new HomeFragment(); loadSuccess = true;
        } else if (itemId == R.id.navigation_card) {
            selectedFragment = new CardFragment(); loadSuccess = true;
        } else if (itemId == R.id.navigation_promotions) {
            selectedFragment = new PromotionsFragment(); loadSuccess = true;
        } else if (itemId == R.id.navigation_profile) {
            selectedFragment = new ProfileFragment(); shouldShowAppBar = false; loadSuccess = true;
        }

        if (appBarLayout != null) {
            appBarLayout.setVisibility(shouldShowAppBar ? View.VISIBLE : View.GONE);
        } else { Log.e(TAG, "appBarLayout равен null при обработке навигации!"); }

        if (selectedFragment != null && loadSuccess) {
            loadFragment(selectedFragment); return true;
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
        topAppBar = findViewById(R.id.topAppBar);         // Проверьте ID в макете
        appBarLayout = findViewById(R.id.appBarLayout);   // Проверьте ID в макете

        // Проверка, что основные элементы найдены (Важно!)
        if (navView == null || topAppBar == null || appBarLayout == null) {
            // Логируем, какой именно элемент не найден
            String missingViews = "";
            if (navView == null) missingViews += "navView ";
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
        navView.setOnNavigationItemReselectedListener(item -> {}); // Пустой слушатель для повторных нажатий

        Log.d(TAG, "initializeUI: Настройка Toolbar...");
        setupToolbar(); // Настраиваем Toolbar
        Log.d(TAG, "initializeUI: Обновление стрелки Назад...");
        shouldDisplayHomeUp(); // Обновляем состояние стрелки "назад" при инициализации

        // Загружаем начальный фрагмент (HomeFragment)
        try { // Дополнительная обертка для отладки фрагментов
            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) { // Проверьте ID fragment_container
                Log.d(TAG, "initializeUI: Загрузка начального HomeFragment...");
                Fragment initialFragment = new HomeFragment(); // Проверьте конструктор HomeFragment
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, initialFragment) // Проверьте, что HomeFragment.onCreateView работает
                        .commit(); // Используем commit() для асинхронной загрузки
                Log.d(TAG, "initializeUI: HomeFragment commit(). Установка элемента навигации через post()...");
                // Выделяем элемент "Главная" и показываем AppBar после отрисовки
                navView.post(() -> { // Выполняем после отрисовки
                    if (navView != null && appBarLayout != null) { // Доп. проверка на null
                        navView.setSelectedItemId(R.id.navigation_home); // Проверьте ID в menu/bottom_nav_menu.xml
                        appBarLayout.setVisibility(View.VISIBLE);
                    } else { Log.e(TAG,"Ошибка в navView.post: navView или appBarLayout == null"); }
                });
            } else {
                Log.d(TAG, "initializeUI: Фрагмент уже существует. Обновление AppBar через post()...");
                // Обновляем состояние AppBar в соответствии с текущим выбранным элементом
                navView.post(() -> { // Выполняем после отрисовки
                    if (navView != null && appBarLayout != null) { // Доп. проверка на null
                        int currentItemId = navView.getSelectedItemId();
                        appBarLayout.setVisibility(currentItemId == R.id.navigation_profile ? View.GONE : View.VISIBLE); // Проверьте ID navigation_profile
                    } else { Log.e(TAG,"Ошибка в navView.post: navView или appBarLayout == null"); }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "!!! EXCEPTION при работе с FragmentManager или начальным фрагментом !!!", e);
            Toast.makeText(this, "Ошибка загрузки начального экрана!", Toast.LENGTH_LONG).show();
        }

        uiInitialized = true; // Ставим флаг, что UI инициализирован
        Log.d(TAG, "initializeUI: Инициализация UI завершена.");
    }

    // Метод для настройки Toolbar (без изменений)
    private void setupToolbar() {
        setSupportActionBar(topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> false);
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

    // --- Обработка стека возврата и навигации --- (без изменений)
    @Override
    public void onBackStackChanged() { shouldDisplayHomeUp(); }

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
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "Вызван onSupportNavigateUp, извлечение из стека возврата");
        getSupportFragmentManager().popBackStack();
        return true;
    }
    // --- Конец обработки стека возврата ---


    // Внутренний статический класс PlaceholderFragment (оставлен без изменений)
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_TEXT = "fragment_text";
        public static PlaceholderFragment newInstance(String text) { /*...*/ PlaceholderFragment fragment = new PlaceholderFragment(); Bundle args = new Bundle(); args.putString(ARG_TEXT, text); fragment.setArguments(args); return fragment; }
        @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { /*...*/ TextView textView = new TextView(getActivity()); textView.setTextSize(20); textView.setGravity(android.view.Gravity.CENTER); if (getArguments() != null) { textView.setText(getArguments().getString(ARG_TEXT)); } setHasOptionsMenu(false); return textView; }
    }
}