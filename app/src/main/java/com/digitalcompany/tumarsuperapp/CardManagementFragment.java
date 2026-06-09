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

    private static final String PREFS_NAME         = "CardDataPrefs";
    private static final String KEY_CARD_COUNT     = "card_count";
    private static final String ARG_CARD_INDEX     = "card_index";
    private static final long   CVV_VISIBILITY_MS  = 60 * 1000;
    private static final String CVV_PLACEHOLDER    = "•••";
    private static final String TAG                = "CardMgmtFragment";

    public static CardManagementFragment newInstance(int cardIndex) {
        CardManagementFragment f = new CardManagementFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CARD_INDEX, cardIndex);
        f.setArguments(args);
        return f;
    }

    // Card index in shared prefs
    private int cardIndex = 0;

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

    // Blocked state
    private LinearLayout layoutBlockedBanner;
    private LinearLayout btnUnblockCta;

    // Actions
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

    // ── Indexed prefs key helpers ────────────────────────────────────────────────

    private String keyNumber()     { return "card_" + cardIndex + "_number"; }
    private String keyExpiry()     { return "card_" + cardIndex + "_expiry"; }
    private String keyCvv()        { return "card_" + cardIndex + "_cvv"; }
    private String keyBlocked()    { return "card_" + cardIndex + "_blocked"; }
    private String keyCustomName() { return "card_" + cardIndex + "_custom_name"; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cardIndex = getArguments() != null ? getArguments().getInt(ARG_CARD_INDEX, 0) : 0;
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

        cardViewManage         = view.findViewById(R.id.card_view_manage);
        textCardCustomName     = view.findViewById(R.id.text_card_custom_name);
        textCardNumberManage   = view.findViewById(R.id.text_card_number_manage);
        textCardBlockedOverlay = view.findViewById(R.id.text_card_blocked_overlay);
        tvManageCardSubtitle   = view.findViewById(R.id.tv_manage_card_subtitle);

        tvManageNumberFull = view.findViewById(R.id.tv_manage_number_full);
        textCardCvvManage  = view.findViewById(R.id.text_card_cvv_manage);
        btnShowCvvManage   = view.findViewById(R.id.btn_show_cvv_manage);

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
        int count = sharedPreferences != null ? sharedPreferences.getInt(KEY_CARD_COUNT, 0) : 0;
        if (cardIndex >= count) {
            closeFragment();
        } else {
            loadCardDataAndUpdateUI();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cvvHandler.removeCallbacks(hideCvvRunnable);
    }

    // ── Load data ────────────────────────────────────────────────────────────────

    private void loadCardDataAndUpdateUI() {
        if (sharedPreferences == null) { closeFragment(); return; }

        String cardNumber = sharedPreferences.getString(keyNumber(), "");
        String expiry     = sharedPreferences.getString(keyExpiry(), "");
        actualCvv         = sharedPreferences.getString(keyCvv(), "");
        currentCustomName = sharedPreferences.getString(keyCustomName(), "");
        isCardBlocked     = sharedPreferences.getBoolean(keyBlocked(), false);

        String masked = maskCard(cardNumber);
        if (textCardNumberManage != null) textCardNumberManage.setText(masked);
        if (tvManageCardSubtitle  != null) tvManageCardSubtitle.setText(masked);

        String nameDisplay = currentCustomName.isEmpty()
                ? getString(R.string.default_card_name_value) : currentCustomName;
        if (textCardCustomName != null) textCardCustomName.setText(nameDisplay);

        if (tvManageNumberFull != null) tvManageNumberFull.setText(formatCardNumber(cardNumber));
        if (textCardCvvManage  != null) textCardCvvManage.setText(CVV_PLACEHOLDER);
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

        float alpha = isCardBlocked ? 0.35f : 1.0f;
        if (cardActionRename  != null) { cardActionRename.setAlpha(alpha);  cardActionRename.setClickable(!isCardBlocked); }
        if (cardActionReissue != null) { cardActionReissue.setAlpha(alpha); cardActionReissue.setClickable(!isCardBlocked); }

        if (isCardBlocked) {
            if (iconBlockToggle    != null) iconBlockToggle.setImageResource(R.drawable.ic_lock_open_24dp);
            if (titleBlockToggle   != null) titleBlockToggle.setText(R.string.unblock_card_button);
            if (subtitleBlockToggle != null) subtitleBlockToggle.setText(R.string.unblock_card_subtitle);
        } else {
            if (iconBlockToggle    != null) iconBlockToggle.setImageResource(R.drawable.ic_lock_24dp);
            if (titleBlockToggle   != null) titleBlockToggle.setText(R.string.block_card_button);
            if (subtitleBlockToggle != null) subtitleBlockToggle.setText(R.string.block_card_subtitle);
        }

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
            cvvHandler.postDelayed(hideCvvRunnable, CVV_VISIBILITY_MS);
        }
    }

    // ── Rename – Bottom Sheet ────────────────────────────────────────────────────

    private void showRenameBottomSheet() {
        if (getContext() == null || !isAdded()) return;

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_rename_card, null);
        sheet.setContentView(sheetView);

        TextView     tvCounter = sheetView.findViewById(R.id.tv_rename_counter);
        EditText     etName    = sheetView.findViewById(R.id.et_rename_card);
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
            @Override public void afterTextChanged(Editable s) { tvCounter.setText(s.length() + " / 30"); }
        });

        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) { etName.setError("Введите название"); return; }
            saveCardName(newName);
            sheet.dismiss();
        });

        sheet.show();
    }

    private void saveCardName(String newName) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putString(keyCustomName(), newName).apply();
        currentCustomName = newName;
        if (textCardCustomName != null) textCardCustomName.setText(newName);
        if (getContext() != null)
            Toast.makeText(requireContext(),
                    getString(R.string.card_renamed_toast, newName), Toast.LENGTH_SHORT).show();
    }

    // ── Block / Reissue / Delete ─────────────────────────────────────────────────

    private void toggleBlockStatus() {
        if (sharedPreferences == null || getContext() == null) return;
        isCardBlocked = !isCardBlocked;
        sharedPreferences.edit().putBoolean(keyBlocked(), isCardBlocked).apply();
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
                .putString(keyNumber(), number)
                .putString(keyExpiry(), expiry)
                .putString(keyCvv(),    cvv)
                .putString(keyCustomName(), getString(R.string.default_card_name_value))
                .putBoolean(keyBlocked(), false)
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
                .setNegativeButton(R.string.no,  (d, w) -> d.cancel())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteCard() {
        if (sharedPreferences == null) return;
        int count = sharedPreferences.getInt(KEY_CARD_COUNT, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Shift cards after the deleted one down by one slot
        for (int i = cardIndex; i < count - 1; i++) {
            editor.putString("card_" + i + "_number",      sharedPreferences.getString("card_" + (i+1) + "_number", ""));
            editor.putString("card_" + i + "_expiry",      sharedPreferences.getString("card_" + (i+1) + "_expiry", ""));
            editor.putString("card_" + i + "_cvv",         sharedPreferences.getString("card_" + (i+1) + "_cvv", ""));
            editor.putBoolean("card_" + i + "_blocked",    sharedPreferences.getBoolean("card_" + (i+1) + "_blocked", false));
            editor.putString("card_" + i + "_custom_name", sharedPreferences.getString("card_" + (i+1) + "_custom_name", ""));
        }
        int last = count - 1;
        editor.remove("card_" + last + "_number");
        editor.remove("card_" + last + "_expiry");
        editor.remove("card_" + last + "_cvv");
        editor.remove("card_" + last + "_blocked");
        editor.remove("card_" + last + "_custom_name");
        editor.putInt(KEY_CARD_COUNT, Math.max(0, count - 1));
        editor.commit();
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
