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
    private String toSearch  = "";
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

    private String toHtDate(Calendar cal) {
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

    // ── Search → ht.kz ───────────────────────────────────────────────────

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

        long diffMs = checkOutCal.getTimeInMillis() - checkInCal.getTimeInMillis();
        int nights = (int) (diffMs / (1000 * 60 * 60 * 24));
        if (nights < 1) nights = 7;

        int countryId = getCountryId(toSearch);
        int regionId  = getRegionId(toSearch);
        int departId  = getDepartCityId(fromCity);
        String dateFrom = toHtDate(checkInCal);

        if (countryId == 0) {
            // Unknown destination — open ht.kz homepage
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container,
                             FlightWebFragment.newInstance("https://ht.kz/", "ht.kz — Туры"))
                    .addToBackStack("tour_web")
                    .commit();
            return;
        }

        StringBuilder url = new StringBuilder("https://ht.kz/findtours?");
        url.append("country=").append(countryId);
        if (regionId > 0) url.append("&region=").append(regionId);
        url.append("&departCity=").append(departId);
        url.append("&adult=").append(adults);
        if (children > 0) url.append("&child=").append(children);
        url.append("&daysFrom=").append(nights);
        url.append("&daysTo=").append(nights + 2);
        url.append("&dateFrom=").append(dateFrom);
        url.append("&stars=any");

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,
                         FlightWebFragment.newInstance(url.toString(), "ht.kz — Туры"))
                .addToBackStack("tour_web")
                .commit();
    }

    // Country IDs from ht.kz
    private static int getCountryId(String dest) {
        switch (dest) {
            case "Турция": case "Анталья": case "Кемер": case "Белек":
            case "Сиде": case "Алания": case "Мармарис": case "Бодрум":
            case "Стамбул":
                return 1;
            case "ОАЭ": case "Дубай": case "Абу-Даби": case "Шарджа":
                return 2;
            case "Таиланд": case "Пхукет": case "Паттайя": case "Самуи":
            case "Бангкок": case "Краби":
                return 3;
            case "Мальдивы":
                return 9;
            case "Кипр": case "Пафос": case "Лимасол":
                return 10;
            case "Италия": case "Рим": case "Милан": case "Венеция":
            case "Сицилия": case "Флоренция":
                return 13;
            case "Испания": case "Барселона": case "Мадрид": case "Тенерифе":
            case "Малага": case "Ибица": case "Майорка":
                return 14;
            case "Греция": case "Крит": case "Родос": case "Корфу":
            case "Афины": case "Санторини": case "Миконос":
                return 6;
            case "Египет": case "Шарм-эш-Шейх": case "Хургада":
            case "Каир": case "Марса-Алам":
                return 18;
            case "Индонезия": case "Бали":
                return 19;
            case "Черногория": case "Будва":
                return 22;
            case "Хорватия": case "Дубровник": case "Сплит":
                return 23;
            case "Португалия": case "Лиссабон": case "Алгарве":
                return 24;
            case "Мальта":
                return 25;
            case "Вьетнам": case "Нячанг": case "Дананг": case "Фукуок":
                return 26;
            case "Шри-Ланка":
                return 27;
            case "Индия": case "Гоа":
                return 28;
            case "Малайзия": case "Куала-Лумпур":
                return 29;
            case "Куба": case "Варадеро":
                return 30;
            case "Доминикана": case "Пунта-Кана":
                return 31;
            case "Мексика": case "Канкун":
                return 32;
            case "Марокко": case "Марракеш":
                return 33;
            default:
                return 0;
        }
    }

    // Region IDs from ht.kz
    private static int getRegionId(String dest) {
        switch (dest) {
            case "Анталья":       return 8;
            case "Кемер":         return 9;
            case "Белек":         return 10;
            case "Сиде":          return 11;
            case "Алания":        return 12;
            case "Мармарис":      return 13;
            case "Бодрум":        return 14;
            case "Пхукет":        return 16;
            case "Паттайя":       return 15;
            case "Хургада":       return 197;
            case "Шарм-эш-Шейх": return 169;
            case "Дубай":         return 21;
            case "Крит":          return 50;
            case "Родос":         return 51;
            case "Корфу":         return 52;
            case "Санторини":     return 53;
            case "Нячанг":        return 80;
            case "Дананг":        return 81;
            case "Фукуок":        return 82;
            case "Гоа":           return 90;
            case "Будва":         return 100;
            case "Варадеро":      return 110;
            case "Пунта-Кана":    return 120;
            case "Канкун":        return 130;
            default:              return 0;
        }
    }

    // Departure city IDs from ht.kz
    private static int getDepartCityId(String city) {
        switch (city) {
            case "Алматы":           return 1;
            case "Астана":           return 2;
            case "Шымкент":          return 3;
            case "Актобе":           return 4;
            case "Актау":            return 5;
            case "Атырау":           return 6;
            case "Усть-Каменогорск": return 7;
            case "Уральск":          return 8;
            case "Павлодар":         return 9;
            default:                 return 1; // fallback Алматы
        }
    }
}
