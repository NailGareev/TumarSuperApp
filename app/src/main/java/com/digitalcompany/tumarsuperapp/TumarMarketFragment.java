package com.digitalcompany.tumarsuperapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayRequest;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
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

    private static final String MARKET_URL   = "http://193.108.113.91:8080";
    private static final String NODE_URL     = "http://193.108.113.91:3000";
    private static final String APP_SECRET   = "tumar_app_secret_2024";
    private static final String USER_PREFS   = "UserPrefs";
    private static final String KEY_TOKEN    = "auth_token";

    private WebView webView;
    private ProgressBar progressBar;
    private View errorLayout;
    private TextView tvErrorMsg;

    private String marketToken = null;
    private boolean tokenInjected = false;

    // Pending Tumar Pay params (set by JS bridge, used by payment flow)
    private double  pendingAmount  = 0;
    private String  pendingAddress = null;
    private String  pendingItems   = null;

    private final OkHttpClient httpClient  = new OkHttpClient();
    private final Handler      mainHandler = new Handler(Looper.getMainLooper());

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

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(false);
        }

        setupWebView();
        setupHeader(rootView);
        setupBottomNav(rootView);
        fetchProfileAndAutoLogin();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (webView != null && webView.canGoBack()) {
                        webView.goBack();
                    } else if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToHome();
                    }
                }
            });

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
                if (marketToken != null && !tokenInjected) {
                    injectMarketToken();
                }
                if (url != null && url.contains("/checkout")) {
                    injectTumarPayBridge();
                }
                if (url != null && url.contains("/orders")) {
                    injectOrdersCancelBridge();
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
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
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
                boolean active = i == activeTab;
                label.setTextColor(active
                    ? getResources().getColor(R.color.colorPrimary, null)
                    : getResources().getColor(R.color.text_secondary, null));
                label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // ── Auto-login ─────────────────────────────────────────────────────────────

    private void fetchProfileAndAutoLogin() {
        SharedPreferences prefs = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        String appToken = prefs.getString(KEY_TOKEN, null);
        if (appToken == null) { webView.loadUrl(MARKET_URL); return; }

        ApiClient.getApiService(requireContext()).getUserProfile()
                .enqueue(new retrofit2.Callback<UserProfileResponse>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<UserProfileResponse> call,
                                           @NonNull retrofit2.Response<UserProfileResponse> resp) {
                        if (!isAdded()) return;
                        String phone = null;
                        if (resp.isSuccessful() && resp.body() != null) phone = resp.body().getPhone();
                        if (phone != null && !phone.isEmpty()) {
                            callMarketAutoLogin(phone);
                        } else {
                            mainHandler.post(() -> webView.loadUrl(MARKET_URL));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull retrofit2.Call<UserProfileResponse> call, @NonNull Throwable t) {
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
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                    try {
                        String s = resp.body() != null ? resp.body().string() : "";
                        String token = new JSONObject(s).optString("token");
                        if (!token.isEmpty()) marketToken = token;
                    } catch (Exception ignored) {}
                    mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
                }
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> { if (isAdded()) webView.loadUrl(MARKET_URL); });
        }
    }

    private void injectMarketToken() {
        if (marketToken == null) return;
        String safe = marketToken.replace("'", "\\'");
        String js = "(function(){var e=localStorage.getItem('token');" +
            "if(!e||e!=='" + safe + "'){localStorage.setItem('token','" + safe + "');window.location.reload();}" +
            "})();";
        webView.evaluateJavascript(js, null);
        tokenInjected = true;
    }

    // ── Checkout bridge injection ──────────────────────────────────────────────

    private void injectTumarPayBridge() {
        String js = "(function(){"
            + "if(window.__tumarPayInjected)return;"
            + "window.__tumarPayInjected=true;"
            // Hide all payment options except tumar_pay
            + "document.querySelectorAll('.payment-option').forEach(function(opt){"
            + "  var i=opt.querySelector('input[name=\"payment\"]');"
            + "  if(i&&i.value!=='tumar_pay')opt.style.display='none';"
            + "});"
            + "var inp=document.querySelector('input[value=\"tumar_pay\"]');"
            + "if(inp){inp.checked=true;"
            + "var card=inp.closest('.payment-option');"
            + "if(card){card.style.outline='2px solid #6200EE';card.style.borderRadius='8px';}}"
            + "var _orig=window.placeOrder;"
            + "window.placeOrder=function(){"
            + "var pay=document.querySelector('input[name=\"payment\"]:checked')?.value;"
            + "if(pay!=='tumar_pay'){if(typeof _orig==='function')_orig();return;}"
            + "var city=document.getElementById('delivery-city')?.value||'';"
            + "var street=(document.getElementById('delivery-street')?.value||'').trim();"
            + "var comment=(document.getElementById('delivery-comment')?.value||'').trim();"
            + "var errEl=document.getElementById('checkout-error');"
            + "if(!street){errEl.textContent='Введите адрес доставки';errEl.style.display='block';return;}"
            + "var addr=city+', '+street+(comment?' ('+comment+')':'');"
            + "var total=document.getElementById('co-total')?.textContent||'0';"
            + "var items=JSON.stringify(window.cartData?.items||[]);"
            + "var btn=document.querySelector('button[onclick=\"placeOrder()\"]');"
            + "if(btn){btn.disabled=true;btn.textContent='Обработка...';}"
            + "TumarBridge.payWithTumar(total,addr,items);"
            + "};"
            + "})();";
        webView.evaluateJavascript(js, null);
    }

    // ── Orders page: intercept cancel for tumar_pay orders ────────────────────

    private void injectOrdersCancelBridge() {
        String js = "(function(){"
            + "if(window.__tumarCancelInjected)return;"
            + "window.__tumarCancelInjected=true;"
            + "var _orig=window.cancelOrder;"
            + "window.cancelOrder=function(orderId){"
            + "var order=(window.allOrders||[]).find(function(o){return o.id===orderId;});"
            + "if(order&&order.payment_method==='tumar_pay'&&order.tumar_ref){"
            + "if(confirm('Отменить заказ? Средства вернутся на Tumar счёт')){"
            + "TumarBridge.cancelTumarPayOrder(order.tumar_ref,String(order.total),String(order.id));"
            + "}"
            + "}else{"
            + "if(typeof _orig==='function')_orig(orderId);"
            + "}"
            + "};"
            + "})();";
        webView.evaluateJavascript(js, null);
    }

    // ── Payment bottom sheet ───────────────────────────────────────────────────

    private void showPaymentBottomSheet() {
        if (getContext() == null || !isAdded()) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(8), 0, dp(32));

        // Drag handle
        View handle = new View(requireContext());
        handle.setBackgroundColor(Color.parseColor("#DDDDDD"));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(40), dp(4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.topMargin = dp(8);
        hp.bottomMargin = dp(16);
        root.addView(handle, hp);

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Tumar Pay");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#111827"));
        title.setPadding(dp(24), dp(4), dp(24), dp(12));
        root.addView(title);

        // Divider
        View div = new View(requireContext());
        div.setBackgroundColor(Color.parseColor("#F0F0F0"));
        root.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // "Tumar счет" option
        TextView balanceLabel = new TextView(requireContext());
        balanceLabel.setText("Загрузка...");
        balanceLabel.setTextSize(13);
        balanceLabel.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout optBalance = makeOptionRow("💳", "Tumar счет", balanceLabel);
        optBalance.setOnClickListener(v -> { sheet.dismiss(); proceedWithTumarBalance(); });
        root.addView(optBalance);

        // "В рассрочку" option
        LinearLayout optInstallment = makeOptionRow("📅", "В рассрочку", "3, 6, 12 месяцев");
        optInstallment.setOnClickListener(v -> { sheet.dismiss(); showInstallmentBottomSheet(); });
        root.addView(optInstallment);

        sheet.setContentView(root);
        sheet.show();

        // Fetch balance async
        ApiClient.getApiService(requireContext()).getUserProfile()
            .enqueue(new retrofit2.Callback<UserProfileResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<UserProfileResponse> call,
                                       @NonNull retrofit2.Response<UserProfileResponse> resp) {
                    mainHandler.post(() -> {
                        if (!sheet.isShowing()) return;
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getBalance() != null) {
                            BigDecimal bal = resp.body().getBalance();
                            NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));
                            balanceLabel.setText("Баланс: " + fmt.format(bal) + " ₸");
                            if (bal.compareTo(BigDecimal.valueOf(pendingAmount)) < 0) {
                                balanceLabel.setText("Недостаточно средств (баланс: " + fmt.format(bal) + " ₸)");
                                balanceLabel.setTextColor(Color.parseColor("#E31E24"));
                                optBalance.setEnabled(false);
                                optBalance.setAlpha(0.5f);
                            }
                        } else {
                            balanceLabel.setText("Баланс недоступен");
                        }
                    });
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<UserProfileResponse> call, @NonNull Throwable t) {
                    mainHandler.post(() -> { if (sheet.isShowing()) balanceLabel.setText("Баланс недоступен"); });
                }
            });
    }

    /** Creates a clickable option row with emoji, title and a subtitle TextView. */
    private LinearLayout makeOptionRow(String emoji, String titleText, TextView subtitleView) {
        Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(24), dp(16), dp(24), dp(16));
        row.setClickable(true);
        row.setFocusable(true);
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        row.setBackgroundResource(tv.resourceId);

        TextView emojitv = new TextView(ctx);
        emojitv.setText(emoji);
        emojitv.setTextSize(26);
        emojitv.setGravity(Gravity.CENTER);
        row.addView(emojitv, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tp.setMarginStart(dp(16));
        texts.setLayoutParams(tp);

        TextView t = new TextView(ctx);
        t.setText(titleText);
        t.setTextSize(15);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(Color.parseColor("#111827"));
        texts.addView(t);
        texts.addView(subtitleView);
        row.addView(texts);
        return row;
    }

    /** Creates a clickable option row with a plain string subtitle. */
    private LinearLayout makeOptionRow(String emoji, String titleText, String subtitle) {
        TextView sub = new TextView(requireContext());
        sub.setText(subtitle);
        sub.setTextSize(13);
        sub.setTextColor(Color.parseColor("#6B7280"));
        return makeOptionRow(emoji, titleText, sub);
    }

    // ── Tumar Balance payment ─────────────────────────────────────────────────

    private void proceedWithTumarBalance() {
        if (pendingAmount <= 0 || pendingAddress == null) {
            showErrorInWebView("Ошибка: данные заказа потеряны");
            return;
        }
        MarketPayRequest req = new MarketPayRequest(pendingAmount, pendingAddress, pendingItems);
        ApiClient.getApiService(requireContext()).marketPay(req)
            .enqueue(new retrofit2.Callback<MarketPayResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<MarketPayResponse> call,
                                       @NonNull retrofit2.Response<MarketPayResponse> resp) {
                    if (!isAdded()) return;
                    if (resp.isSuccessful() && resp.body() != null && resp.body().success) {
                        String orderRef = resp.body().orderRef;
                        // Create order in Go market so it appears in /orders
                        createGoMarketOrder(pendingAddress, orderRef);
                        showSuccessInWebView(orderRef);
                        mainHandler.post(() -> showNativeSuccess(orderRef, pendingAmount));
                    } else {
                        String msg = (resp.body() != null && resp.body().message != null)
                                ? resp.body().message : "Ошибка оплаты";
                        showErrorInWebView(msg);
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<MarketPayResponse> call, @NonNull Throwable t) {
                    showErrorInWebView("Ошибка сети. Проверьте подключение.");
                }
            });
    }

    private void saveDeliveryCode(String orderRef) {
        if (getContext() == null || orderRef == null) return;
        getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .edit().putString("market_delivery_code", orderRef).apply();
    }

    /** Creates a corresponding order in the Go market backend after successful Tumar Pay. */
    private void createGoMarketOrder(String address, String orderRef) {
        if (marketToken == null) return;
        try {
            JSONObject body = new JSONObject();
            body.put("delivery_address", address);
            body.put("payment_method", "tumar_pay");
            body.put("tumar_ref", orderRef);
            Request request = new Request.Builder()
                    .url(MARKET_URL + "/api/orders")
                    .header("Authorization", "Bearer " + marketToken)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException { r.close(); }
                @Override public void onFailure(@NonNull Call c, @NonNull IOException e) { /* silent */ }
            });
        } catch (Exception ignored) {}
    }

    // ── Installment flow ──────────────────────────────────────────────────────

    private void showInstallmentBottomSheet() {
        if (getContext() == null || !isAdded()) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(8), 0, dp(32));

        // Drag handle
        View handle = new View(requireContext());
        handle.setBackgroundColor(Color.parseColor("#DDDDDD"));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(40), dp(4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.topMargin = dp(8);
        hp.bottomMargin = dp(16);
        root.addView(handle, hp);

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Выберите срок рассрочки");
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#111827"));
        title.setPadding(dp(24), dp(4), dp(24), dp(12));
        root.addView(title);

        View div = new View(requireContext());
        div.setBackgroundColor(Color.parseColor("#F0F0F0"));
        root.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));
        String[] terms = {"3 мес.", "6 мес.", "12 мес."};
        double[] monthly = {pendingAmount / 3, pendingAmount / 6, pendingAmount / 12};

        for (int i = 0; i < terms.length; i++) {
            final String term = terms[i];
            String sub = "~" + fmt.format((long) monthly[i]) + " ₸ / мес.";
            LinearLayout row = makeOptionRow("📅", term, sub);
            row.setOnClickListener(v -> {
                sheet.dismiss();
                showBankWaitingAndDecline();
            });
            root.addView(row);
        }

        sheet.setContentView(root);
        sheet.show();
    }

    private void showBankWaitingAndDecline() {
        if (getContext() == null || !isAdded()) return;

        // Reset checkout button so it doesn't stay "Обработка..."
        resetCheckoutButton();

        AlertDialog waitingDialog = new AlertDialog.Builder(requireContext())
            .setTitle("🏦 Проверка заявки")
            .setMessage("⏳ Ожидание ответа банка...\n\nЭто займёт несколько секунд.")
            .setCancelable(false)
            .show();

        mainHandler.postDelayed(() -> {
            if (!isAdded() || getContext() == null) return;
            if (waitingDialog.isShowing()) waitingDialog.dismiss();
            new AlertDialog.Builder(requireContext())
                .setTitle("❌ Отказано")
                .setMessage("Отказано в предоставлении рассрочки.\n\nПопробуйте другой способ оплаты.")
                .setPositiveButton("Понятно", null)
                .setCancelable(true)
                .show();
        }, 5000);
    }

    // ── Cancel Tumar Pay order (called from JS bridge) ─────────────────────────

    private void cancelTumarPayOrderAsync(String orderRef, String goOrderId) {
        SharedPreferences prefs = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        String appToken = prefs.getString(KEY_TOKEN, "");
        if (appToken.isEmpty()) { showToastSafe("Ошибка авторизации"); return; }

        AlertDialog[] progressHolder = new AlertDialog[1];
        mainHandler.post(() -> {
            if (!isAdded() || getContext() == null) return;
            progressHolder[0] = new AlertDialog.Builder(requireContext())
                .setMessage("Отмена заказа...")
                .setCancelable(false)
                .show();
        });

        try {
            // Step 1: refund via Node.js wallet
            JSONObject body = new JSONObject();
            body.put("order_ref", orderRef);
            Request req = new Request.Builder()
                .url(NODE_URL + "/api/market/cancel")
                .header("Authorization", "Bearer " + appToken)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();
            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                    boolean ok = resp.isSuccessful();
                    resp.close();
                    // Step 2: cancel Go market order regardless of wallet result
                    // (wallet may already be refunded if called twice)
                    if (goOrderId != null && !goOrderId.isEmpty() && marketToken != null) {
                        try {
                            Request goReq = new Request.Builder()
                                .url(MARKET_URL + "/api/orders/" + goOrderId + "/cancel")
                                .header("Authorization", "Bearer " + marketToken)
                                .put(RequestBody.create("{}", MediaType.parse("application/json")))
                                .build();
                            httpClient.newCall(goReq).enqueue(new Callback() {
                                @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException { r.close(); }
                                @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {}
                            });
                        } catch (Exception ignored) {}
                    }
                    mainHandler.post(() -> {
                        if (progressHolder[0] != null && progressHolder[0].isShowing())
                            progressHolder[0].dismiss();
                        if (!isAdded()) return;
                        if (ok) {
                            showToastSafe("Заказ отменён. Средства возвращены на Tumar счёт.");
                        } else {
                            showToastSafe("Не удалось отменить заказ");
                        }
                        webView.reload();
                    });
                }
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> {
                        if (progressHolder[0] != null && progressHolder[0].isShowing())
                            progressHolder[0].dismiss();
                        showToastSafe("Ошибка сети");
                    });
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (progressHolder[0] != null && progressHolder[0].isShowing())
                    progressHolder[0].dismiss();
            });
        }
    }

    // ── WebView helpers ────────────────────────────────────────────────────────

    private void showSuccessInWebView(String orderRef) {
        String js = "(function(){"
            + "var l=document.getElementById('checkout-layout');"
            + "var s=document.getElementById('checkout-success');"
            + "if(l)l.style.display='none';"
            + "if(s)s.style.display='block';"
            + "var n=document.getElementById('order-number');"
            + "if(n)n.textContent='#" + orderRef + "';"
            + "})();";
        mainHandler.post(() -> { if (isAdded()) webView.evaluateJavascript(js, null); });
    }

    private void showErrorInWebView(String message) {
        String safe = message.replace("'", "\\'");
        String js = "(function(){"
            + "var btn=document.querySelector('button[onclick=\"placeOrder()\"]');"
            + "if(btn){btn.disabled=false;btn.textContent='Подтвердить заказ';}"
            + "var err=document.getElementById('checkout-error');"
            + "if(err){err.textContent='" + safe + "';err.style.display='block';}"
            + "})();";
        mainHandler.post(() -> { if (isAdded()) webView.evaluateJavascript(js, null); });
    }

    private void resetCheckoutButton() {
        String js = "var btn=document.querySelector('button[onclick=\"placeOrder()\"]');"
            + "if(btn){btn.disabled=false;btn.textContent='Подтвердить заказ';}";
        mainHandler.post(() -> { if (isAdded()) webView.evaluateJavascript(js, null); });
    }

    private void showNativeSuccess(String orderRef, double amount) {
        if (!isAdded() || getContext() == null) return;
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));
        String amountStr = fmt.format((long) amount) + " ₸";
        new AlertDialog.Builder(requireContext())
            .setTitle("Заказ оплачен!")
            .setMessage("Заказ " + orderRef + " на сумму " + amountStr + " успешно оплачен через Tumar счёт.")
            .setPositiveButton("Мои заказы", (d, w) -> selectTab(4))
            .setNegativeButton("Продолжить покупки", (d, w) -> selectTab(0))
            .setCancelable(false)
            .show();
    }

    private void showToastSafe(String msg) {
        mainHandler.post(() -> {
            if (isAdded() && getContext() != null)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    public boolean onBackPressed() {
        if (webView != null && webView.canGoBack()) { webView.goBack(); return true; }
        return false;
    }

    @Override
    public void onDestroyView() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSystemNavVisible(true);
        }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroyView();
    }

    // ── JavaScript Bridge ──────────────────────────────────────────────────────

    private class TumarBridge {

        @JavascriptInterface
        public void payWithTumar(String totalStr, String address, String itemsJson) {
            String cleaned = totalStr.replaceAll("[^0-9.]", "").trim();
            double amount;
            try { amount = Double.parseDouble(cleaned); } catch (NumberFormatException e) {
                showErrorInWebView("Не удалось определить сумму заказа"); return;
            }
            if (amount <= 0) { showErrorInWebView("Сумма заказа не может быть нулевой"); return; }
            if (getContext() == null) return;

            pendingAmount  = amount;
            pendingAddress = address;
            pendingItems   = itemsJson;

            mainHandler.post(() -> { if (isAdded()) showPaymentBottomSheet(); });
        }

        @JavascriptInterface
        public void cancelTumarPayOrder(String orderRef, String totalStr, String goOrderId) {
            if (getContext() == null || orderRef == null || orderRef.isEmpty()) return;
            cancelTumarPayOrderAsync(orderRef, goOrderId);
        }
    }
}
