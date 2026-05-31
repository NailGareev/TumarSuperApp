package com.digitalcompany.tumarsuperapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TourSearchFragment extends Fragment {

    // ── Departure cities ──────────────────────────────────────────────────
    // {Russian name, IATA (for display), country}
    private static final String[][] DEPARTURE_CITIES = {
        {"Алматы",           "ALA", "Казахстан"},
        {"Астана",           "NQZ", "Казахстан"},
        {"Шымкент",          "CIT", "Казахстан"},
        {"Актобе",           "AKX", "Казахстан"},
        {"Актау",            "SCO", "Казахстан"},
        {"Атырау",           "GUW", "Казахстан"},
        {"Усть-Каменогорск", "UKK", "Казахстан"},
        {"Уральск",          "URA", "Казахстан"},
        {"Павлодар",         "PWQ", "Казахстан"},
        {"Бишкек",           "FRU", "Кыргызстан"},
        {"Ош",               "OSS", "Кыргызстан"},
        {"Ташкент",          "TAS", "Узбекистан"},
        {"Самарканд",        "SKD", "Узбекистан"},
        {"Душанбе",          "DYU", "Таджикистан"},
        {"Ашхабад",          "ASB", "Туркменистан"},
        {"Баку",             "GYD", "Азербайджан"},
        {"Тбилиси",          "TBS", "Грузия"},
        {"Ереван",           "EVN", "Армения"},
        {"Минск",            "MSQ", "Беларусь"},
        {"Москва",           "MOW", "Россия"},
        {"Санкт-Петербург",  "LED", "Россия"},
        {"Новосибирск",      "OVB", "Россия"},
        {"Екатеринбург",     "SVX", "Россия"},
        {"Казань",           "KZN", "Россия"},
        {"Краснодар",        "KRR", "Россия"},
        {"Сочи",             "AER", "Россия"},
        {"Стамбул",          "IST", "Турция"},
        {"Дубай",            "DXB", "ОАЭ"},
    };

    // ── Destinations: {Russian name, English Booking.com term, category} ─
    private static final String[][] DESTINATIONS = {
        // Turkey
        {"Турция",           "Turkey",            "Страна"},
        {"Анталья",          "Antalya, Turkey",   "Курорт · Турция"},
        {"Кемер",            "Kemer, Turkey",     "Курорт · Турция"},
        {"Белек",            "Belek, Turkey",     "Курорт · Турция"},
        {"Сиде",             "Side, Turkey",      "Курорт · Турция"},
        {"Алания",           "Alanya, Turkey",    "Курорт · Турция"},
        {"Мармарис",         "Marmaris, Turkey",  "Курорт · Турция"},
        {"Бодрум",           "Bodrum, Turkey",    "Курорт · Турция"},
        {"Стамбул",          "Istanbul, Turkey",  "Город · Турция"},
        // UAE
        {"ОАЭ",              "United Arab Emirates", "Страна"},
        {"Дубай",            "Dubai",             "Город · ОАЭ"},
        {"Абу-Даби",         "Abu Dhabi",         "Город · ОАЭ"},
        {"Шарджа",           "Sharjah",           "Город · ОАЭ"},
        // Egypt
        {"Египет",           "Egypt",             "Страна"},
        {"Шарм-эш-Шейх",     "Sharm El Sheikh",   "Курорт · Египет"},
        {"Хургада",          "Hurghada",          "Курорт · Египет"},
        {"Каир",             "Cairo, Egypt",      "Город · Египет"},
        {"Марса-Алам",       "Marsa Alam, Egypt", "Курорт · Египет"},
        // Greece
        {"Греция",           "Greece",            "Страна"},
        {"Крит",             "Crete, Greece",     "Остров · Греция"},
        {"Родос",            "Rhodes, Greece",    "Остров · Греция"},
        {"Корфу",            "Corfu, Greece",     "Остров · Греция"},
        {"Афины",            "Athens, Greece",    "Город · Греция"},
        {"Санторини",        "Santorini, Greece", "Остров · Греция"},
        {"Миконос",          "Mykonos, Greece",   "Остров · Греция"},
        // Spain
        {"Испания",          "Spain",             "Страна"},
        {"Барселона",        "Barcelona, Spain",  "Город · Испания"},
        {"Мадрид",           "Madrid, Spain",     "Город · Испания"},
        {"Тенерифе",         "Tenerife, Spain",   "Остров · Испания"},
        {"Малага",           "Malaga, Spain",     "Курорт · Испания"},
        {"Ибица",            "Ibiza, Spain",      "Остров · Испания"},
        {"Майорка",          "Mallorca, Spain",   "Остров · Испания"},
        // Italy
        {"Италия",           "Italy",             "Страна"},
        {"Рим",              "Rome, Italy",       "Город · Италия"},
        {"Милан",            "Milan, Italy",      "Город · Италия"},
        {"Венеция",          "Venice, Italy",     "Город · Италия"},
        {"Сицилия",          "Sicily, Italy",     "Остров · Италия"},
        {"Флоренция",        "Florence, Italy",   "Город · Италия"},
        // France
        {"Франция",          "France",            "Страна"},
        {"Париж",            "Paris, France",     "Город · Франция"},
        {"Ницца",            "Nice, France",      "Курорт · Франция"},
        {"Канны",            "Cannes, France",    "Курорт · Франция"},
        // Thailand
        {"Таиланд",          "Thailand",          "Страна"},
        {"Пхукет",           "Phuket, Thailand",  "Курорт · Таиланд"},
        {"Паттайя",          "Pattaya, Thailand", "Курорт · Таиланд"},
        {"Самуи",            "Koh Samui, Thailand","Остров · Таиланд"},
        {"Бангкок",          "Bangkok, Thailand", "Город · Таиланд"},
        {"Краби",            "Krabi, Thailand",   "Курорт · Таиланд"},
        // Bali / Indonesia
        {"Бали",             "Bali, Indonesia",   "Остров · Индонезия"},
        {"Индонезия",        "Indonesia",         "Страна"},
        // Malaysia
        {"Малайзия",         "Malaysia",          "Страна"},
        {"Куала-Лумпур",     "Kuala Lumpur",      "Город · Малайзия"},
        // Singapore
        {"Сингапур",         "Singapore",         "Страна"},
        // Vietnam
        {"Вьетнам",          "Vietnam",           "Страна"},
        {"Нячанг",           "Nha Trang, Vietnam","Курорт · Вьетнам"},
        {"Дананг",           "Da Nang, Vietnam",  "Курорт · Вьетнам"},
        {"Фукуок",           "Phu Quoc, Vietnam", "Остров · Вьетнам"},
        // Maldives
        {"Мальдивы",         "Maldives",          "Страна"},
        // Sri Lanka
        {"Шри-Ланка",        "Sri Lanka",         "Страна"},
        // India
        {"Индия",            "India",             "Страна"},
        {"Гоа",              "Goa, India",        "Курорт · Индия"},
        // Cyprus
        {"Кипр",             "Cyprus",            "Страна"},
        {"Пафос",            "Paphos, Cyprus",    "Курорт · Кипр"},
        {"Лимасол",          "Limassol, Cyprus",  "Курорт · Кипр"},
        // Montenegro
        {"Черногория",       "Montenegro",        "Страна"},
        {"Будва",            "Budva, Montenegro", "Курорт · Черногория"},
        // Croatia
        {"Хорватия",         "Croatia",           "Страна"},
        {"Дубровник",        "Dubrovnik, Croatia","Город · Хорватия"},
        {"Сплит",            "Split, Croatia",    "Город · Хорватия"},
        // Malta
        {"Мальта",           "Malta",             "Страна"},
        // Portugal
        {"Португалия",       "Portugal",          "Страна"},
        {"Лиссабон",         "Lisbon, Portugal",  "Город · Португалия"},
        {"Алгарве",          "Algarve, Portugal", "Курорт · Португалия"},
        // Netherlands
        {"Нидерланды",       "Netherlands",       "Страна"},
        {"Амстердам",        "Amsterdam",         "Город · Нидерланды"},
        // Austria
        {"Австрия",          "Austria",           "Страна"},
        {"Вена",             "Vienna, Austria",   "Город · Австрия"},
        // Czech Republic
        {"Чехия",            "Czech Republic",    "Страна"},
        {"Прага",            "Prague, Czech Republic","Город · Чехия"},
        // Hungary
        {"Венгрия",          "Hungary",           "Страна"},
        {"Будапешт",         "Budapest, Hungary", "Город · Венгрия"},
        // Germany
        {"Германия",         "Germany",           "Страна"},
        {"Берлин",           "Berlin, Germany",   "Город · Германия"},
        {"Мюнхен",           "Munich, Germany",   "Город · Германия"},
        // Switzerland
        {"Швейцария",        "Switzerland",       "Страна"},
        {"Женева",           "Geneva, Switzerland","Город · Швейцария"},
        // Georgia
        {"Грузия",           "Georgia",           "Страна"},
        {"Тбилиси",          "Tbilisi, Georgia",  "Город · Грузия"},
        {"Батуми",           "Batumi, Georgia",   "Курорт · Грузия"},
        // Mexico
        {"Мексика",          "Mexico",            "Страна"},
        {"Канкун",           "Cancun, Mexico",    "Курорт · Мексика"},
        // Cuba
        {"Куба",             "Cuba",              "Страна"},
        {"Варадеро",         "Varadero, Cuba",    "Курорт · Куба"},
        // Dominican Republic
        {"Доминикана",       "Dominican Republic","Страна"},
        {"Пунта-Кана",       "Punta Cana",        "Курорт · Доминикана"},
        // Morocco
        {"Марокко",          "Morocco",           "Страна"},
        {"Марракеш",         "Marrakech, Morocco","Город · Марокко"},
        // Israel
        {"Израиль",          "Israel",            "Страна"},
        {"Тель-Авив",        "Tel Aviv, Israel",  "Город · Израиль"},
        // Japan
        {"Япония",           "Japan",             "Страна"},
        {"Токио",            "Tokyo, Japan",      "Город · Япония"},
        // South Korea
        {"Южная Корея",      "South Korea",       "Страна"},
        {"Сеул",             "Seoul, South Korea","Город · Южная Корея"},
        // Australia
        {"Австралия",        "Australia",         "Страна"},
        {"Сидней",           "Sydney, Australia", "Город · Австралия"},
        // USA
        {"США",              "United States",     "Страна"},
        {"Нью-Йорк",         "New York, USA",     "Город · США"},
        {"Майами",           "Miami, USA",        "Город · США"},
        {"Лос-Анджелес",     "Los Angeles, USA",  "Город · США"},
        {"Лас-Вегас",        "Las Vegas, USA",    "Город · США"},
    };

    private TextView tvTourFrom, tvTourTo, tvDateFrom, tvDateTo;
    private TextView tvAdultsCount, tvChildrenCount;
    private TextView tvEmpty;

    private String fromCity  = "";
    private String toDisplay = "";
    private String toSearch  = "";    // Russian destination name for Level.travel
    private Calendar checkInCal  = null;
    private Calendar checkOutCal = null;
    private int adults   = 2;
    private int children = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tour_search, container, false);

        tvTourFrom      = view.findViewById(R.id.tv_tour_from);
        tvTourTo        = view.findViewById(R.id.tv_tour_to);
        tvDateFrom      = view.findViewById(R.id.tv_date_from);
        tvDateTo        = view.findViewById(R.id.tv_date_to);
        tvAdultsCount   = view.findViewById(R.id.tv_adults_count);
        tvChildrenCount = view.findViewById(R.id.tv_children_count);
        tvEmpty         = view.findViewById(R.id.tv_search_empty);

        view.findViewById(R.id.ll_tour_from).setOnClickListener(v -> showDeparturePicker());
        view.findViewById(R.id.ll_tour_to).setOnClickListener(v -> showDestinationPicker());
        view.findViewById(R.id.tv_date_from).setOnClickListener(v -> showDatePicker(true));
        view.findViewById(R.id.tv_date_to).setOnClickListener(v -> showDatePicker(false));
        setupPersonsControls(view);
        view.findViewById(R.id.btn_search_tours).setOnClickListener(v -> searchTours());
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        return view;
    }

    // ── City / destination pickers ────────────────────────────────────────

    private void showDeparturePicker() {
        showPicker("Откуда летим?", DEPARTURE_CITIES, (chosen) -> {
            fromCity = chosen[0];
            tvTourFrom.setText(chosen[0] + "  •  " + chosen[1]);
            tvTourFrom.setTextColor(0xFF212121);
        });
    }

    private void showDestinationPicker() {
        showPicker("Куда едем?", DESTINATIONS, (chosen) -> {
            toDisplay = chosen[0];
            toSearch  = chosen[0];   // Russian name → Level.travel
            tvTourTo.setText(chosen[0]);
            tvTourTo.setTextColor(0xFF212121);
        });
    }

    private interface OnCityChosen { void onChosen(String[] city); }

    private void showPicker(String title, String[][] data, OnCityChosen callback) {
        if (getContext() == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sv = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_city_picker, null);

        ((TextView) sv.findViewById(R.id.tv_sheet_title)).setText(title);

        EditText etSearch = sv.findViewById(R.id.et_city_search);
        ListView lv       = sv.findViewById(R.id.lv_cities);

        List<String[]> filtered = new ArrayList<>();
        for (String[] row : data) filtered.add(row);

        PickerAdapter adapter = new PickerAdapter(filtered);
        lv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().toLowerCase().trim();
                filtered.clear();
                for (String[] row : data) {
                    if (row[0].toLowerCase().contains(q) || row[2].toLowerCase().contains(q)) {
                        filtered.add(row);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable e) {}
        });

        lv.setOnItemClickListener((parent, v, pos, id) -> {
            callback.onChosen(filtered.get(pos));
            sheet.dismiss();
        });

        sheet.setContentView(sv);
        sheet.show();
    }

    private static class PickerAdapter extends BaseAdapter {
        private final List<String[]> items;
        PickerAdapter(List<String[]> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int p) { return items.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            String[] row = items.get(pos);
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(row[0]);
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(row[2]);
            return convertView;
        }
    }

    // ── Date pickers ─────────────────────────────────────────────────────

    private void showDatePicker(boolean isCheckIn) {
        if (getContext() == null) return;
        Calendar now = Calendar.getInstance();
        Calendar minCal = isCheckIn ? now : (checkInCal != null ? checkInCal : now);

        DatePickerDialog dp = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            if (isCheckIn) {
                checkInCal = cal;
                tvDateFrom.setText(formatDate(cal));
                tvDateFrom.setTextColor(0xFF212121);
                if (checkOutCal != null && !checkOutCal.after(cal)) {
                    checkOutCal = null;
                    tvDateTo.setText("Обратно");
                    tvDateTo.setTextColor(0xFF9E9E9E);
                }
            } else {
                checkOutCal = cal;
                tvDateTo.setText(formatDate(cal));
                tvDateTo.setTextColor(0xFF212121);
            }
        }, minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), minCal.get(Calendar.DAY_OF_MONTH));

        dp.getDatePicker().setMinDate(minCal.getTimeInMillis());
        dp.show();
    }

    private String formatDate(Calendar cal) {
        return String.format(Locale.getDefault(), "%d %s",
                cal.get(Calendar.DAY_OF_MONTH), MONTHS[cal.get(Calendar.MONTH)]);
    }

    private String toLevelDate(Calendar cal) {
        return String.format(Locale.getDefault(), "%02d.%02d.%04d",
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
    }

    private static final String[] MONTHS =
        {"янв", "фев", "мар", "апр", "мая", "июн",
         "июл", "авг", "сен", "окт", "ноя", "дек"};

    // ── Persons ───────────────────────────────────────────────────────────

    private void setupPersonsControls(View root) {
        tvAdultsCount.setText(String.valueOf(adults));
        tvChildrenCount.setText(String.valueOf(children));
        root.findViewById(R.id.btn_adults_minus).setOnClickListener(v -> {
            if (adults > 1) { adults--; tvAdultsCount.setText(String.valueOf(adults)); }
        });
        root.findViewById(R.id.btn_adults_plus).setOnClickListener(v -> {
            if (adults < 9) { adults++; tvAdultsCount.setText(String.valueOf(adults)); }
        });
        root.findViewById(R.id.btn_children_minus).setOnClickListener(v -> {
            if (children > 0) { children--; tvChildrenCount.setText(String.valueOf(children)); }
        });
        root.findViewById(R.id.btn_children_plus).setOnClickListener(v -> {
            if (children < 6) { children++; tvChildrenCount.setText(String.valueOf(children)); }
        });
    }

    // ── Search → Level.travel (пакетные туры "все включено") ─────────────

    private void searchTours() {
        if (toSearch.isEmpty()) {
            Toast.makeText(requireContext(), "Выберите направление", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkInCal == null) {
            Toast.makeText(requireContext(), "Выберите дату заезда", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkOutCal == null) {
            Toast.makeText(requireContext(), "Выберите дату выезда", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nights count from check-in to check-out
        long diffMs = checkOutCal.getTimeInMillis() - checkInCal.getTimeInMillis();
        int nights = (int) (diffMs / (1000 * 60 * 60 * 24));
        if (nights < 1) nights = 7;

        String url = "https://level.travel/search"
                + "#adults=" + adults
                + "&children_count=" + children
                + "&nights_from=" + nights
                + "&nights_to=" + (nights + 3)
                + "&departure_city_name=" + android.net.Uri.encode(fromCity.isEmpty() ? "Алматы" : fromCity)
                + "&arrival_country_name=" + android.net.Uri.encode(toSearch)
                + "&date_from=" + toLevelDate(checkInCal)
                + "&meal=all_inclusive";

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,
                         FlightWebFragment.newInstance(url, "Level.travel — Туры"))
                .addToBackStack("tour_web")
                .commit();
    }
}
