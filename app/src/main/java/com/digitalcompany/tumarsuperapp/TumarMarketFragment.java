package com.digitalcompany.tumarsuperapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayRequest;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TumarMarketFragment extends Fragment {

    private static final String MARKET_URL = "http://10.0.2.2:8080";
    private static final String APP_SECRET = "tumar_app_secret_2024";
    private static final String USER_PREFS = "UserPrefs";
    private static final String KEY_TOKEN  = "auth_token";

    private WebView webView;
    private ProgressBar progressBar;
    private View errorLayout;
    private TextView tvErrorMsg;

    private String marketToken = null;
    private boolean tokenInjected = false;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Bottom nav tab IDs in order
    private static final int[] TAB_IDS = {
        R.id.mnav_shop, R.id.mnav_catalog, R.id.mnav_favorites,
        R.id.mnav_cart, R.id.mnav_orders
    };
    private static final int[] LABEL_IDS = {
        R.id.mnav_shop_label, R.id.mnav_catalog_label, R.id.mnav_favorites_label,
        R.id.mnav_cart_label, R.id.mnav_orders_label
    };
    private static final String[] TAB_URLS = {
        "/", "/catalog", "/favorites", "/cart", "/orders"
    };
    private int activeTab = 0;
    private View rootView;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_tumar_market, container, false);

        webView     = rootView.findViewById(R.id.webview_market);
        progressBar = rootView.findViewById(R.id.progress_market);
        errorLayout = rootView.findViewById(R.id.layout_market_error);
        tvErrorMsg  = rootView.findViewById(R.id.tv_market_error);

        // Hide system AppBar and BottomNav
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }

        setupWebView();
        setupHeader(rootView);
        setupBottomNav(rootView);

        // Start auto-login flow, then load WebView
        fetchProfileAndAutoLogin();

        return rootView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
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
                // Inject token if available and not yet injected
                if (marketToken != null && !tokenInjected) {
                    injectMarketToken();
                }
                if (url != null && url.contains("/checkout")) {
                    injectTumarPayBridge();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (!isAdded() || request == null || !request.isForMainFrame()) return;
                webView.setVisibility(View.GONE);
                tvErrorMsg.setText("Не удалось подключиться к Tumar Market.\n\nУбедитесь, что сервер запущен.");
                errorLayout.setVisibility(View.VISIBLE);
            }
        });

        rootView.findViewById(R.id.btn_market_retry).setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.reload();
        });
    }

    private void setupHeader(View root) {
        root.findViewById(R.id.btn_market_close).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        EditText searchBar = root.findViewById(R.id.et_market_search);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchBar.getText().toString().trim();
                if (!query.isEmpty()) {
                    webView.loadUrl(MARKET_URL + "/search?q=" + android.net.Uri.encode(query));
                    hideKeyboard(searchBar);
                }
                return true;
            }
            return false;
        });
    }

    private void setupBottomNav(View root) {
        for (int i = 0; i < TAB_IDS.length; i++) {
            final int idx = i;
            root.findViewById(TAB_IDS[i]).setOnClickListener(v -> selectTab(idx));
        }
        updateTabUI();
    }

    private void selectTab(int idx) {
        activeTab = idx;
        updateTabUI();
        webView.loadUrl(MARKET_URL + TAB_URLS[idx]);
    }

    private void updateTabUI() {
        for (int i = 0; i < LABEL_IDS.length; i++) {
            TextView label = rootView.findViewById(LABEL_IDS[i]);
            if (label != null) {
                boolean isActive = i == activeTab;
                label.setTextColor(isActive
                    ? getResources().getColor(R.color.colorPrimary, null)
                    : getResources().getColor(R.color.text_secondary, null));
                label.setTypeface(null, isActive
                    ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // ── Auto-login flow ───────────────────────────────────────────────────────

    private void fetchProfileAndAutoLogin() {
        SharedPreferences prefs = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        String appToken = prefs.getString(KEY_TOKEN, null);
        if (appToken == null) {
            // No token, just load market without auto-login
            webView.loadUrl(MARKET_URL);
            return;
        }

        ApiClient.getApiService(requireContext()).getUserProfile()
                .enqueue(new retrofit2.Callback<UserProfileResponse>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<UserProfileResponse> call,
                                           @NonNull retrofit2.Response<UserProfileResponse> response) {
                        if (!isAdded()) return;
                        String phone = null;
                        if (response.isSuccessful() && response.body() != null) {
                            phone = response.body().getPhone();
                        }
                        if (phone != null && !phone.isEmpty()) {
                            callMarketAutoLogin(phone);
                        } else {
                            mainHandler.post(() -> webView.loadUrl(MARKET_URL));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<UserProfileResponse> call,
                                          @NonNull Throwable t) {
                        if (isAdded()) mainHandler.post(() -> webView.loadUrl(MARKET_URL));
                    }
                });
    }

    private void callMarketAutoLogin(String phone) {
        try {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            body.put("app_secret", APP_SECRET);

            Request request = new Request.Builder()
                    .url(MARKET_URL + "/api/auth/app-auto-login")
                    .post(RequestBody.create(body.toString(),
                            MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String bodyStr = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(bodyStr);
                        String token = json.optString("token");
                        if (!token.isEmpty()) {
                            marketToken = token;
                            mainHandler.post(() -> {
                                if (isAdded()) {
                                    webView.loadUrl(MARKET_URL);
                                }
                            });
                        } else {
                            mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
        }
    }

    private void injectMarketToken() {
        if (marketToken == null) return;
        String safeToken = marketToken.replace("'", "\\'");
        String js = "(function(){" +
            "var existing = localStorage.getItem('token');" +
            "if (!existing || existing !== '" + safeToken + "') {" +
            "  localStorage.setItem('token', '" + safeToken + "');" +
            "  window.location.reload();" +
            "}" +
            "})();";
        webView.evaluateJavascript(js, null);
        tokenInjected = true;
    }

    // ── Checkout bridge ───────────────────────────────────────────────────────

    private void injectTumarPayBridge() {
        String js = "(function() {" +
            "if (window.__tumarPayInjected) return;" +
            "window.__tumarPayInjected = true;" +
            "var inp = document.querySelector('input[value=\"tumar_pay\"]');" +
            "if (inp) {" +
            "  inp.checked = true;" +
            "  var card = inp.closest('.payment-option');" +
            "  if (card) { card.style.outline='2px solid #6200EE'; card.style.borderRadius='8px'; }" +
            "}" +
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

    private void showSuccessInWebView(String orderRef) {
        String js = "(function() {" +
            "var lay = document.getElementById('checkout-layout');" +
            "var suc = document.getElementById('checkout-success');" +
            "if (lay) lay.style.display='none';" +
            "if (suc) { suc.style.display='block'; }" +
            "var num = document.getElementById('order-number');" +
            "if (num) num.textContent='#" + orderRef + "';" +
            "})();";
        mainHandler.post(() -> { if (isAdded()) webView.evaluateJavascript(js, null); });
    }

    private void showErrorInWebView(String message) {
        String safeMsg = message.replace("'", "\\'");
        String js = "(function() {" +
            "var btn = document.querySelector('button[onclick=\"placeOrder()\"]');" +
            "if (btn) { btn.disabled=false; btn.textContent='Подтвердить заказ'; }" +
            "var err = document.getElementById('checkout-error');" +
            "if (err) { err.textContent='" + safeMsg + "'; err.style.display='block'; }" +
            "})();";
        mainHandler.post(() -> { if (isAdded()) webView.evaluateJavascript(js, null); });
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
        // Restore system AppBar and BottomNav
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(true);
        }
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
                    .enqueue(new retrofit2.Callback<MarketPayResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<MarketPayResponse> call,
                                       @NonNull retrofit2.Response<MarketPayResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        String orderRef = response.body().orderRef;
                        showSuccessInWebView(orderRef);
                        mainHandler.post(() -> showNativeSuccess(orderRef, amount));
                    } else {
                        String msg = (response.body() != null && response.body().message != null)
                                ? response.body().message : "Ошибка оплаты";
                        showErrorInWebView(msg);
                    }
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<MarketPayResponse> call,
                                      @NonNull Throwable t) {
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
                .setMessage("Заказ " + orderRef + " на сумму " + amountStr + " успешно оплачен.")
                .setPositiveButton("Мои заказы", (d, w) -> selectTab(4))
                .setNegativeButton("Продолжить покупки", (d, w) -> selectTab(0))
                .setCancelable(false)
                .show();
    }
}
