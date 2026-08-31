package dev.soranerai.selectivewebviewtest;

import android.app.Activity;
import android.os.Bundle;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Isolated host app for validating the module against an embedded WebView. */
public final class WebViewTestActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.i("SelectiveWebViewTest", "SSL error=" + error.getPrimaryError()
                        + " url=" + error.getUrl());
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onReceivedError(
                    WebView view, int errorCode, String description, String failingUrl) {
                Log.i("SelectiveWebViewTest", "load error=" + errorCode
                        + " description=" + description + " url=" + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i("SelectiveWebViewTest", "page finished=" + url);
                super.onPageFinished(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
        webView.loadUrl(getIntent().getDataString() != null
                ? getIntent().getDataString()
                : "https://self-signed.badssl.com/");
    }
}
