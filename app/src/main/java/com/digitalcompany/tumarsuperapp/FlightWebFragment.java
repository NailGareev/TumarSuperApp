package com.digitalcompany.tumarsuperapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FlightWebFragment extends Fragment {

    private static final String ARG_URL   = "url";
    private static final String ARG_TITLE = "title";
    private WebView webView;
    private ProgressBar progressBar;

    public static FlightWebFragment newInstance(String url) {
        return newInstance(url, "Aviasales");
    }

    public static FlightWebFragment newInstance(String url, String title) {
        FlightWebFragment f = new FlightWebFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_TITLE, title);
        f.setArguments(args);
        return f;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_flight_web, container, false);

        webView     = root.findViewById(R.id.webview_flights);
        progressBar = root.findViewById(R.id.progress_flights);

        String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "Aviasales") : "Aviasales";
        ((android.widget.TextView) root.findViewById(R.id.tv_web_title)).setText(title);

        root.findViewById(R.id.btn_flight_close).setOnClickListener(v -> goBack());

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10; Mobile) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        String url = getArguments() != null ? getArguments().getString(ARG_URL, "") : "";
        webView.loadUrl(url);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack();
                    else goBack();
                }
            });

        return root;
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

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
