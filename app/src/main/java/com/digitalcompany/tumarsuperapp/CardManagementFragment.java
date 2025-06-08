package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <-- Импорт для кнопки в диалоге
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class CardManagementFragment extends Fragment {

    // Константы SharedPreferences
    private static final String PREFS_NAME = "CardDataPrefs";
    private static final String KEY_CARD_EXISTS = "card_exists";
    private static final String KEY_CARD_NUMBER = "card_number";
    private static final String KEY_CARD_EXPIRY = "card_expiry";
    private static final String KEY_CARD_CVV = "card_cvv";
    private static final String KEY_CARD_BLOCKED = "card_blocked";
    private static final String KEY_CARD_CUSTOM_NAME = "card_custom_name";

    // Константы для CVV
    private static final long CVV_VISIBILITY_DURATION_MS = 60 * 1000;
    private static final String CVV_PLACEHOLDER = "***";
    private static final String TAG = "CardMgmtFragment"; // Тег для логов

    // View элементы
    private CardView cardViewManage;
    private TextView textCardNumberManage;
    private TextView textCardExpiryManage;
    private TextView textCardCvvManage;
    private TextView textCardBlockedOverlay;
    private TextView textCardCustomName;
    private CardView cardActionReissue;
    private CardView cardActionBlockToggle;
    private CardView cardActionDelete;
    private CardView cardActionRename;
    private ImageView iconBlockToggle;
    private TextView titleBlockToggle;
    private TextView subtitleBlockToggle;

    // Состояние карты
    private SharedPreferences sharedPreferences;
    private String actualCvv;
    private String currentCustomName = "";
    private boolean isCvvVisible = false;
    private boolean isCardBlocked = false;

    // Handler для CVV
    private Handler cvvHandler;
    private Runnable hideCvvRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cvvHandler = new Handler(Looper.getMainLooper());
        hideCvvRunnable = () -> {
            if (textCardCvvManage != null) {
                textCardCvvManage.setText(CVV_PLACEHOLDER);
                isCvvVisible = false;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_management, container, false);

        // Находим View карты
        cardViewManage = view.findViewById(R.id.card_view_manage);
        textCardCustomName = view.findViewById(R.id.text_card_custom_name);
        textCardNumberManage = view.findViewById(R.id.text_card_number_manage);
        textCardExpiryManage = view.findViewById(R.id.text_card_expiry_manage);
        textCardCvvManage = view.findViewById(R.id.text_card_cvv_manage);
        textCardBlockedOverlay = view.findViewById(R.id.text_card_blocked_overlay);

        // Находим CardView действий
        cardActionRename = view.findViewById(R.id.card_action_rename);
        cardActionReissue = view.findViewById(R.id.card_action_reissue);
        cardActionBlockToggle = view.findViewById(R.id.card_action_block_toggle);
        cardActionDelete = view.findViewById(R.id.card_action_delete);

        // Находим View внутри действия блокировки
        iconBlockToggle = view.findViewById(R.id.icon_block_toggle);
        titleBlockToggle = view.findViewById(R.id.title_block_toggle);
        subtitleBlockToggle = view.findViewById(R.id.subtitle_block_toggle);

        // Получаем SharedPreferences безопасно
        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } else {
            Log.e(TAG, "getActivity() returned null in onCreateView!");
        }

        // Устанавливаем слушатели
        textCardCvvManage.setOnClickListener(v -> {
            Log.d(TAG, "CVV TextView clicked");
            toggleCvvVisibility();
        });
        cardActionRename.setOnClickListener(v -> {
            Log.d(TAG, "Rename CardView clicked");
            showRenameCardDialog();
        });
        cardActionReissue.setOnClickListener(v -> {
            Log.d(TAG, "Reissue CardView clicked");
            reissueCard();
        });
        cardActionBlockToggle.setOnClickListener(v -> {
            Log.d(TAG, "Block/Unblock CardView clicked");
            toggleBlockStatus();
        });
        cardActionDelete.setOnClickListener(v -> {
            Log.d(TAG, "Delete CardView clicked");
            showDeleteConfirmationDialog();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Проверка на случай если карта была удалена пока фрагмент был в backstack
        if (sharedPreferences != null && sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            loadCardDataAndUpdateUI();
        } else {
            closeFragment(); // Если карты уже нет, закрываем фрагмент
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cvvHandler.removeCallbacks(hideCvvRunnable); // Очистка Handler'а
    }

    // Загрузка данных из SharedPreferences и обновление всего UI
    private void loadCardDataAndUpdateUI() {
        if (sharedPreferences == null) {
            Log.e(TAG, "SharedPreferences is null in loadCardDataAndUpdateUI");
            closeFragment();
            return;
        }
        if (!sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            Log.w(TAG, "Card does not exist according to SharedPreferences");
            closeFragment();
            return;
        }

        // Читаем данные
        String cardNumber = sharedPreferences.getString(KEY_CARD_NUMBER, "NOT_FOUND_IN_PREFS");
        Log.d(TAG, "Read Card Number from Prefs: [" + cardNumber + "]");
        String formattedNumber = formatCardNumber(cardNumber);
        Log.d(TAG, "Formatted Card Number: [" + formattedNumber + "]");

        String expiryDate = sharedPreferences.getString(KEY_CARD_EXPIRY, "");
        actualCvv = sharedPreferences.getString(KEY_CARD_CVV, "");
        currentCustomName = sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, "");
        isCardBlocked = sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false);

        // Отображаем имя карты
        if (textCardCustomName != null) {
            String nameToShow = currentCustomName;
            // Если имя пустое после загрузки, используем имя по умолчанию
            if (nameToShow.isEmpty() && getContext() != null) {
                nameToShow = getString(R.string.default_card_name_value);
            }
            textCardCustomName.setText(nameToShow);
            textCardCustomName.setVisibility(View.VISIBLE); // Всегда видимо, если есть карта
        } else {
            Log.e(TAG, "textCardCustomName is null!");
        }

        // Отображаем номер карты
        if (textCardNumberManage != null) {
            textCardNumberManage.setText(formattedNumber);
            Log.d(TAG, "Set card number text to TextView");
        } else {
            Log.e(TAG, "textCardNumberManage is null!");
        }

        // Отображаем остальные данные
        if(textCardExpiryManage != null) textCardExpiryManage.setText(expiryDate);
        if(textCardCvvManage != null) textCardCvvManage.setText(CVV_PLACEHOLDER);
        isCvvVisible = false;

        updateCardAppearance(); // Обновляем внешний вид карты и элемента управления блокировкой
    }

    // Обновление внешнего вида карты и элемента управления блокировкой/разблокировкой
    private void updateCardAppearance() {
        if (getContext() == null) return; // Проверка контекста

        // Обновляем статус блокировки (фон, оверлей, кнопка)
        if (isCardBlocked) {
            cardViewManage.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_medium));
            textCardBlockedOverlay.setVisibility(View.VISIBLE);
            iconBlockToggle.setImageResource(R.drawable.ic_lock_open_24dp);
            titleBlockToggle.setText(R.string.unblock_card_button);
            subtitleBlockToggle.setText(R.string.unblock_card_subtitle);
        } else {
            cardViewManage.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            textCardBlockedOverlay.setVisibility(View.GONE);
            iconBlockToggle.setImageResource(R.drawable.ic_lock_24dp);
            titleBlockToggle.setText(R.string.block_card_button);
            subtitleBlockToggle.setText(R.string.block_card_subtitle);
        }

        // Сброс видимости CVV
        if (textCardCvvManage != null) {
            textCardCvvManage.setText(CVV_PLACEHOLDER);
            isCvvVisible = false;
            cvvHandler.removeCallbacks(hideCvvRunnable);
        }
    }

    // Переключение видимости CVV
    private void toggleCvvVisibility() {
        if (getContext() == null) return;

        // Не показываем CVV для заблокированной карты
        if (isCardBlocked) {
            Toast.makeText(requireContext(), "Карта заблокирована", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isCvvVisible) {
            if (actualCvv != null && !actualCvv.isEmpty() && textCardCvvManage != null) {
                textCardCvvManage.setText(actualCvv);
                isCvvVisible = true;
                cvvHandler.removeCallbacks(hideCvvRunnable);
                cvvHandler.postDelayed(hideCvvRunnable, CVV_VISIBILITY_DURATION_MS);
            }
        }
    }

    // Показ диалога для переименования (с валидацией)
    private void showRenameCardDialog() {
        Log.d(TAG, "Attempting to show Rename Dialog...");
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.rename_card_dialog_title);

            // Контейнер и EditText
            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            // Используем dimension ресурс для отступов (если он есть, иначе можно задать в dp)
            int margin = 16; // Значение по умолчанию
            try {
                margin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
            } catch (Exception e) { Log.w(TAG, "Cannot find dimen activity_horizontal_margin, using default 16");}
            if (margin <= 0) margin = 16; // Запасной вариант
            params.setMargins(margin, margin / 2, margin, margin / 2);

            final EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.setHint(R.string.rename_card_dialog_hint);
            // Если текущее имя - это имя по умолчанию, показываем пустое поле для удобства
            String currentNameDisplay = currentCustomName;
            if (currentNameDisplay.equals(getString(R.string.default_card_name_value))) {
                currentNameDisplay = "";
                input.setHint(R.string.default_card_name_value); // Подсказка будет именем по умолчанию
            }
            input.setText(currentNameDisplay);
            input.setLayoutParams(params);

            container.addView(input);
            builder.setView(container);

            // Устанавливаем кнопки, Positive с null listener для последующей валидации
            builder.setPositiveButton(R.string.save, null);
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

            // Создаем и показываем диалог
            final AlertDialog dialog = builder.create();
            dialog.show();

            // Переопределяем слушатель для Positive кнопки ПОСЛЕ показа диалога
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newName = input.getText().toString().trim();
                if (newName.isEmpty()) {
                    // Показываем ошибку, диалог не закрываем
                    input.setError(getString(R.string.error_card_name_empty));
                    Log.w(TAG, "Rename validation failed: Empty name.");
                } else {
                    // Все ОК - сохраняем и закрываем
                    input.setError(null); // Убираем ошибку, если была
                    saveCardName(newName);
                    dialog.dismiss(); // Закрываем диалог
                }
            });

            Log.d(TAG, "Rename Dialog shown.");

        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot show rename dialog, fragment not attached or invalid context?", e);
            if (getActivity() != null) Toast.makeText(getActivity().getApplicationContext(), "Не удалось открыть диалог", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing rename dialog", e);
            if (getActivity() != null) Toast.makeText(getActivity().getApplicationContext(), "Ошибка при открытии диалога", Toast.LENGTH_SHORT).show();
        }
    }


    // Сохранение нового имени карты
    private void saveCardName(String newName) {
        Log.d(TAG, "saveCardName called with name: " + newName);
        if (sharedPreferences == null) {
            Log.e(TAG,"saveCardName aborted: SharedPreferences is null.");
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CARD_CUSTOM_NAME, newName);
        editor.apply();

        currentCustomName = newName;

        // Обновляем отображение имени на карте
        if (textCardCustomName != null) {
            if (currentCustomName.isEmpty()) {
                // Если сохранили пустое (хотя не должны из-за валидации), покажем имя по умолчанию
                textCardCustomName.setText(getString(R.string.default_card_name_value));
            } else {
                textCardCustomName.setText(currentCustomName);
            }
            textCardCustomName.setVisibility(View.VISIBLE); // Имя всегда видимо
        }
        if (getContext() != null) {
            Toast.makeText(requireContext(), getString(R.string.card_renamed_toast, newName), Toast.LENGTH_SHORT).show();
        }
    }

    // Перевыпуск карты (с именем по умолчанию)
    private void reissueCard() {
        Log.d(TAG, "reissueCard called.");
        if (sharedPreferences == null || getContext() == null) {
            Log.w(TAG, "reissueCard aborted: SharedPreferences or Context is null.");
            return;
        }
        String newCardNumber = generateCardNumber();
        String newExpiryDate = generateExpiryDate();
        String newCvv = generateCvv();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CARD_NUMBER, newCardNumber);
        editor.putString(KEY_CARD_EXPIRY, newExpiryDate);
        editor.putString(KEY_CARD_CVV, newCvv);
        // Устанавливаем имя по умолчанию из ресурсов при перевыпуске
        editor.putString(KEY_CARD_CUSTOM_NAME, getString(R.string.default_card_name_value)); // ИЗМЕНЕНО
        editor.putBoolean(KEY_CARD_BLOCKED, false);
        editor.apply();

        Toast.makeText(requireContext(), R.string.card_reissued_toast, Toast.LENGTH_SHORT).show();
        loadCardDataAndUpdateUI(); // Обновляем весь UI
    }

    // Переключение статуса блокировки
    private void toggleBlockStatus() {
        Log.d(TAG, "toggleBlockStatus called. Current state (isCardBlocked): " + isCardBlocked);
        if (sharedPreferences == null || getContext() == null) {
            Log.w(TAG, "toggleBlockStatus aborted: SharedPreferences or Context is null.");
            return;
        }
        isCardBlocked = !isCardBlocked;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_CARD_BLOCKED, isCardBlocked);
        editor.apply();

        Log.d(TAG, "New blocked status saved: " + isCardBlocked);

        if(getContext() != null){
            Toast.makeText(requireContext(),
                    isCardBlocked ? R.string.card_blocked_toast : R.string.card_unblocked_toast,
                    Toast.LENGTH_SHORT).show();
        }

        updateCardAppearance(); // Обновляем вид карты и элемента управления блокировкой
        Log.d(TAG, "toggleBlockStatus finished.");
    }

    // Показ диалога подтверждения удаления
    private void showDeleteConfirmationDialog() {
        Log.d(TAG, "Attempting to show Delete Confirmation Dialog...");
        try {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_card_confirmation_title)
                    .setMessage(R.string.delete_card_confirmation_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        Log.d(TAG, "Delete confirmation: YES clicked.");
                        deleteCard();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        Log.d(TAG, "Delete confirmation: NO clicked.");
                        dialog.cancel();
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            Log.d(TAG, "Delete Dialog shown (or at least builder.show() called).");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot show delete dialog, fragment not attached or invalid context?", e);
            if (getActivity() != null) Toast.makeText(getActivity().getApplicationContext(), "Не удалось открыть диалог", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog", e);
            if (getActivity() != null) Toast.makeText(getActivity().getApplicationContext(), "Ошибка при открытии диалога", Toast.LENGTH_SHORT).show();
        }
    }

    // Удаление карты
    private void deleteCard() {
        Log.d(TAG, "deleteCard called.");
        if (sharedPreferences == null) {
            Log.e(TAG, "deleteCard aborted: SharedPreferences is null.");
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CARD_NUMBER);
        editor.remove(KEY_CARD_EXPIRY);
        editor.remove(KEY_CARD_CVV);
        editor.remove(KEY_CARD_BLOCKED);
        editor.remove(KEY_CARD_CUSTOM_NAME);
        editor.putBoolean(KEY_CARD_EXISTS, false);
        boolean success = editor.commit(); // Используем commit для синхронности
        Log.d(TAG, "SharedPreferences commit success: " + success);

        if (getContext() != null) {
            Toast.makeText(requireContext(), R.string.card_deleted_toast, Toast.LENGTH_SHORT).show();
        }

        closeFragment(); // Закрываем фрагмент после удаления
    }

    // Безопасное закрытие фрагмента
    private void closeFragment() {
        Log.d(TAG, "Attempting to close fragment (popBackStackImmediate)");
        if (getParentFragmentManager() != null) {
            try {
                boolean popped = getParentFragmentManager().popBackStackImmediate();
                Log.d(TAG, "popBackStackImmediate result: " + popped);
                if (!popped) { // Если immediate не сработал, пробуем обычный
                    getParentFragmentManager().popBackStack();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG,"popBackStack failed: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "getParentFragmentManager is null in closeFragment");
        }
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