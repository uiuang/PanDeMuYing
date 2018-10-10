package com.pande.muying.utils;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.pande.muying.R;

public class X5WebViewActivity extends AppCompatActivity {
    private final static String url = "http://www.pandamuying.com/crm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_x5_web_view);
        X5WebView webView = (X5WebView) findViewById(R.id.full_web_webview);
        webView.loadUrl(url);
    }
}
