package com.digitalcompany.tumarsuperapp;

import android.app.DatePickerDialog;
import android.net.Uri;
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

public class TrainSearchFragment extends Fragment {

    private static final String[][] CITIES = {
        // Kazakhstan
        {"Алматы",           "ALA", "Казахстан"},
        {"Астана",           "NQZ", "Казахстан"},
        {"Шымкент",          "CIT", "Казахстан"},
        {"Актобе",           "AKX", "Казахстан"},
        {"Атырау",           "GUW", "Казахстан"},
        {"Уральск",          "URA", "Казахстан"},
        {"Павлодар",         "PWQ", "Казахстан"},
        {"Петропавловск",    "PPK", "Казахстан"},
        {"Костанай",         "KSN", "Казахстан"},
        {"Кызылорда",        "KZO", "Казахстан"},
        {"Туркестан",        "HSA", "Казахстан"},
        {"Семей",            "PLX", "Казахстан"},
        {"Усть-Каменогорск", "UKK", "Казахстан"},
        {"Тараз",            "DMB", "Казахстан"},
        // Russia
        {"Москва",           "MOW", "Россия"},
        {"Санкт-Петербург",  "LED", "Россия"},
        {"Новосибирск",      "OVB", "Россия"},
        {"Екатеринбург",     "SVX", "Россия"},
        {"Омск",             "OMS", "Россия"},
        {"Самара",           "KUF", "Россия"},
        {"Казань",           "KZN", "Россия"},
        {"Уфа",              "UFA", "Россия"},
        {"Оренбург",         "REN", "Россия"},
        {"Саратов",          "RTW", "Россия"},
        // Uzbekistan
        {"Ташкент",          "TAS", "Узбекистан"},
        {"Самарканд",        "SKD", "Узбекистан"},
        // Kyrgyzstan
        {"Бишкек",           "FRU", "Кыргызстан"},
    };

    private TextView tvFrom, tvTo, tvDate, tvPassengers;
    private String fromCity = "", toCity = "";
    private Calendar departureCal = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_train_search, container, false);

        tvFrom       = view.findViewById(R.id.tv_train_from);
        tvTo         = view.findViewById(R.id.tv_train_to);
        tvDate       = view.findViewById(R.id.tv_train_date);
        tvPassengers = view.findViewById(R.id.tv_train_passengers);

        view.findViewById(R.id.btn_train_back).setOnClickListener(v ->
                requireActivity().onBackPressed());

        view.findViewById(R.id.ll_train_from).setOnClickListener(v -> showCityPicker(true));
        view.findViewById(R.id.ll_train_to).setOnClickListener(v -> showCityPicker(false));
        view.findViewById(R.id.btn_train_swap).setOnClickListener(v -> swapCities());
        view.findViewById(R.id.card_train_date).setOnClickListener(v -> showDatePicker());
        view.findViewById(R.id.card_train_passengers).setOnClickListener(v -> showPassengersDialog());
        view.findViewById(R.id.btn_search_trains).setOnClickListener(v -> handleSearch());

        return view;
    }

    // ── City picker ───────────────────────────────────────────────────────

    private void showCityPicker(boolean isFrom) {
        if (getContext() == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_city_picker, null);

        ((TextView) sheetView.findViewById(R.id.tv_sheet_title))
                .setText(isFrom ? "Откуда едем?" : "Куда едем?");

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
                fromCity = chosen[0];
                tvFrom.setText(chosen[0] + "  •  " + chosen[1]);
                tvFrom.setTextColor(0xFF212121);
            } else {
                toCity = chosen[0];
                tvTo.setText(chosen[0] + "  •  " + chosen[1]);
                tvTo.setTextColor(0xFF212121);
            }
            sheet.dismiss();
        });

        sheet.setContentView(sheetView);
        sheet.show();
    }

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
        String tmp = fromCity;
        fromCity = toCity;
        toCity   = tmp;

        tvFrom.setText(fromCity.isEmpty() ? "Выберите город" : fromCity);
        tvFrom.setTextColor(fromCity.isEmpty() ? 0xFF9E9E9E : 0xFF212121);
        tvTo.setText(toCity.isEmpty() ? "Выберите город" : toCity);
        tvTo.setTextColor(toCity.isEmpty() ? 0xFF9E9E9E : 0xFF212121);
    }

    // ── Date picker ───────────────────────────────────────────────────────

    private void showDatePicker() {
        if (getContext() == null) return;
        Calendar now = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            departureCal = cal;
            tvDate.setText(formatDisplay(cal));
            tvDate.setTextColor(0xFF212121);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
        dialog.show();
    }

    private String formatDisplay(Calendar cal) {
        return String.format(Locale.getDefault(), "%d %s %d",
                cal.get(Calendar.DAY_OF_MONTH),
                MONTH_NAMES[cal.get(Calendar.MONTH)],
                cal.get(Calendar.YEAR));
    }

    private String formatYandexDate(Calendar cal) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private static final String[] MONTH_NAMES =
        {"янв", "фев", "мар", "апр", "мая", "июн",
         "июл", "авг", "сен", "окт", "ноя", "дек"};

    // ── Passengers ────────────────────────────────────────────────────────

    private void showPassengersDialog() {
        if (getContext() == null) return;
        String[] options = {"1 взрослый", "2 взрослых", "3 взрослых", "4 взрослых"};
        final int[] selected = {0};
        new AlertDialog.Builder(requireContext())
                .setTitle("Пассажиры")
                .setSingleChoiceItems(options, 0, (d, which) -> selected[0] = which)
                .setPositiveButton("Выбрать", (d, w) -> {
                    tvPassengers.setText(options[selected[0]]);
                    tvPassengers.setTextColor(0xFF212121);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ── Search ────────────────────────────────────────────────────────────

    private void handleSearch() {
        if (fromCity.isEmpty() || toCity.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Выберите город отправления и назначения", Toast.LENGTH_SHORT).show();
            return;
        }
        if (departureCal == null) {
            Toast.makeText(requireContext(),
                    "Выберите дату отправления", Toast.LENGTH_SHORT).show();
            return;
        }

        // KTZ Express — official Kazakhstan Railways e-ticketing
        String url = "https://online.ktzh.kz/ru/purchase/route"
                + "?from=" + Uri.encode(fromCity)
                + "&to=" + Uri.encode(toCity)
                + "&date=" + formatYandexDate(departureCal)
                + "&passengers=1";

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, FlightWebFragment.newInstance(url, "КТЖ — ЖД Билеты"))
                .addToBackStack("train_web")
                .commit();
    }
}
