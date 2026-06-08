package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class CardManagementFragment extends Fragment {

    private static final String PREFS_NAME           = "CardDataPrefs";
    private static final String KEY_CARD_EXISTS      = "card_exists";
    private static final String KEY_CARD_NUMBER      = "card_number";
    private static final String KEY_CARD_EXPIRY      = "card_expiry";
    private static final String KEY_CARD_CVV         = "card_cvv";
    private static final String KEY_CARD_BLOCKED     = "card_blocked";
    private static final String KEY_CARD_CUSTOM_NAME = "card_custom_name";

    private static final long   CVV_VISIBILITY_DURATION_MS = 60 * 1000;
    private static final String CVV_PLACEHOLDER = "•••";
    private static final String TAG = "CardMgmtFragment";

    // Header
    private FrameLayout btnManageBack;

    // Card visual
    private CardView cardViewManage;
    private TextView textCardCustomName;
    private TextView textCardNumberManage;
    private View     textCardBlockedOverlay;

    // Detail strip
    private TextView    tvManageNumberFull;
    private TextView    textCardCvvManage;
    private FrameLayout btnShowCvvManage;

    // Blocked state views
    private LinearLayout layoutBlockedBanner;
    private LinearLayout btnUnblockCta;

    // Action items
    private LinearLayout cardActionRename;
    private LinearLayout cardActionReissue;
    private LinearLayout cardActionBlockToggle;
    private LinearLayout cardActionDelete;
    private ImageView    iconBlockToggle;
    private TextView     titleBlockToggle;
    private TextView     subtitleBlockToggle;

    // Header subtitle
    private TextView tvManageCardSubtitle;

    private SharedPreferences sharedPreferences;
    private String  actualCvv;
    private String  currentCustomName = "";
    private boolean isCvvVisible  = false;
    private boolean isCardBlocked = false;

    private Handler  cvvHandler;
    private Runnable hideCvvRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cvvHandler = new Handler(Looper.getMainLooper());
        hideCvvRunnable = () -> {
            if (textCardCvvManage != null) textCardCvvManage.setText(CVV_PLACEHOLDER);
            isCvvVisible = false;
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_management, container, false);

        btnManageBack = view.findViewById(R.id.btn_manage_back);

        cardViewManage        = view.findViewById(R.id.card_view_manage);
        textCardCustomName    = view.findViewById(R.id.text_card_custom_name);
        textCardNumberManage  = view.findViewById(R.id.text_card_number_manage);
        textCardBlockedOverlay = view.findViewById(R.id.text_card_blocked_overlay);
        tvManageCardSubtitle  = view.findViewById(R.id.tv_manage_card_subtitle);

        tvManageNumberFull  = view.findViewById(R.id.tv_manage_number_full);
        textCardCvvManage   = view.findViewById(R.id.text_card_cvv_manage);
        btnShowCvvManage    = view.findViewById(R.id.btn_show_cvv_manage);

        layoutBlockedBanner = view.findViewById(R.id.layout_blocked_banner);
        btnUnblockCta       = view.findViewById(R.id.btn_unblock_cta);

        cardActionRename      = view.findViewById(R.id.card_action_rename);
        cardActionReissue     = view.findViewById(R.id.card_action_reissue);
        cardActionBlockToggle = view.findViewById(R.id.card_action_block_toggle);
        cardActionDelete      = view.findViewById(R.id.card_action_delete);
        iconBlockToggle   = view.findViewById(R.id.icon_block_toggle);
        titleBlockToggle  = view.findViewById(R.id.title_block_toggle);
        subtitleBlockToggle = view.findViewById(R.id.subtitle_block_toggle);

        if (getActivity() != null)
            sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        btnManageBack.setOnClickListener(v -> closeFragment());
        textCardCvvManage.setOnClickListener(v -> toggleCvvVisibility());
        btnShowCvvManage.setOnClickListener(v -> toggleCvvVisibility());
        cardActionRename.setOnClickListener(v -> showRenameBottomSheet());
        cardActionReissue.setOnClickListener(v -> reissueCard());
        cardActionBlockToggle.setOnClickListener(v -> toggleBlockStatus());
        btnUnblockCta.setOnClickListener(v -> toggleBlockStatus());
        cardActionDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (sharedPreferences != null && sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            loadCardDataAndUpdateUI();
        } else {
            closeFragment();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cvvHandler.removeCallbacks(hideCvvRunnable);
    }

    // ── Load data ────────────────────────────────────────────────────────────────

    private void loadCardDataAndUpdateUI() {
        if (sharedPreferences == null || !sharedPreferences.getBoolean(KEY_CARD_EXISTS, false)) {
            closeFragment();
            return;
        }

        String cardNumber = sharedPreferences.getString(KEY_CARD_NUMBER, "");
        String expiry     = sharedPreferences.getString(KEY_CARD_EXPIRY, "");
        actualCvv         = sharedPreferences.getString(KEY_CARD_CVV, "");
        currentCustomName = sharedPreferences.getString(KEY_CARD_CUSTOM_NAME, "");
        isCardBlocked     = sharedPreferences.getBoolean(KEY_CARD_BLOCKED, false);

        // Card visual: compact masked number
        String masked = maskCard(cardNumber);
        if (textCardNumberManage != null) textCardNumberManage.setText(masked);
        if (tvManageCardSubtitle != null) tvManageCardSubtitle.setText(masked);

        // Card name
        String nameDisplay = currentCustomName.isEmpty()
                ? getString(R.string.default_card_name_value) : currentCustomName;
        if (textCardCustomName != null) textCardCustomName.setText(nameDisplay);

        // Full number in detail strip
        if (tvManageNumberFull != null) tvManageNumberFull.setText(formatCardNumber(cardNumber));

        // Expiry shown in compact card header
        if (textCardCvvManage != null) textCardCvvManage.setText(CVV_PLACEHOLDER);
        isCvvVisible = false;

        updateCardAppearance();
    }

    // ── Appearance ───────────────────────────────────────────────────────────────

    private void updateCardAppearance() {
        if (getContext() == null) return;

        if (textCardBlockedOverlay != null)
            textCardBlockedOverlay.setVisibility(isCardBlocked ? View.VISIBLE : View.GONE);

        if (layoutBlockedBanner != null)
            layoutBlockedBanner.setVisibility(isCardBlocked ? View.VISIBLE : View.GONE);

        if (btnUnblockCta != null)
            btnUnblockCta.setVisibility(isCardBlocked ? View.VISIBLE : View.GONE);

        // Dim rename/reissue when blocked
        float alpha = isCardBlocked ? 0.35f : 1.0f;
        if (cardActionRename  != null) cardActionRename.setAlpha(alpha);
        if (cardActionReissue != null) cardActionReissue.setAlpha(alpha);
        if (cardActionRename  != null) cardActionRename.setClickable(!isCardBlocked);
        if (cardActionReissue != null) cardActionReissue.setClickable(!isCardBlocked);

        // Block/Unblock toggle item
        if (isCardBlocked) {
            if (iconBlockToggle != null)
                iconBlockToggle.setImageResource(R.drawable.ic_lock_open_24dp);
            if (titleBlockToggle != null)
                titleBlockToggle.setText(R.string.unblock_card_button);
            if (subtitleBlockToggle != null)
                subtitleBlockToggle.setText(R.string.unblock_card_subtitle);
        } else {
            if (iconBlockToggle != null)
                iconBlockToggle.setImageResource(R.drawable.ic_lock_24dp);
            if (titleBlockToggle != null)
                titleBlockToggle.setText(R.string.block_card_button);
            if (subtitleBlockToggle != null)
                subtitleBlockToggle.setText(R.string.block_card_subtitle);
        }

        // Reset CVV
        if (textCardCvvManage != null) textCardCvvManage.setText(CVV_PLACEHOLDER);
        isCvvVisible = false;
        cvvHandler.removeCallbacks(hideCvvRunnable);
    }

    // ── CVV ──────────────────────────────────────────────────────────────────────

    private void toggleCvvVisibility() {
        if (getContext() == null) return;
        if (isCardBlocked) {
            Toast.makeText(requireContext(), "Карта заблокирована", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isCvvVisible && actualCvv != null && !actualCvv.isEmpty()) {
            if (textCardCvvManage != null) textCardCvvManage.setText(actualCvv);
            isCvvVisible = true;
            cvvHandler.removeCallbacks(hideCvvRunnable);
            cvvHandler.postDelayed(hideCvvRunnable, CVV_VISIBILITY_DURATION_MS);
        }
    }

    // ── Rename – Bottom Sheet ────────────────────────────────────────────────────

    private void showRenameBottomSheet() {
        if (getContext() == null || !isAdded()) return;

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_rename_card, null);
        sheet.setContentView(sheetView);

        TextView    tvCounter  = sheetView.findViewById(R.id.tv_rename_counter);
        EditText    etName     = sheetView.findViewById(R.id.et_rename_card);
        LinearLayout btnCancel = sheetView.findViewById(R.id.btn_rename_cancel);
        LinearLayout btnSave   = sheetView.findViewById(R.id.btn_rename_save);

        String nameDisplay = currentCustomName.isEmpty()
                ? getString(R.string.default_card_name_value) : currentCustomName;
        etName.setText(nameDisplay);
        etName.setSelection(nameDisplay.length());
        tvCounter.setText(nameDisplay.length() + " / 30");

        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                tvCounter.setText(s.length() + " / 30");
            }
        });

        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                etName.setError("Введите название");
                return;
            }
            saveCardName(newName);
            sheet.dismiss();
        });

        sheet.show();
    }

    private void saveCardName(String newName) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putString(KEY_CARD_CUSTOM_NAME, newName).apply();
        currentCustomName = newName;
        if (textCardCustomName != null) textCardCustomName.setText(newName);
        if (getContext() != null)
            Toast.makeText(requireContext(),
                    getString(R.string.card_renamed_toast, newName), Toast.LENGTH_SHORT).show();
    }

    // ── Block / Reissue / Delete ──────────────────────────────────────────────────

    private void toggleBlockStatus() {
        if (sharedPreferences == null || getContext() == null) return;
        isCardBlocked = !isCardBlocked;
        sharedPreferences.edit().putBoolean(KEY_CARD_BLOCKED, isCardBlocked).apply();
        Toast.makeText(requireContext(),
                isCardBlocked ? R.string.card_blocked_toast : R.string.card_unblocked_toast,
                Toast.LENGTH_SHORT).show();
        updateCardAppearance();
    }

    private void reissueCard() {
        if (sharedPreferences == null || getContext() == null || isCardBlocked) return;
        String number = generateCardNumber();
        String expiry = generateExpiryDate();
        String cvv    = generateCvv();
        sharedPreferences.edit()
                .putString(KEY_CARD_NUMBER, number)
                .putString(KEY_CARD_EXPIRY, expiry)
                .putString(KEY_CARD_CVV, cvv)
                .putString(KEY_CARD_CUSTOM_NAME, getString(R.string.default_card_name_value))
                .putBoolean(KEY_CARD_BLOCKED, false)
                .apply();
        Toast.makeText(requireContext(), R.string.card_reissued_toast, Toast.LENGTH_SHORT).show();
        loadCardDataAndUpdateUI();
    }

    private void showDeleteConfirmationDialog() {
        if (getContext() == null || !isAdded()) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_card_confirmation_title)
                .setMessage(R.string.delete_card_confirmation_message)
                .setPositiveButton(R.string.yes, (d, w) -> deleteCard())
                .setNegativeButton(R.string.no, (d, w) -> d.cancel())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteCard() {
        if (sharedPreferences == null) return;
        sharedPreferences.edit()
                .remove(KEY_CARD_NUMBER).remove(KEY_CARD_EXPIRY)
                .remove(KEY_CARD_CVV).remove(KEY_CARD_BLOCKED)
                .remove(KEY_CARD_CUSTOM_NAME)
                .putBoolean(KEY_CARD_EXISTS, false)
                .commit();
        if (getContext() != null)
            Toast.makeText(requireContext(), R.string.card_deleted_toast, Toast.LENGTH_SHORT).show();
        closeFragment();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void closeFragment() {
        if (getParentFragmentManager() != null) {
            try {
                if (!getParentFragmentManager().popBackStackImmediate())
                    getParentFragmentManager().popBackStack();
            } catch (IllegalStateException e) {
                Log.e(TAG, "popBackStack failed", e);
            }
        }
    }

    private String maskCard(String n) {
        if (n == null || n.length() != 16) return "···· ···· ···· ----";
        return "···· ···· ···· " + n.substring(12);
    }

    private String formatCardNumber(String n) {
        if (n == null || n.length() != 16) return "---- ---- ---- ----";
        try {
            return n.substring(0,4)+" "+n.substring(4,8)+" "+n.substring(8,12)+" "+n.substring(12,16);
        } catch (Exception e) { return "---- ---- ---- ----"; }
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("772233");
        Random r = new Random();
        for (int i = 0; i < 10; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private String generateExpiryDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 5);
        return new SimpleDateFormat("MM/yy", Locale.getDefault()).format(c.getTime());
    }

    private String generateCvv() {
        return String.valueOf(100 + new Random().nextInt(900));
    }
}
