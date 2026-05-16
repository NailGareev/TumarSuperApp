package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.adapter.PaymentsAdapter;

import java.util.ArrayList;
import java.util.List;

public class PaymentsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payments, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_payments);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new PaymentsAdapter(buildItems(), this::openPaymentSheet));

        return view;
    }

    private List<Object> buildItems() {
        List<Object> items = new ArrayList<>();

        items.add(new PaymentsAdapter.Category("📱  Мобильная связь"));
        items.add(svc("Activ / Kcell",   "Номер телефона", "+7XXXXXXXXXX", "📶", "Мобильная связь"));
        items.add(svc("Beeline / IZI",   "Номер телефона", "+7XXXXXXXXXX", "🟡", "Мобильная связь"));
        items.add(svc("Tele2 / Altel",   "Номер телефона", "+7XXXXXXXXXX", "🔵", "Мобильная связь"));
        items.add(svc("Beeline KG",      "Номер телефона", "+996XXXXXXXXX", "📡", "Мобильная связь"));
        items.add(svc("O! KG",           "Номер телефона", "+996XXXXXXXXX", "📱", "Мобильная связь"));
        items.add(svc("MegaCom KG",      "Номер телефона", "+996XXXXXXXXX", "📲", "Мобильная связь"));

        items.add(new PaymentsAdapter.Category("🏠  Коммунальные услуги"));
        items.add(svc("АЛСЕКО",          "Лицевой счёт", "123456789",  "💡", "ЖКХ"));
        items.add(svc("YURTA DOM",       "Лицевой счёт", "123456789",  "🏘", "ЖКХ"));
        items.add(svc("Водоканал",       "Лицевой счёт", "123456789",  "🚰", "ЖКХ"));
        items.add(svc("Теплоснабжение",  "Лицевой счёт", "123456789",  "🔥", "ЖКХ"));

        items.add(new PaymentsAdapter.Category("🌐  Домашний интернет"));
        items.add(svc("Beeline Internet",   "Номер договора", "123456789", "🌐", "Интернет"));
        items.add(svc("ALMA PLUS",          "Номер договора", "123456789", "📺", "Интернет"));
        items.add(svc("MEGANET",            "Номер договора", "123456789", "🔗", "Интернет"));
        items.add(svc("Kazakhtelecom",      "Номер договора", "123456789", "☎️", "Интернет"));

        items.add(new PaymentsAdapter.Category("💳  Электронные кошельки"));
        items.add(svc("YURTA WALLET", "Номер телефона / счёта", "+7XXXXXXXXXX", "👛", "Кошельки"));
        items.add(svc("QPLUS",        "Номер счёта",             "123456789",    "💰", "Кошельки"));
        items.add(svc("Kaspi Gold",   "Номер телефона",           "+7XXXXXXXXXX", "🏦", "Кошельки"));

        items.add(new PaymentsAdapter.Category("🎮  Игры и развлечения"));
        items.add(svc("STEAM",        "Логин Steam",     "steam_username", "🎮", "Игры"));
        items.add(svc("PlayStation",  "PSN аккаунт",     "psn_id",         "🕹️", "Игры"));
        items.add(svc("Xbox",         "Microsoft аккаунт", "xbox@email.com", "🎯", "Игры"));
        items.add(svc("Netflix",      "Email аккаунта",   "email@mail.com", "🎬", "Игры"));

        items.add(new PaymentsAdapter.Category("✈️  Авиа и транспорт"));
        items.add(svc("Air Astana",    "Номер бронирования", "XXXXXX", "✈️", "Транспорт"));
        items.add(svc("FlyArystan",    "Номер бронирования", "XXXXXX", "🛫", "Транспорт"));
        items.add(svc("Яндекс Такси",  "Номер телефона",     "+7XXXXXXXXXX", "🚕", "Транспорт"));

        return items;
    }

    private PaymentsAdapter.Service svc(String name, String label, String hint, String icon, String cat) {
        return new PaymentsAdapter.Service(name, label, hint, icon, cat);
    }

    private void openPaymentSheet(PaymentsAdapter.Service service) {
        if (getActivity() == null) return;
        PaymentBottomSheet sheet = PaymentBottomSheet.newInstance(
                service.name, service.icon, service.category,
                service.accountLabel, service.accountHint);
        sheet.show(getChildFragmentManager(), "payment");
    }
}
