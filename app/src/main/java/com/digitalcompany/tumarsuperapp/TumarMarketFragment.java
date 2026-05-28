package com.digitalcompany.tumarsuperapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayRequest;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayResponse;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TumarMarketFragment extends Fragment {

    // URL рынка на эмуляторе (10.0.2.2 = localhost хост-машины)
    private static final String MARKET_URL = "http://10.0.2.2:8080";

    private WebView webView;
    private ProgressBar progressBar;
    private View errorLayout;
    private TextView tvErrorMsg;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tumar_market, container, false);

        webView    = view.findViewById(R.id.webview_market);
        progressBar = view.findViewById(R.id.progress_market);
        errorLayout = view.findViewById(R.id.layout_market_error);
        tvErrorMsg  = view.findViewById(R.id.tv_market_error);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setUserAgentString(ws.getUserAgentString() + " TumarApp/1.0");

        webView.addJavascriptInterface(new TumarBridge(), "TumarBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!isAdded()) return;
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isAdded()) return;
                errorLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                if (url != null && url.contains("/checkout")) {
                    injectTumarPayBridge();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (!isAdded() || request == null || !request.isForMainFrame()) return;
                webView.setVisibility(View.GONE);
                tvErrorMsg.setText("Не удалось подключиться к Tumar Market.\n\nУбедитесь, что сервер запущен:\npython run.py  (в папке tumar-market)");
                errorLayout.setVisibility(View.VISIBLE);
            }
        });

        view.findViewById(R.id.btn_market_retry).setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.reload();
        });

        webView.loadUrl(MARKET_URL);
        return view;
    }

    /** Перехватывает кнопку «Подтвердить заказ» для способа оплаты Tumar Pay */
    private void injectTumarPayBridge() {
        String js = "(function() {" +
            "if (window.__tumarPayInjected) return;" +
            "window.__tumarPayInjected = true;" +
            // Выделяем Tumar Pay как выбранный способ по умолчанию
            "var inp = document.querySelector('input[value=\"tumar_pay\"]');" +
            "if (inp) {" +
            "  inp.checked = true;" +
            "  var card = inp.closest('.payment-option');" +
            "  if (card) { card.style.outline='2px solid #6200EE'; card.style.borderRadius='8px'; }" +
            "}" +
            // Перехватываем placeOrder
            "var _orig = window.placeOrder;" +
            "window.placeOrder = function() {" +
            "  var pay = document.querySelector('input[name=\"payment\"]:checked')?.value;" +
            "  if (pay !== 'tumar_pay') { if (typeof _orig==='function') _orig(); return; }" +
            "  var city   = document.getElementById('delivery-city')?.value || '';" +
            "  var street = (document.getElementById('delivery-street')?.value || '').trim();" +
            "  var comment= (document.getElementById('delivery-comment')?.value || '').trim();" +
            "  var errEl  = document.getElementById('checkout-error');" +
            "  if (!street) {" +
            "    errEl.textContent = 'Введите адрес доставки';" +
            "    errEl.style.display = 'block'; return;" +
            "  }" +
            "  var addr  = city + ', ' + street + (comment ? ' (' + comment + ')' : '');" +
            "  var total = document.getElementById('co-total')?.textContent || '0';" +
            "  var items = JSON.stringify(window.cartData?.items || []);" +
            "  var btn = document.querySelector('button[onclick=\"placeOrder()\"]');" +
            "  if (btn) { btn.disabled=true; btn.textContent='Обработка...'; }" +
            "  TumarBridge.payWithTumar(total, addr, items);" +
            "};" +
            "})();";
        webView.evaluateJavascript(js, null);
    }

    /** Показывает успех прямо в WebView */
    private void showSuccessInWebView(String orderRef) {
        String js = "(function() {" +
            "var lay = document.getElementById('checkout-layout');" +
            "var suc = document.getElementById('checkout-success');" +
            "if (lay) lay.style.display='none';" +
            "if (suc) { suc.style.display='block'; }" +
            "var num = document.getElementById('order-number');" +
            "if (num) num.textContent='#" + orderRef + "';" +
            "})();";
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) webView.evaluateJavascript(js, null);
        });
    }

    /** Сбрасывает кнопку в WebView и показывает ошибку */
    private void showErrorInWebView(String message) {
        String safeMsg = message.replace("'", "\\'");
        String js = "(function() {" +
            "var btn = document.querySelector('button[onclick=\"placeOrder()\"]');" +
            "if (btn) { btn.disabled=false; btn.textContent='Подтвердить заказ'; }" +
            "var err = document.getElementById('checkout-error');" +
            "if (err) { err.textContent='" + safeMsg + "'; err.style.display='block'; }" +
            "})();";
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) webView.evaluateJavascript(js, null);
        });
    }

    public boolean onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }

    // ─── JavaScript Bridge ───────────────────────────────────────────────────

    private class TumarBridge {

        @JavascriptInterface
        public void payWithTumar(String totalStr, String address, String itemsJson) {
            // Парсим сумму: "657 780 ₸" → 657780.0
            String cleaned = totalStr.replaceAll("[^0-9.]", "").trim();
            double amount;
            try {
                amount = Double.parseDouble(cleaned);
            } catch (NumberFormatException e) {
                showErrorInWebView("Не удалось определить сумму заказа");
                return;
            }
            if (amount <= 0) {
                showErrorInWebView("Сумма заказа не может быть нулевой");
                return;
            }

            if (getContext() == null) return;
            MarketPayRequest req = new MarketPayRequest(amount, address, itemsJson);
            ApiClient.getApiService(requireContext()).marketPay(req)
                    .enqueue(new Callback<MarketPayResponse>() {
                @Override
                public void onResponse(@NonNull Call<MarketPayResponse> call,
                                       @NonNull Response<MarketPayResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null
                            && response.body().success) {
                        String orderRef = response.body().orderRef;
                        showSuccessInWebView(orderRef);
                        // Показываем нативный диалог с предложением открыть Покупки
                        new Handler(Looper.getMainLooper()).post(() -> showNativeSuccess(orderRef, amount));
                    } else {
                        String msg = (response.body() != null && response.body().message != null)
                                ? response.body().message : "Ошибка оплаты";
                        showErrorInWebView(msg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<MarketPayResponse> call, @NonNull Throwable t) {
                    showErrorInWebView("Ошибка сети. Проверьте подключение.");
                }
            });
        }
    }

    private void showNativeSuccess(String orderRef, double amount) {
        if (!isAdded() || getContext() == null) return;
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));
        String amountStr = fmt.format((long) amount) + " ₸";

        new AlertDialog.Builder(requireContext())
                .setTitle("Заказ оплачен!")
                .setMessage("Заказ " + orderRef + " на сумму " + amountStr +
                        " успешно оплачен с баланса Tumar.")
                .setPositiveButton("Мои покупки", (d, w) -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container,
                                        new MarketPurchasesFragment(), "market_purchases")
                                .addToBackStack("market_purchases")
                                .commit();
                    }
                })
                .setNegativeButton("Продолжить покупки", (d, w) -> {
                    if (webView != null) webView.loadUrl(MARKET_URL);
                })
                .setCancelable(false)
                .show();
    }
}
