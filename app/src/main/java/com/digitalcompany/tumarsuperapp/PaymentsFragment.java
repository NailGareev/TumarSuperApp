package com.digitalcompany.tumarsuperapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.adapter.PaymentsAdapter;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.PayRequest;
import com.digitalcompany.tumarsuperapp.network.models.PayResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentsFragment extends Fragment {

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payments, container, false);

        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }

        RecyclerView recyclerView = view.findViewById(R.id.rv_payments);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        PaymentsAdapter adapter = new PaymentsAdapter(buildItems(), this::showPaymentDialog);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private List<Object> buildItems() {
        List<Object> items = new ArrayList<>();

        items.add(new PaymentsAdapter.Category("Мобильная связь"));
        items.add(new PaymentsAdapter.Service("Activ / Kcell",   "Номер телефона", "+7XXXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("Beeline / IZI",   "Номер телефона", "+7XXXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("Tele2 / Altel",   "Номер телефона", "+7XXXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("Beeline KG",      "Номер телефона", "+996XXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("О! KG",           "Номер телефона", "+996XXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("MegaCom KG",      "Номер телефона", "+996XXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("IZI KG",          "Номер телефона", "+996XXXXXXXXX"));

        items.add(new PaymentsAdapter.Category("Коммунальные услуги"));
        items.add(new PaymentsAdapter.Service("АЛСЕКО",    "Лицевой счет", "123456789"));
        items.add(new PaymentsAdapter.Service("YURTA DOM", "Лицевой счет", "123456789"));

        items.add(new PaymentsAdapter.Category("Домашний интернет"));
        items.add(new PaymentsAdapter.Service("Beeline",        "Номер договора", "123456789"));
        items.add(new PaymentsAdapter.Service("ALMA PLUS",      "Номер договора", "123456789"));
        items.add(new PaymentsAdapter.Service("MEGANET",        "Номер договора", "123456789"));
        items.add(new PaymentsAdapter.Service("Kazakhtelecom",  "Номер договора", "123456789"));

        items.add(new PaymentsAdapter.Category("Электронные кошельки"));
        items.add(new PaymentsAdapter.Service("YURTA WALLET", "Номер телефона / счета", "+7XXXXXXXXXX"));
        items.add(new PaymentsAdapter.Service("QPLUS",        "Номер счета",            "123456789"));

        items.add(new PaymentsAdapter.Category("Игровые сервисы"));
        items.add(new PaymentsAdapter.Service("STEAM", "Логин Steam", "steam_username"));

        return items;
    }

    private void showPaymentDialog(PaymentsAdapter.Service service) {
        if (getContext() == null) return;

        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        int dp8  = dp16 / 2;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp8, dp16, 0);

        EditText etAccount = new EditText(requireContext());
        etAccount.setHint(service.accountLabel + "  (" + service.accountHint + ")");
        etAccount.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etAccount);

        EditText etAmount = new EditText(requireContext());
        etAmount.setHint("Сумма (KZT)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp8;
        etAmount.setLayoutParams(lp);
        layout.addView(etAmount);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(service.name)
                .setView(layout)
                .setPositiveButton("Оплатить", null)
                .setNegativeButton("Отмена", null)
                .create();

        // Override positive button to prevent auto-dismiss on validation error
        dialog.setOnShowListener(d -> {
            Button btnPay = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnPay.setOnClickListener(v -> {
                String account   = etAccount.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();

                if (account.isEmpty()) {
                    etAccount.setError("Введите " + service.accountLabel.toLowerCase());
                    return;
                }
                if (amountStr.isEmpty()) {
                    etAmount.setError("Введите сумму");
                    return;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Некорректная сумма");
                    return;
                }

                if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("500000")) > 0) {
                    etAmount.setError("От 1 до 500 000 KZT");
                    return;
                }

                dialog.dismiss();
                performPayment(service.name, account, amount);
            });
        });

        dialog.show();
    }

    private void performPayment(String service, String accountNumber, BigDecimal amount) {
        if (apiService == null || getContext() == null) return;

        apiService.pay(new PayRequest(service, accountNumber, amount)).enqueue(new Callback<PayResponse>() {
            @Override
            public void onResponse(@NonNull Call<PayResponse> call, @NonNull Response<PayResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BigDecimal newBalance = response.body().getNewBalance();
                    String balanceStr = newBalance != null ? newBalance.toPlainString() : "—";
                    Toast.makeText(requireContext(),
                            "Оплата прошла успешно!\nОстаток на счете: " + balanceStr + " KZT",
                            Toast.LENGTH_LONG).show();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Ошибка сервера";
                    Toast.makeText(requireContext(), "Ошибка оплаты: " + msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PayResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
