package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // Не забудьте импорт Log, если используете
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class CardFragment extends Fragment {

    // Константы
    private static final String PREFS_NAME = "CardDataPrefs";
    private static final String KEY_CARD_EXISTS = "card_exists";
    private static final String KEY_CARD_NUMBER = "card_number";
    private static final String KEY_CARD_EXPIRY = "card_expiry";
    private static final String KEY_CARD_CVV = "card_cvv";
    private static final String KEY_CARD_BLOCKED = "card_blocked";
    private static final String KEY_CARD_CUSTOM_NAME = "card_custom_name";

    private static final long CVV_VISIBILITY_DURATION_MS = 60 * 1000;
    private static final String CVV_PLACEHOLDER = "***";
    private static final String TAG = "CardFragment"; // Тег для логов

    // View элементы
    private LinearLayout layoutNoCard;
    private LinearLayout layoutCardDetails;
    private Button buttonCreateCard;
    private TextView textCardNumber;
    private TextView textCardExpiry;
    private TextView textCardCvv;
    private CardView cardViewClickableArea;
    private TextView textCardBlockedOverlayMain;
    private TextView textCardCustomNameMain;

    // Состояние карты и CVV
    private SharedPreferences sharedPreferences;
    private String actualCvv;
    private boolean isCvvVisible = false;

    // Handler для CVV
    private Handler cvvHandler;
    private Runnable hideCvvRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cvvHandler = new Handler(Looper.getMainLooper());
        hideCvvRunnable = () -> {
            if (textCardCvv != null) {
                textCardCvv.setText(CVV_PLACEHOLDER);
                isCvvVisible = false;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        // Находим View
        layoutNoCard = view.findViewById(R.id.layout_no_card);
        layoutCardDetails = view.findViewById(R.id.layout_card_details);
        buttonCreateCard = view.findViewById(R.id.button_create_card);
        textCardNumber = view.findViewById(R.id.text_card_number);
        textCardExpiry = view.findViewById(R.id.text_card_expiry);
        textCardCvv = view.findViewById(R.id.text_card_cvv);
        cardViewClickableArea = view.findViewById(R.id.card_view_clickable_area);
        textCardBlockedOverlayMain = view.findViewById(R.id.text_card_blocked_overlay_main);
        textCardCustomNameMain = view.findViewById(R.id.text_card_custom_name_main);

        // Получаем SharedPreferences безопасно
        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } else {
            Log.e(TAG, "getActivity() is null in onCreateView, cannot get SharedPreferences");
        }


        // Устанавливаем слушатели
        buttonCreateCard.setOnClickListener(v -> createNewCard());
        textCardCvv.setOnClickListener(v -> toggleCvvVisibility());
        cardViewClickableArea.setOnClickListener(v -> navigateToCardManagement());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Первичная проверка статуса при создании View
        checkCardStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Повторная проверка статуса при возвращении на фрагмент
        if (sharedPreferences != null && sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            String customName = sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, "");
            boolean isBlocked = sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false);
            updateCardAppearance(isBlocked, customName);
        } else {
            showCreateCardView(); // Если карты нет, показать UI создания
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очищаем Handler, чтобы избежать утечек
        cvvHandler.removeCallbacks(hideCvvRunnable);
    }

    // Проверка наличия карты и обновление UI
    private void checkCardStatus() {
        if (sharedPreferences == null) { // Проверка на null
            Log.e(TAG, "SharedPreferences not initialized in checkCardStatus");
            showCreateCardView(); // Показать UI создания, если prefs недоступны
            return;
        }
        boolean cardExists = sharedPreferences.getBoolean(KEY_CARD_EXISTS, false);
        if (cardExists) {
            displayCardDetails();
        } else {
            showCreateCardView();
        }
    }

    // Отображение деталей существующей карты
    private void displayCardDetails() {
        if (sharedPreferences == null) {
            Log.e(TAG, "SharedPreferences not initialized in displayCardDetails");
            return;
        }
        // Читаем данные
        String cardNumber = sharedPreferences.getString(KEY_CARD_NUMBER, "");
        String expiryDate = sharedPreferences.getString(KEY_CARD_EXPIRY, "");
        actualCvv = sharedPreferences.getString(KEY_CARD_CVV, "");
        boolean isBlocked = sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false);
        String customName = sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, "");

        // Заполняем текстом
        if(textCardNumber != null) textCardNumber.setText(formatCardNumber(cardNumber));
        if(textCardExpiry != null) textCardExpiry.setText(expiryDate);
        if(textCardCvv != null) textCardCvv.setText(CVV_PLACEHOLDER);
        isCvvVisible = false;

        // Обновляем внешний вид (цвет/оверлей/имя)
        updateCardAppearance(isBlocked, customName);

        // Управляем видимостью слоев
        if(layoutNoCard != null) layoutNoCard.setVisibility(View.GONE);
        if(layoutCardDetails != null) layoutCardDetails.setVisibility(View.VISIBLE);
        if(cardViewClickableArea != null) cardViewClickableArea.setClickable(true);
    }

    // Обновление внешнего вида карты (цвет, оверлей и ИМЯ)
    private void updateCardAppearance(boolean isBlocked, String customName) {
        if (getContext() == null || cardViewClickableArea == null || textCardBlockedOverlayMain == null || textCardCustomNameMain == null) {
            Log.w(TAG, "updateCardAppearance aborted: context or views are null");
            return;
        }

        // Обновляем имя
        if (customName.isEmpty()) {
            // Если имя пустое, используем имя по умолчанию (на случай если оно было стерто)
            String defaultName = getString(R.string.default_card_name_value);
            textCardCustomNameMain.setText(defaultName);
            textCardCustomNameMain.setVisibility(View.VISIBLE);
        } else {
            textCardCustomNameMain.setText(customName);
            textCardCustomNameMain.setVisibility(View.VISIBLE);
        }

        // Обновляем статус блокировки (фон и оверлей)
        if (isBlocked) {
            cardViewClickableArea.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_medium));
            textCardBlockedOverlayMain.setVisibility(View.VISIBLE);
        } else {
            cardViewClickableArea.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            textCardBlockedOverlayMain.setVisibility(View.GONE);
        }

        // Сброс видимости CVV
        if (textCardCvv != null) {
            textCardCvv.setText(CVV_PLACEHOLDER);
            isCvvVisible = false;
            if (cvvHandler != null && hideCvvRunnable != null) {
                cvvHandler.removeCallbacks(hideCvvRunnable);
            }
        }
    }


    // Отображение UI для создания карты
    private void showCreateCardView() {
        if(layoutNoCard != null) layoutNoCard.setVisibility(View.VISIBLE);
        if(layoutCardDetails != null) layoutCardDetails.setVisibility(View.GONE);
        cvvHandler.removeCallbacks(hideCvvRunnable);
        isCvvVisible = false;
        if (cardViewClickableArea != null) {
            cardViewClickableArea.setClickable(false);
        }
    }

    // Переключение видимости CVV
    private void toggleCvvVisibility() {
        if (getContext() == null || sharedPreferences == null) return;

        if (sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false)){
            Toast.makeText(requireContext(), "Карта заблокирована", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isCvvVisible) {
            if (actualCvv != null && !actualCvv.isEmpty() && textCardCvv != null) {
                textCardCvv.setText(actualCvv);
                isCvvVisible = true;
                cvvHandler.removeCallbacks(hideCvvRunnable);
                cvvHandler.postDelayed(hideCvvRunnable, CVV_VISIBILITY_DURATION_MS);
            }
        }
    }

    // Навигация к фрагменту управления картой (с анимацией)
    private void navigateToCardManagement() {
        if (getParentFragmentManager() == null) {
            Log.e(TAG, "Cannot navigate: ParentFragmentManager is null");
            return;
        }
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Устанавливаем анимации сдвига для входа/выхода
        transaction.setCustomAnimations(
                R.anim.slide_in_right,   // Новый фрагмент (управление) въезжает справа
                R.anim.slide_out_left,   // Текущий фрагмент (обзор) уезжает влево
                R.anim.slide_in_left,    // Текущий фрагмент (обзор) въезжает слева при возврате
                R.anim.slide_out_right   // Фрагмент управления уезжает вправо при возврате
        );

        transaction.replace(R.id.fragment_container, new CardManagementFragment());
        transaction.addToBackStack("CardFragment"); // Имя для стека, чтобы вернуться назад
        transaction.commit();
    }

    // Создание новой карты (с именем по умолчанию)
    private void createNewCard() {
        if (getContext() == null || sharedPreferences == null) {
            Log.e(TAG, "Cannot create card: Context or SharedPreferences is null");
            return;
        }

        String cardNumber = generateCardNumber();
        String expiryDate = generateExpiryDate();
        String cvv = generateCvv();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_CARD_EXISTS, true);
        editor.putString(KEY_CARD_NUMBER, cardNumber);
        editor.putString(KEY_CARD_EXPIRY, expiryDate);
        editor.putString(KEY_CARD_CVV, cvv);
        editor.putBoolean(KEY_CARD_BLOCKED, false);
        // Устанавливаем имя по умолчанию из ресурсов
        editor.putString(KEY_CARD_CUSTOM_NAME, getString(R.string.default_card_name_value)); // ИЗМЕНЕНО
        editor.apply(); // Используем apply для асинхронности

        Toast.makeText(requireContext(), R.string.card_created_toast, Toast.LENGTH_LONG).show();
        // Немедленно обновляем UI, чтобы показать новую карту
        displayCardDetails();
    }

    // --- Методы генерации и форматирования ---
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder("772233");
        for (int i = 0; i < 10; i++) {
            cardNumber.append(random.nextInt(10));
        }
        Log.d(TAG, "Generated Card Number: " + cardNumber.toString());
        return cardNumber.toString();
    }

    private String generateExpiryDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 5);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/yy", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    private String generateCvv() {
        Random random = new Random();
        int cvvNumber = 100 + random.nextInt(900);
        return String.valueOf(cvvNumber);
    }

    private String formatCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16 || cardNumber.equals("NOT_FOUND_IN_PREFS")) {
            Log.w(TAG, "formatCardNumber received invalid input: [" + cardNumber + "]");
            return "---- ---- ---- ----";
        }
        try {
            return cardNumber.substring(0, 4) + " " +
                    cardNumber.substring(4, 8) + " " +
                    cardNumber.substring(8, 12) + " " +
                    cardNumber.substring(12, 16);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Error formatting card number: " + cardNumber, e);
            return "---- ---- ---- ----";
        }
    }
}