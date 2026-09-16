package com.schulte.trainer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 舒尔特方格训练 —— WebView 外壳。
 *
 * 整个应用逻辑都在 assets/schulte.html 里（就是电脑上双击打开的那个文件），
 * 这里只负责把它装进一个原生窗口，并打开必要的开关：
 *   - DOM Storage 必须开，否则训练成绩存不下来（localStorage 会抛异常）
 *   - 关闭缩放与滚动条，避免训练时误触放大
 *   - 训练期间屏幕常亮
 *   - 物理返回键先交给网页处理（暂停训练），处理不了才退出应用
 */
public class MainActivity extends Activity {

    private static final String START_URL = "file:///android_asset/schulte.html";

    private WebView web;
    private boolean pageReady = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 训练中不要熄屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        web.setBackgroundColor(0xFFF4F5F7);          // 与网页底色一致，避免白屏闪一下
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                // ★ 成绩记录依赖它
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setTextZoom(100);                          // 不跟随系统字体缩放，保证方格不被撑破
        s.setMediaPlaybackRequiresUserGesture(false);

        setContentView(web);
        web.loadUrl(START_URL);
    }

    /**
     * 物理返回键：先问网页要不要处理。
     * 训练中 -> 暂停；结算页 -> 回设置；否则交还给系统（退出应用）。
     */
    @Override
    public void onBackPressed() {
        if (web == null || !pageReady) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript(
                "(function(){try{return (window.__schulteBack && window.__schulteBack())?'1':'0';}"
                        + "catch(e){return '0';}})()",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"\"1\"".equals(value)) {
                            finish();                // 网页没接手，退出应用
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
