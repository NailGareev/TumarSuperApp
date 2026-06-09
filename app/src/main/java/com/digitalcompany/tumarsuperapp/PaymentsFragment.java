package com.digitalcompany.tumarsuperapp;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.adapter.PaymentsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaymentsFragment extends Fragment {

    private RecyclerView rvCategories;
    private RecyclerView rvPayments;
    private TextView tvPaymentsTitle;
    private View layoutCategories;
    private LinearLayout layoutServices;
    private LinearLayout llFavoritesRow;
    private LinearLayout llCountryTabs;
    private View sectionCountryTabs;

    private final Map<String, List<PaymentsAdapter.Service>> categoryMap = buildCategoryMap();

    private OnBackPressedCallback backCallback;

    // Current open category (for country tab filtering)
    private String currentCategoryKey = "";
    private List<PaymentsAdapter.Service> currentServices = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payments, container, false);

        rvCategories      = view.findViewById(R.id.rv_categories);
        rvPayments        = view.findViewById(R.id.rv_payments);
        tvPaymentsTitle   = view.findViewById(R.id.tv_payments_title);
        layoutCategories  = view.findViewById(R.id.layout_categories);
        layoutServices    = view.findViewById(R.id.layout_services);
        llFavoritesRow    = view.findViewById(R.id.ll_favorites_row);
        llCountryTabs     = view.findViewById(R.id.ll_country_tabs);
        sectionCountryTabs = view.findViewById(R.id.section_country_tabs);

        setupCategoriesGrid();
        setupFavoritesRow();

        // Back button in header
        view.findViewById(R.id.btn_back_payments).setOnClickListener(v -> {
            if (layoutServices != null && layoutServices.getVisibility() == View.VISIBLE) {
                showCategories();
            } else {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Edit favorites button
        View tvEditFav = view.findViewById(R.id.tv_edit_favorites);
        if (tvEditFav != null) {
            tvEditFav.setOnClickListener(v -> {
                // Future: open favorites edit screen
            });
        }

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showCategories();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
        // Refresh favorites row each time we resume
        setupFavoritesRow();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    // ── Categories grid ───────────────────────────────────────

    private void setupCategoriesGrid() {
        List<CategoryItem> cats = new ArrayList<>();
        cats.add(new CategoryItem("📱", "Мобильная связь",      "Мобильная связь", getAccentColor("Мобильная связь"), getAccentColorLight("Мобильная связь")));
        cats.add(new CategoryItem("🏠", "Коммунальные\nуслуги", "ЖКХ",            getAccentColor("ЖКХ"),            getAccentColorLight("ЖКХ")));
        cats.add(new CategoryItem("🌐", "Домашний\nинтернет",   "Интернет",        getAccentColor("Интернет"),       getAccentColorLight("Интернет")));
        cats.add(new CategoryItem("💳", "Электронные\nкошельки","Кошельки",        getAccentColor("Кошельки"),       getAccentColorLight("Кошельки")));
        cats.add(new CategoryItem("🎮", "Игры и\nразвлечения",  "Игры",            getAccentColor("Игры"),           getAccentColorLight("Игры")));
        cats.add(new CategoryItem("✈️", "Авиа и\nтранспорт",    "Транспорт",       getAccentColor("Транспорт"),      getAccentColorLight("Транспорт")));

        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvCategories.setAdapter(new CategoriesAdapter(cats, this::openCategory));
    }

    private void setupFavoritesRow() {
        if (llFavoritesRow == null || getContext() == null) return;
        llFavoritesRow.removeAllViews();

        SharedPreferences prefs = requireContext().getSharedPreferences("payment_favorites", android.content.Context.MODE_PRIVATE);
        Set<String> favSet = prefs.getStringSet("fav_set", new HashSet<>());

        float density = getResources().getDisplayMetrics().density;
        int iconSizePx = (int)(46 * density);
        int marginPx   = (int)(12 * density);

        List<String> favList = new ArrayList<>(favSet);
        // Show max 5 favorites
        int count = Math.min(favList.size(), 5);
        for (int i = 0; i < count; i++) {
            String svcName = favList.get(i);
            // Find accent color for this service
            int accent = 0xFF6200EE;
            for (Map.Entry<String, List<PaymentsAdapter.Service>> entry : categoryMap.entrySet()) {
                for (PaymentsAdapter.Service svc : entry.getValue()) {
                    if (svc.name.equals(svcName)) {
                        accent = getAccentColor(svc.category);
                        break;
                    }
                }
            }

            final String finalSvcName = svcName;
            final int finalAccent = accent;

            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(marginPx);
            item.setLayoutParams(lp);
            item.setClickable(true);
            item.setFocusable(true);

            // Icon box
            FrameLayout iconBox = new FrameLayout(requireContext());
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSizePx, iconSizePx);
            iconBox.setLayoutParams(iconLp);

            int accentLight  = (finalAccent & 0x00FFFFFF) | 0x1A000000;
            int accentBorder = (finalAccent & 0x00FFFFFF) | 0x47000000;
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(accentLight);
            gd.setStroke((int)(1.5f * density), accentBorder);
            gd.setCornerRadius(12 * density);
            iconBox.setBackground(gd);

            android.widget.ImageView ivIcon = new android.widget.ImageView(requireContext());
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(
                    (int)(24 * density), (int)(24 * density));
            ivLp.gravity = android.view.Gravity.CENTER;
            ivIcon.setLayoutParams(ivLp);
            ivIcon.setImageResource(getServiceIconRes(svcName));
            android.graphics.PorterDuffColorFilter cf = new android.graphics.PorterDuffColorFilter(finalAccent, android.graphics.PorterDuff.Mode.SRC_IN);
            ivIcon.setColorFilter(cf);
            iconBox.addView(ivIcon);
            item.addView(iconBox);

            // Label
            TextView tvLabel = new TextView(requireContext());
            LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lblLp.topMargin = (int)(4 * density);
            tvLabel.setLayoutParams(lblLp);
            tvLabel.setTextSize(10);
            tvLabel.setTextColor(0xFF777777);
            tvLabel.setMaxWidth((int)(52 * density));
            tvLabel.setMaxLines(1);
            tvLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvLabel.setText(svcName);
            item.addView(tvLabel);

            item.setOnClickListener(v -> {
                // Find and open the service
                for (Map.Entry<String, List<PaymentsAdapter.Service>> entry : categoryMap.entrySet()) {
                    for (PaymentsAdapter.Service svc : entry.getValue()) {
                        if (svc.name.equals(finalSvcName)) {
                            openPaymentSheet(svc);
                            return;
                        }
                    }
                }
            });

            llFavoritesRow.addView(item);
        }

        // "+ Добавить" placeholder if fewer than 5
        if (count < 5) {
            LinearLayout addItem = new LinearLayout(requireContext());
            addItem.setOrientation(LinearLayout.VERTICAL);
            addItem.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(marginPx);
            addItem.setLayoutParams(lp);

            FrameLayout addBox = new FrameLayout(requireContext());
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(iconSizePx, iconSizePx);
            addBox.setLayoutParams(boxLp);
            GradientDrawable addBg = new GradientDrawable();
            addBg.setColor(0xFFFFFFFF);
            addBg.setStroke((int)(2 * density), 0xFFC9A227);
            addBg.setCornerRadius(12 * density);
            addBox.setBackground(addBg);

            TextView tvPlus = new TextView(requireContext());
            FrameLayout.LayoutParams plusLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            plusLp.gravity = android.view.Gravity.CENTER;
            tvPlus.setLayoutParams(plusLp);
            tvPlus.setTextSize(20);
            tvPlus.setTextColor(0xFFC9A227);
            tvPlus.setText("+");
            addBox.addView(tvPlus);
            addItem.addView(addBox);

            TextView tvAddLabel = new TextView(requireContext());
            LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lblLp.topMargin = (int)(4 * density);
            tvAddLabel.setLayoutParams(lblLp);
            tvAddLabel.setTextSize(10);
            tvAddLabel.setTextColor(0xFF777777);
            tvAddLabel.setText("Добавить");
            addItem.addView(tvAddLabel);
            llFavoritesRow.addView(addItem);
        }
    }

    private void openCategory(CategoryItem cat) {
        List<PaymentsAdapter.Service> services = categoryMap.get(cat.key);
        if (services == null || services.isEmpty()) return;

        currentCategoryKey = cat.key;
        currentServices    = new ArrayList<>(services);

        tvPaymentsTitle.setText(cat.displayName.replace("\n", " "));

        // Show country tabs only for Мобильная связь
        if ("Мобильная связь".equals(cat.key)) {
            sectionCountryTabs.setVisibility(View.VISIBLE);
            setupCountryTabs();
        } else {
            sectionCountryTabs.setVisibility(View.GONE);
            loadServicesList(currentServices);
        }

        layoutCategories.setVisibility(View.GONE);
        layoutServices.setVisibility(View.VISIBLE);
        backCallback.setEnabled(true);
    }

    private void setupCountryTabs() {
        if (llCountryTabs == null) return;
        llCountryTabs.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        String[] countries = {"Все", "Казахстан", "Кыргызстан"};

        for (int i = 0; i < countries.length; i++) {
            final String country = countries[i];
            final boolean isFirst = (i == 0);

            TextView chip = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, (int)(32 * density));
            lp.setMarginEnd((int)(8 * density));
            chip.setLayoutParams(lp);
            chip.setText(country);
            chip.setTextSize(12);
            chip.setPadding((int)(14*density), 0, (int)(14*density), 0);
            chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
            chip.setClickable(true);
            chip.setFocusable(true);

            if (isFirst) {
                chip.setBackground(requireContext().getDrawable(R.drawable.bg_pay_chip_country_active));
                chip.setTextColor(0xFFFFFFFF);
            } else {
                chip.setBackground(requireContext().getDrawable(R.drawable.bg_pay_chip_country));
                chip.setTextColor(0xFF6200EE);
            }

            chip.setOnClickListener(v -> {
                // Reset all chips
                for (int j = 0; j < llCountryTabs.getChildCount(); j++) {
                    View c = llCountryTabs.getChildAt(j);
                    if (c instanceof TextView) {
                        c.setBackground(requireContext().getDrawable(R.drawable.bg_pay_chip_country));
                        ((TextView) c).setTextColor(0xFF6200EE);
                    }
                }
                chip.setBackground(requireContext().getDrawable(R.drawable.bg_pay_chip_country_active));
                chip.setTextColor(0xFFFFFFFF);

                // Filter services
                List<PaymentsAdapter.Service> filtered = new ArrayList<>();
                for (PaymentsAdapter.Service svc : currentServices) {
                    if ("Все".equals(country)) {
                        filtered.add(svc);
                    } else if ("Казахстан".equals(country)) {
                        if (isKazakhstanService(svc.name)) filtered.add(svc);
                    } else if ("Кыргызстан".equals(country)) {
                        if (isKyrgyzstanService(svc.name)) filtered.add(svc);
                    }
                }
                loadServicesList(filtered);
            });

            llCountryTabs.addView(chip);
        }

        // Load all by default
        loadServicesList(currentServices);
    }

    private boolean isKazakhstanService(String name) {
        return name.contains("Activ") || name.contains("Kcell") ||
               name.contains("Beeline") && !name.contains("KG") ||
               name.contains("Tele2") || name.contains("Altel");
    }

    private boolean isKyrgyzstanService(String name) {
        return name.contains("KG") || name.contains("O!") || name.contains("MegaCom");
    }

    private void loadServicesList(List<PaymentsAdapter.Service> services) {
        rvPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPayments.setAdapter(new PaymentsAdapter(new ArrayList<>(services), this::openPaymentSheet));
    }

    private void showCategories() {
        tvPaymentsTitle.setText("Платежи и услуги");
        layoutServices.setVisibility(View.GONE);
        layoutCategories.setVisibility(View.VISIBLE);
        backCallback.setEnabled(false);
        setupFavoritesRow(); // Refresh favorites in case they changed
    }

    /** Called by MainActivity to intercept the AppBar back arrow. */
    public boolean handleNavigateUp() {
        if (layoutServices != null && layoutServices.getVisibility() == View.VISIBLE) {
            showCategories();
            return true;
        }
        return false;
    }

    // ── Payment bottom sheet ──────────────────────────────────

    private void openPaymentSheet(PaymentsAdapter.Service service) {
        if (getActivity() == null) return;
        int accentColor = getAccentColor(service.category);
        PaymentBottomSheet sheet = PaymentBottomSheet.newInstance(
                service.name, service.icon, service.category,
                service.accountLabel, service.accountHint, accentColor);
        sheet.show(getChildFragmentManager(), "payment");
    }

    // ── Accent color helpers ──────────────────────────────────

    public static int getAccentColor(String category) {
        if (category == null) return 0xFF6200EE;
        switch (category) {
            case "Мобильная связь": return 0xFF1A4A8A;
            case "ЖКХ":             return 0xFFD97222;
            case "Интернет":        return 0xFF1A8A4A;
            case "Кошельки":        return 0xFFC9A227;
            case "Игры":            return 0xFFCC2222;
            case "Транспорт":       return 0xFF6B21A8;
            default:                return 0xFF6200EE;
        }
    }

    static int getAccentColorLight(String category) {
        if (category == null) return 0x1A6200EE;
        switch (category) {
            case "Мобильная связь": return 0x1A1A4A8A;
            case "ЖКХ":             return 0x1AD97222;
            case "Интернет":        return 0x1A1A8A4A;
            case "Кошельки":        return 0x1AC9A227;
            case "Игры":            return 0x1ACC2222;
            case "Транспорт":       return 0x1A6B21A8;
            default:                return 0x1A6200EE;
        }
    }

    static int getCategoryIconRes(String key) {
        if (key == null) return R.drawable.ic_cat_mobile;
        switch (key) {
            case "Мобильная связь": return R.drawable.ic_cat_mobile;
            case "ЖКХ":             return R.drawable.ic_cat_utilities;
            case "Интернет":        return R.drawable.ic_cat_internet;
            case "Кошельки":        return R.drawable.ic_cat_wallet;
            case "Игры":            return R.drawable.ic_cat_games;
            case "Транспорт":       return R.drawable.ic_cat_transport;
            default:                return R.drawable.ic_cat_mobile;
        }
    }

    public static int getServiceIconRes(String name) {
        if (name == null) return R.drawable.ic_svc_signal;
        if (name.contains("Activ") || name.contains("Beeline") || name.contains("Tele2") || name.contains("Altel") || name.contains("O!") || name.contains("MegaCom")) return R.drawable.ic_svc_signal;
        if (name.equals("АЛСЕКО")) return R.drawable.ic_svc_electricity;
        if (name.contains("YURTA DOM")) return R.drawable.ic_svc_home;
        if (name.equals("Водоканал")) return R.drawable.ic_svc_water;
        if (name.equals("Теплоснабжение")) return R.drawable.ic_svc_heat;
        if (name.contains("Internet") || name.equals("MEGANET")) return R.drawable.ic_svc_signal;
        if (name.equals("ALMA PLUS")) return R.drawable.ic_svc_tv;
        if (name.equals("Kazakhtelecom")) return R.drawable.ic_svc_globe;
        if (name.contains("WALLET") || name.equals("QPLUS")) return R.drawable.ic_svc_wallet;
        if (name.equals("Kaspi Gold")) return R.drawable.ic_svc_bank;
        if (name.equals("STEAM") || name.equals("PlayStation") || name.equals("Xbox")) return R.drawable.ic_svc_controller;
        if (name.equals("Netflix")) return R.drawable.ic_svc_movie;
        if (name.equals("Air Astana") || name.equals("FlyArystan")) return R.drawable.ic_svc_plane;
        if (name.equals("Яндекс Такси")) return R.drawable.ic_svc_taxi;
        return R.drawable.ic_svc_signal;
    }

    static android.graphics.drawable.GradientDrawable makeIconBoxBg(
            int accentLight, int accentBorder, float cornerRadiusDp,
            android.content.res.Resources res) {
        float px = cornerRadiusDp * res.getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(accentLight);
        gd.setStroke((int)(1.5f * res.getDisplayMetrics().density), accentBorder);
        gd.setCornerRadius(px);
        return gd;
    }

    // ── Data ──────────────────────────────────────────────────

    private Map<String, List<PaymentsAdapter.Service>> buildCategoryMap() {
        Map<String, List<PaymentsAdapter.Service>> map = new LinkedHashMap<>();

        map.put("Мобильная связь", Arrays.asList(
                svc("Activ / Kcell",  "Номер телефона", "+7XXXXXXXXXX",  "📶", "Мобильная связь", "https://logo.clearbit.com/kcell.kz"),
                svc("Beeline / IZI",  "Номер телефона", "+7XXXXXXXXXX",  "🟡", "Мобильная связь", "https://logo.clearbit.com/beeline.kz"),
                svc("Tele2 / Altel",  "Номер телефона", "+7XXXXXXXXXX",  "🔵", "Мобильная связь", "https://logo.clearbit.com/tele2.kz"),
                svc("Beeline KG",     "Номер телефона", "+996XXXXXXXXX", "📡", "Мобильная связь", "https://logo.clearbit.com/beeline.kg"),
                svc("O! KG",          "Номер телефона", "+996XXXXXXXXX", "📱", "Мобильная связь", "https://logo.clearbit.com/o.kg"),
                svc("MegaCom KG",     "Номер телефона", "+996XXXXXXXXX", "📲", "Мобильная связь", "https://logo.clearbit.com/megacom.kg")
        ));

        map.put("ЖКХ", Arrays.asList(
                svc("АЛСЕКО",         "Лицевой счёт", "123456789", "💡", "ЖКХ", "https://logo.clearbit.com/alseco.kz"),
                svc("YURTA DOM",      "Лицевой счёт", "123456789", "🏘", "ЖКХ", null),
                svc("Водоканал",      "Лицевой счёт", "123456789", "🚰", "ЖКХ", null),
                svc("Теплоснабжение", "Лицевой счёт", "123456789", "🔥", "ЖКХ", null)
        ));

        map.put("Интернет", Arrays.asList(
                svc("Beeline Internet", "Номер договора", "123456789", "🌐", "Интернет", "https://logo.clearbit.com/beeline.kz"),
                svc("ALMA PLUS",        "Номер договора", "123456789", "📺", "Интернет", "https://logo.clearbit.com/almaplus.kz"),
                svc("MEGANET",          "Номер договора", "123456789", "🔗", "Интернет", "https://logo.clearbit.com/meganet.kz"),
                svc("Kazakhtelecom",    "Номер договора", "123456789", "☎️", "Интернет", "https://logo.clearbit.com/telecom.kz")
        ));

        map.put("Кошельки", Arrays.asList(
                svc("YURTA WALLET", "Номер телефона / счёта", "+7XXXXXXXXXX", "👛", "Кошельки", "https://logo.clearbit.com/yurta.kz"),
                svc("QPLUS",        "Номер счёта",             "123456789",    "💰", "Кошельки", null),
                svc("Kaspi Gold",   "Номер телефона",           "+7XXXXXXXXXX", "🏦", "Кошельки", "https://logo.clearbit.com/kaspi.kz")
        ));

        map.put("Игры", Arrays.asList(
                svc("STEAM",       "Логин Steam",       "steam_username", "🎮", "Игры", "https://logo.clearbit.com/steampowered.com"),
                svc("PlayStation", "PSN аккаунт",       "psn_id",         "🕹️", "Игры", "https://logo.clearbit.com/playstation.com"),
                svc("Xbox",        "Microsoft аккаунт", "xbox@email.com", "🎯", "Игры", "https://logo.clearbit.com/xbox.com"),
                svc("Netflix",     "Email аккаунта",    "email@mail.com", "🎬", "Игры", "https://logo.clearbit.com/netflix.com")
        ));

        map.put("Транспорт", Arrays.asList(
                svc("Air Astana",   "Номер бронирования", "XXXXXX",       "✈️", "Транспорт", "https://logo.clearbit.com/airastana.com"),
                svc("FlyArystan",   "Номер бронирования", "XXXXXX",       "🛫", "Транспорт", "https://logo.clearbit.com/flyarystan.com"),
                svc("Яндекс Такси", "Номер телефона",     "+7XXXXXXXXXX", "🚕", "Транспорт", "https://logo.clearbit.com/yandex.com")
        ));

        return map;
    }

    private PaymentsAdapter.Service svc(String name, String label, String hint,
                                        String icon, String cat, String logoUrl) {
        return new PaymentsAdapter.Service(name, label, hint, icon, cat, logoUrl);
    }

    // ── CategoryItem model ────────────────────────────────────

    static class CategoryItem {
        final String icon;
        final String displayName;
        final String key;
        final int accentColor;
        final int accentColorLight;

        CategoryItem(String icon, String displayName, String key, int accentColor, int accentColorLight) {
            this.icon             = icon;
            this.displayName      = displayName;
            this.key              = key;
            this.accentColor      = accentColor;
            this.accentColorLight = accentColorLight;
        }
    }

    // ── Categories adapter (inline) ───────────────────────────

    interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem cat);
    }

    static class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.VH> {
        private final List<CategoryItem> items;
        private final OnCategoryClickListener listener;

        CategoriesAdapter(List<CategoryItem> items, OnCategoryClickListener listener) {
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_payment_category_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryItem cat = items.get(position);
            holder.tvName.setText(cat.displayName);
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(cat));

            float density = holder.itemView.getResources().getDisplayMetrics().density;
            int accentBorder = (cat.accentColor & 0x00FFFFFF) | 0x47000000;
            int accentMedium = (cat.accentColor & 0x00FFFFFF) | 0x35000000;

            // Icon box background
            GradientDrawable iconBoxBg = new GradientDrawable();
            iconBoxBg.setColor(cat.accentColorLight);
            iconBoxBg.setStroke((int)(1.5f * density), accentBorder);
            iconBoxBg.setCornerRadius(11 * density);
            holder.flCatIconBox.setBackground(iconBoxBg);

            // Set icon drawable with tint
            int iconRes = getCategoryIconRes(cat.key);
            holder.ivCatIcon.setImageResource(iconRes);
            android.graphics.PorterDuffColorFilter cf = new android.graphics.PorterDuffColorFilter(cat.accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
            holder.ivCatIcon.setColorFilter(cf);

            // Accent bar
            GradientDrawable barBg = new GradientDrawable();
            barBg.setColor(cat.accentColor);
            barBg.setCornerRadius(2 * density);
            holder.viewCatAccentBar.setBackground(barBg);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            FrameLayout flCatIconBox;
            android.widget.ImageView ivCatIcon;
            View viewCatAccentBar;

            VH(View v) {
                super(v);
                tvName             = v.findViewById(R.id.tv_cat_name);
                flCatIconBox       = v.findViewById(R.id.fl_cat_icon_box);
                ivCatIcon          = v.findViewById(R.id.iv_cat_icon);
                viewCatAccentBar   = v.findViewById(R.id.view_cat_accent_bar);
            }
        }
    }
}
