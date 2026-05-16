package com.digitalcompany.tumarsuperapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.util.Calendar;
import java.util.Locale;

public class AviationFragment extends Fragment {

    private TextView tvFrom, tvTo, tvDateDepart, tvDateReturn;
    private String fromCity = "", toCity = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aviation, container, false);

        tvFrom       = view.findViewById(R.id.tv_from);
        tvTo         = view.findViewById(R.id.tv_to);
        tvDateDepart = view.findViewById(R.id.tv_date_depart);
        tvDateReturn = view.findViewById(R.id.tv_date_return);

        view.findViewById(R.id.ll_from).setOnClickListener(v -> showCityDialog(true));
        view.findViewById(R.id.ll_to).setOnClickListener(v -> showCityDialog(false));

        view.findViewById(R.id.btn_swap).setOnClickListener(v -> swapCities());

        view.findViewById(R.id.card_date_depart).setOnClickListener(v ->
                showDatePicker(true));
        view.findViewById(R.id.card_date_return).setOnClickListener(v ->
                showDatePicker(false));

        view.findViewById(R.id.card_passengers).setOnClickListener(v ->
                showPassengersDialog(view.findViewById(R.id.tv_passengers)));

        view.findViewById(R.id.btn_search_flights).setOnClickListener(v ->
                handleSearch());

        return view;
    }

    private void showCityDialog(boolean isFrom) {
        if (getContext() == null) return;

        EditText etCity = new EditText(requireContext());
        etCity.setHint("Город или аэропорт");
        etCity.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etCity.setText(isFrom ? fromCity : toCity);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        etCity.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(requireContext())
                .setTitle(isFrom ? "Откуда" : "Куда")
                .setView(etCity)
                .setPositiveButton("Выбрать", (d, w) -> {
                    String city = etCity.getText().toString().trim();
                    if (!city.isEmpty()) {
                        if (isFrom) {
                            fromCity = city;
                            tvFrom.setText(city);
                            tvFrom.setTextColor(0xFF212121);
                        } else {
                            toCity = city;
                            tvTo.setText(city);
                            tvTo.setTextColor(0xFF212121);
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void swapCities() {
        String tmp = fromCity;
        fromCity = toCity;
        toCity = tmp;

        tvFrom.setText(fromCity.isEmpty() ? "Откуда" : fromCity);
        tvFrom.setTextColor(fromCity.isEmpty() ? 0xFF9E9E9E : 0xFF212121);
        tvTo.setText(toCity.isEmpty() ? "Куда" : toCity);
        tvTo.setTextColor(toCity.isEmpty() ? 0xFF9E9E9E : 0xFF212121);
    }

    private void showDatePicker(boolean isDepart) {
        if (getContext() == null) return;
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%d %s",
                    day, monthName(month));
            if (isDepart) {
                tvDateDepart.setText(date);
                tvDateDepart.setTextColor(0xFF212121);
            } else {
                tvDateReturn.setText(date);
                tvDateReturn.setTextColor(0xFF212121);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

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

    private void handleSearch() {
        if (fromCity.isEmpty() || toCity.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Укажите город отправления и назначения", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(),
                "Поиск рейсов: " + fromCity + " → " + toCity + "\nДемо-режим: онлайн-поиск в разработке",
                Toast.LENGTH_LONG).show();
    }

    private static String monthName(int month) {
        String[] months = {"янв", "фев", "мар", "апр", "мая", "июн",
                           "июл", "авг", "сен", "окт", "ноя", "дек"};
        return months[month];
    }
}
