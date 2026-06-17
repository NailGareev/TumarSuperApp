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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AviationFragment extends Fragment {

    // ── World cities: {display name, IATA code, country} ─────────────────
    private static final String[][] CITIES = {
        // Kazakhstan
        {"Алматы",              "ALA", "Казахстан"},
        {"Астана",              "NQZ", "Казахстан"},
        {"Шымкент",             "CIT", "Казахстан"},
        {"Актобе",              "AKX", "Казахстан"},
        {"Актау",               "SCO", "Казахстан"},
        {"Атырау",              "GUW", "Казахстан"},
        {"Усть-Каменогорск",    "UKK", "Казахстан"},
        {"Уральск",             "URA", "Казахстан"},
        {"Павлодар",            "PWQ", "Казахстан"},
        {"Семей",               "PLX", "Казахстан"},
        {"Тараз",               "DMB", "Казахстан"},
        {"Костанай",            "KSN", "Казахстан"},
        {"Петропавловск",       "PPK", "Казахстан"},
        // Kyrgyzstan
        {"Бишкек",              "FRU", "Кыргызстан"},
        {"Ош",                  "OSS", "Кыргызстан"},
        // Uzbekistan
        {"Ташкент",             "TAS", "Узбекистан"},
        {"Самарканд",           "SKD", "Узбекистан"},
        {"Бухара",              "BHK", "Узбекистан"},
        {"Наманган",            "NMA", "Узбекистан"},
        {"Фергана",             "FEG", "Узбекистан"},
        // Tajikistan
        {"Душанбе",             "DYU", "Таджикистан"},
        // Turkmenistan
        {"Ашхабад",             "ASB", "Туркменистан"},
        // Azerbaijan
        {"Баку",                "GYD", "Азербайджан"},
        // Georgia
        {"Тбилиси",             "TBS", "Грузия"},
        {"Батуми",              "BUS", "Грузия"},
        // Armenia
        {"Ереван",              "EVN", "Армения"},
        // Belarus
        {"Минск",               "MSQ", "Беларусь"},
        // Russia
        {"Москва",              "MOW", "Россия"},
        {"Санкт-Петербург",     "LED", "Россия"},
        {"Новосибирск",         "OVB", "Россия"},
        {"Екатеринбург",        "SVX", "Россия"},
        {"Казань",              "KZN", "Россия"},
        {"Уфа",                 "UFA", "Россия"},
        {"Краснодар",           "KRR", "Россия"},
        {"Сочи",                "AER", "Россия"},
        {"Красноярск",          "KJA", "Россия"},
        {"Омск",                "OMS", "Россия"},
        {"Самара",              "KUF", "Россия"},
        {"Иркутск",             "IKT", "Россия"},
        {"Владивосток",         "VVO", "Россия"},
        {"Хабаровск",           "KHV", "Россия"},
        {"Пермь",               "PEE", "Россия"},
        {"Ростов-на-Дону",      "ROV", "Россия"},
        // Turkey
        {"Стамбул",             "IST", "Турция"},
        {"Анталья",             "AYT", "Турция"},
        {"Анкара",              "ESB", "Турция"},
        {"Бодрум",              "BJV", "Турция"},
        {"Даламан",             "DLM", "Турция"},
        // UAE
        {"Дубай",               "DXB", "ОАЭ"},
        {"Абу-Даби",            "AUH", "ОАЭ"},
        // Qatar
        {"Доха",                "DOH", "Катар"},
        // Egypt
        {"Каир",                "CAI", "Египет"},
        {"Шарм-эш-Шейх",        "SSH", "Египет"},
        {"Хургада",             "HRG", "Египет"},
        // Germany
        {"Берлин",              "BER", "Германия"},
        {"Франкфурт",           "FRA", "Германия"},
        {"Мюнхен",              "MUC", "Германия"},
        // France
        {"Париж",               "PAR", "Франция"},
        {"Ницца",               "NCE", "Франция"},
        // UK
        {"Лондон",              "LON", "Великобритания"},
        {"Манчестер",           "MAN", "Великобритания"},
        // Spain
        {"Мадрид",              "MAD", "Испания"},
        {"Барселона",           "BCN", "Испания"},
        {"Малага",              "AGP", "Испания"},
        // Italy
        {"Рим",                 "ROM", "Италия"},
        {"Милан",               "MIL", "Италия"},
        {"Венеция",             "VCE", "Италия"},
        // Netherlands
        {"Амстердам",           "AMS", "Нидерланды"},
        // Austria
        {"Вена",                "VIE", "Австрия"},
        // Czech Republic
        {"Прага",               "PRG", "Чехия"},
        // Poland
        {"Варшава",             "WAW", "Польша"},
        {"Краков",              "KRK", "Польша"},
        // Hungary
        {"Будапешт",            "BUD", "Венгрия"},
        // Greece
        {"Афины",               "ATH", "Греция"},
        {"Салоники",            "SKG", "Греция"},
        {"Ираклион",            "HER", "Греция"},
        // Portugal
        {"Лиссабон",            "LIS", "Португалия"},
        // Switzerland
        {"Женева",              "GVA", "Швейцария"},
        {"Цюрих",               "ZRH", "Швейцария"},
        // Finland
        {"Хельсинки",           "HEL", "Финляндия"},
        // Sweden
        {"Стокгольм",           "STO", "Швеция"},
        // Romania
        {"Бухарест",            "BUH", "Румыния"},
        // Serbia
        {"Белград",             "BEG", "Сербия"},
        // India
        {"Дели",                "DEL", "Индия"},
        {"Мумбаи",              "BOM", "Индия"},
        {"Гоа",                 "GOI", "Индия"},
        // Thailand
        {"Бангкок",             "BKK", "Таиланд"},
        {"Пхукет",              "HKT", "Таиланд"},
        {"Краби",               "KBV", "Таиланд"},
        {"Самуи",               "USM", "Таиланд"},
        // Malaysia
        {"Куала-Лумпур",        "KUL", "Малайзия"},
        // Singapore
        {"Сингапур",            "SIN", "Сингапур"},
        // Hong Kong
        {"Гонконг",             "HKG", "Гонконг"},
        // Japan
        {"Токио",               "TYO", "Япония"},
        {"Осака",               "OSA", "Япония"},
        // South Korea
        {"Сеул",                "SEL", "Южная Корея"},
        // China
        {"Пекин",               "PEK", "Китай"},
        {"Шанхай",              "SHA", "Китай"},
        // Vietnam
        {"Ханой",               "HAN", "Вьетнам"},
        {"Хошимин",             "SGN", "Вьетнам"},
        {"Дананг",              "DAD", "Вьетнам"},
        // Indonesia
        {"Бали",                "DPS", "Индонезия"},
        {"Джакарта",            "JKT", "Индонезия"},
        // Maldives
        {"Мале",                "MLE", "Мальдивы"},
        // Sri Lanka
        {"Коломбо",             "CMB", "Шри-Ланка"},
        // Nepal
        {"Катманду",            "KTM", "Непал"},
        // USA
        {"Нью-Йорк",            "NYC", "США"},
        {"Лос-Анджелес",        "LAX", "США"},
        {"Майами",              "MIA", "США"},
        {"Чикаго",              "ORD", "США"},
        {"Лас-Вегас",           "LAS", "США"},
        {"Сан-Франциско",       "SFO", "США"},
        // Canada
        {"Торонто",             "YYZ", "Канада"},
        {"Ванкувер",            "YVR", "Канада"},
        // Mexico
        {"Канкун",              "CUN", "Мексика"},
        // Cuba
        {"Гавана",              "HAV", "Куба"},
        {"Варадеро",            "VRA", "Куба"},
        // Dominican Republic
        {"Пунта-Кана",          "PUJ", "Доминикана"},
        // Morocco
        {"Марракеш",            "RAK", "Марокко"},
        // Kenya
        {"Найроби",             "NBO", "Кения"},
        // South Africa
        {"Йоханнесбург",        "JNB", "ЮАР"},
        // Australia
        {"Сидней",              "SYD", "Австралия"},
        {"Мельбурн",            "MEL", "Австралия"},
    };

    private TextView tvFrom, tvTo, tvDateDepart, tvDateReturn;
    private String fromCity = "", fromIata = "";
    private String toCity   = "", toIata   = "";
    private Calendar departureCal = null;
    private Calendar returnCal    = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aviation, container, false);

        view.findViewById(R.id.btn_avia_back).setOnClickListener(v ->
                requireActivity().onBackPressed());

        tvFrom       = view.findViewById(R.id.tv_from);
        tvTo         = view.findViewById(R.id.tv_to);
        tvDateDepart = view.findViewById(R.id.tv_date_depart);
        tvDateReturn = view.findViewById(R.id.tv_date_return);

        view.findViewById(R.id.ll_from).setOnClickListener(v -> showCityPicker(true));
        view.findViewById(R.id.ll_to).setOnClickListener(v -> showCityPicker(false));
        view.findViewById(R.id.btn_swap).setOnClickListener(v -> swapCities());
        view.findViewById(R.id.card_date_depart).setOnClickListener(v -> showDatePicker(true));
        view.findViewById(R.id.card_date_return).setOnClickListener(v -> showDatePicker(false));
        view.findViewById(R.id.card_passengers).setOnClickListener(v ->
                showPassengersDialog(view.findViewById(R.id.tv_passengers)));
        view.findViewById(R.id.btn_search_flights).setOnClickListener(v -> handleSearch());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreNavBars();
        }
    }

    // ── City picker ───────────────────────────────────────────────────────

    private void showCityPicker(boolean isFrom) {
        if (getContext() == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_city_picker, null);

        ((TextView) sheetView.findViewById(R.id.tv_sheet_title))
                .setText(isFrom ? "Откуда летим?" : "Куда летим?");

        EditText etSearch = sheetView.findViewById(R.id.et_city_search);
        ListView lv       = sheetView.findViewById(R.id.lv_cities);

        List<String[]> filtered = new ArrayList<>();
        for (String[] c : CITIES) filtered.add(c);

        CityListAdapter adapter = new CityListAdapter(filtered);
        lv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().toLowerCase().trim();
                filtered.clear();
                for (String[] city : CITIES) {
                    if (city[0].toLowerCase().contains(q)
                            || city[1].toLowerCase().contains(q)
                            || city[2].toLowerCase().contains(q)) {
                        filtered.add(city);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable e) {}
        });

        lv.setOnItemClickListener((parent, v, pos, id) -> {
            String[] chosen = filtered.get(pos);
            if (isFrom) {
                fromCity = chosen[0]; fromIata = chosen[1];
                tvFrom.setText(chosen[0] + "  •  " + chosen[1]);
                tvFrom.setTextColor(0xFF212121);
            } else {
                toCity = chosen[0]; toIata = chosen[1];
                tvTo.setText(chosen[0] + "  •  " + chosen[1]);
                tvTo.setTextColor(0xFF212121);
            }
            sheet.dismiss();
        });

        sheet.setContentView(sheetView);
        sheet.show();
    }

    /** Simple adapter that renders "{City}  ({IATA})" with country subtitle */
    private static class CityListAdapter extends BaseAdapter {
        private final List<String[]> cities;

        CityListAdapter(List<String[]> cities) {
            this.cities = cities;
        }

        @Override public int getCount() { return cities.size(); }
        @Override public Object getItem(int pos) { return cities.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            String[] city = cities.get(pos);
            ((TextView) convertView.findViewById(android.R.id.text1))
                    .setText(city[0] + "  (" + city[1] + ")");
            ((TextView) convertView.findViewById(android.R.id.text2))
                    .setText(city[2]);
            return convertView;
        }
    }

    // ── Swap ──────────────────────────────────────────────────────────────

    private void swapCities() {
        String tmpCity = fromCity, tmpIata = fromIata;
        fromCity = toCity;   fromIata = toIata;
        toCity   = tmpCity;  toIata   = tmpIata;

        tvFrom.setText(fromCity.isEmpty() ? "Выберите город" : fromCity + "  •  " + fromIata);
        tvFrom.setTextColor(fromCity.isEmpty() ? 0xFF9E9E9E : 0xFF212121);
        tvTo.setText(toCity.isEmpty() ? "Выберите город" : toCity + "  •  " + toIata);
        tvTo.setTextColor(toCity.isEmpty() ? 0xFF9E9E9E : 0xFF212121);
    }

    // ── Date picker ───────────────────────────────────────────────────────

    private void showDatePicker(boolean isDepart) {
        if (getContext() == null) return;
        Calendar now = Calendar.getInstance();
        // Minimum date: today for departure, departure date for return
        Calendar minCal = isDepart ? now : (departureCal != null ? departureCal : now);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            if (isDepart) {
                departureCal = cal;
                tvDateDepart.setText(formatDisplay(cal));
                tvDateDepart.setTextColor(0xFF212121);
                // If return date is now before departure, clear it
                if (returnCal != null && !returnCal.after(cal)) {
                    returnCal = null;
                    tvDateReturn.setText("Выберите дату");
                    tvDateReturn.setTextColor(0xFF9E9E9E);
                }
            } else {
                returnCal = cal;
                tvDateReturn.setText(formatDisplay(cal));
                tvDateReturn.setTextColor(0xFF212121);
            }
        }, minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), minCal.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(minCal.getTimeInMillis());
        dialog.show();
    }

    private String formatDisplay(Calendar cal) {
        return String.format(Locale.getDefault(), "%d %s",
                cal.get(Calendar.DAY_OF_MONTH), MONTH_NAMES[cal.get(Calendar.MONTH)]);
    }

    /** Returns DDMM string for Aviasales URL */
    private String toAviasalesDate(Calendar cal) {
        return String.format(Locale.getDefault(), "%02d%02d",
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1);
    }

    private static final String[] MONTH_NAMES =
        {"янв", "фев", "мар", "апр", "мая", "июн",
         "июл", "авг", "сен", "окт", "ноя", "дек"};

    // ── Passengers ────────────────────────────────────────────────────────

    private void showPassengersDialog(TextView tvPassengers) {
        if (getContext() == null) return;
        String[] classes = {"Эконом", "Комфорт", "Бизнес", "Первый"};
        final int[] selected = {0};
        new AlertDialog.Builder(requireContext())
                .setTitle("Класс обслуживания")
                .setSingleChoiceItems(classes, 0, (d, which) -> selected[0] = which)
                .setPositiveButton("Выбрать", (d, w) -> {
                    tvPassengers.setText("1 пассажир, " + classes[selected[0]].toLowerCase());
                    tvPassengers.setTextColor(0xFF212121);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ── Search ────────────────────────────────────────────────────────────

    private void handleSearch() {
        if (fromIata.isEmpty() || toIata.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Выберите город отправления и назначения", Toast.LENGTH_SHORT).show();
            return;
        }
        if (departureCal == null) {
            Toast.makeText(requireContext(),
                    "Выберите дату вылета", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build Aviasales URL: {from}{DDMM}{to}{DDMM_return?}{pax}1
        StringBuilder url = new StringBuilder("https://www.aviasales.ru/search/");
        url.append(fromIata);
        url.append(toAviasalesDate(departureCal));
        url.append(toIata);
        if (returnCal != null) url.append(toAviasalesDate(returnCal));
        url.append("1");  // 1 passenger

        openAviasales(url.toString());
    }

    private void openAviasales(String url) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, FlightWebFragment.newInstance(url))
                .addToBackStack("flight_web")
                .commit();
    }
}
