package com.pande.muying;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.just.agentweb.AbsAgentWebSettings;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.IAgentWebSettings;
import com.just.agentweb.PermissionInterceptor;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "222222222";
    private LinearLayout llWebView;
    private AgentWeb mAgentWeb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        llWebView = findViewById(R.id.ll_web_view);
        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(llWebView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                .useDefaultIndicator()
                .setAgentWebWebSettings(getSettings())
                .setWebViewClient(mWebViewClient)//WebViewClient ， 与 WebView 使用一致 ，但是请勿获取WebView调用setWebViewClient(xx)方法了,会覆盖AgentWeb DefaultWebClient,同时相应的中间件也会失效。
                .setWebChromeClient(mWebChromeClient) //WebChromeClient
//                .useMiddlewareWebChrome(new DefaultChromeClient())
                .setPermissionInterceptor(mPermissionInterceptor) //权限拦截 2.0.0 加入。
                .createAgentWeb()
                .ready()
                .go("http://www.pandamuying.com/crm");


    }

    private boolean videoFlag = false;
    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //do you  work
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            Logger.e(url);
            if (!TextUtils.isEmpty(url)) {
                videoFlag = url.contains("vedio");
            }
            if (url.trim().startsWith("tel")) {//特殊情况tel，调用系统的拨号软件拨号【<a href="tel:1111111111">1111111111</a>】
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else {
                String port = url.substring(url.lastIndexOf(":") + 1, url.lastIndexOf("/"));//尝试要拦截的视频通讯url格式(808端口)：【http://xxxx:808/?roomName】
                if (port.equals("808")) {//特殊情况【若打开的链接是视频通讯地址格式则调用系统浏览器打开】
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } else {//其它非特殊情况全部放行
                    view.loadUrl(url);
                }
            }
            return true;
        }


    };
    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            //do you work
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            Log.i("2222222222", origin);
            callback.invoke(origin, true, false);
            super.onGeolocationPermissionsShowPrompt(origin, callback);
        }
    };


    protected PermissionInterceptor mPermissionInterceptor = new PermissionInterceptor() {

        /**
         * PermissionInterceptor 能达到 url1 允许授权， url2 拒绝授权的效果。
         * @param url
         * @param permissions
         * @param action
         * @return true 该Url对应页面请求权限进行拦截 ，false 表示不拦截。
         */
        @Override
        public boolean intercept(String url, String[] permissions, String action) {
            Log.i(TAG, "mUrl:" + url + "  permission:" + permissions.toString() + " action:" + action);
            return false;
        }
    };

    private IAgentWebSettings getSettings() {
        return new AbsAgentWebSettings() {
            @Override
            protected void bindAgentWebSupport(AgentWeb agentWeb) {

            }

            @Override
            public IAgentWebSettings toSetting(WebView webView) {
                super.toSetting(webView);
                WebSettings webSettings = getWebSettings();
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setDefaultTextEncodingName("UTF-8");
                webSettings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
                webSettings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true
                // 是否允许通过file url加载的Javascript读取本地文件，默认值 false
                if (Build.VERSION.SDK_INT >= 16) {

                    webSettings.setAllowFileAccessFromFileURLs(false);
                    // 是否允许通过file url加载的Javascript读取全部资源(包括文件,http,https)，默认值 false
                    webSettings.setAllowUniversalAccessFromFileURLs(false);
                }

                if (Build.VERSION.SDK_INT >= 19) {
                    webSettings.setUseWideViewPort(true);
                }
                //设置定位的数据库路径
                String dir = MainActivity.this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
//        settings.setGeolocationDatabasePath(dir);
                webSettings.setGeolocationEnabled(true);
                webSettings.setAppCacheMaxSize(Long.MAX_VALUE);
                webSettings.setAppCachePath(MainActivity.this.getDir("appcache", 0).getPath());
                webSettings.setGeolocationDatabasePath(MainActivity.this.getDir("geolocation", 0).getPath());
                //开启JavaScript支持
                webSettings.setJavaScriptEnabled(true);
                // 支持缩放
                webSettings.setSupportZoom(true);
                return this;
            }


        };
    }

   /* private IAgentWebSettings getSettings() {
        return new AbsAgentWebSettings() {
            @Override
            protected void bindAgentWebSupport(AgentWeb agentWeb) {

            }

            @Override
            public IAgentWebSettings toSetting(WebView webView) {
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);//允许使用js

                */

    /**
     * LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
     * LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
     * LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
     * LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
     *//*
                webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);//不使用缓存，只从网络获取数据.

                //支持屏幕缩放
                webSettings.setSupportZoom(true);
                webSettings.setBuiltInZoomControls(true);

                //不显示webview缩放按钮
//        webSettings.setDisplayZoomControls(false);
                return (IAgentWebSettings) webSettings;
            }
        };
    }*/
    @Override
    protected void onPause() {
        mAgentWeb.getWebLifeCycle().onPause();
        super.onPause();

    }

    @Override
    protected void onResume() {
        mAgentWeb.getWebLifeCycle().onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mAgentWeb.getWebLifeCycle().onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (mAgentWeb != null && mAgentWeb.handleKeyEvent(keyCode, event)) {
            return true;
        } else {
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

}
