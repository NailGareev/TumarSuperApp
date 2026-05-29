package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentsFragment extends Fragment {

    private RecyclerView rvCategories;
    private LinearLayout llServices;
    private RecyclerView rvPayments;
    private TextView tvSelectedCategory;

    private final Map<String, List<PaymentsAdapter.Service>> categoryMap = buildCategoryMap();

    private OnBackPressedCallback backCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payments, container, false);

        rvCategories      = view.findViewById(R.id.rv_categories);
        llServices        = view.findViewById(R.id.ll_services);
        rvPayments        = view.findViewById(R.id.rv_payments);
        tvSelectedCategory = view.findViewById(R.id.tv_selected_category);

        setupCategoriesGrid();

        view.findViewById(R.id.ll_services_back).setOnClickListener(v -> showCategories());

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showCategories();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

        return view;
    }

    // ── Categories grid ───────────────────────────────────────

    private void setupCategoriesGrid() {
        List<CategoryItem> cats = new ArrayList<>();
        cats.add(new CategoryItem("📱", "Мобильная связь",     "Мобильная связь"));
        cats.add(new CategoryItem("🏠", "Коммунальные\nуслуги", "ЖКХ"));
        cats.add(new CategoryItem("🌐", "Домашний\nинтернет",  "Интернет"));
        cats.add(new CategoryItem("💳", "Электронные\nкошельки","Кошельки"));
        cats.add(new CategoryItem("🎮", "Игры и\nразвлечения", "Игры"));
        cats.add(new CategoryItem("✈️", "Авиа и\nтранспорт",   "Транспорт"));

        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvCategories.setAdapter(new CategoriesAdapter(cats, this::openCategory));
    }

    private void openCategory(CategoryItem cat) {
        List<PaymentsAdapter.Service> services = categoryMap.get(cat.key);
        if (services == null || services.isEmpty()) return;

        tvSelectedCategory.setText(cat.icon + "  " + cat.displayName.replace("\n", " "));

        rvPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPayments.setAdapter(new PaymentsAdapter(
                new ArrayList<>(services), this::openPaymentSheet));

        rvCategories.setVisibility(View.GONE);
        llServices.setVisibility(View.VISIBLE);
        backCallback.setEnabled(true);
    }

    private void showCategories() {
        llServices.setVisibility(View.GONE);
        rvCategories.setVisibility(View.VISIBLE);
        backCallback.setEnabled(false);
    }

    // ── Payment bottom sheet ──────────────────────────────────

    private void openPaymentSheet(PaymentsAdapter.Service service) {
        if (getActivity() == null) return;
        PaymentBottomSheet sheet = PaymentBottomSheet.newInstance(
                service.name, service.icon, service.category,
                service.accountLabel, service.accountHint);
        sheet.show(getChildFragmentManager(), "payment");
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
        CategoryItem(String icon, String displayName, String key) {
            this.icon = icon;
            this.displayName = displayName;
            this.key = key;
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
            this.items = items;
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
            holder.tvIcon.setText(cat.icon);
            holder.tvName.setText(cat.displayName);
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(cat));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName;
            VH(View v) {
                super(v);
                tvIcon = v.findViewById(R.id.tv_cat_icon);
                tvName = v.findViewById(R.id.tv_cat_name);
            }
        }
    }
}
