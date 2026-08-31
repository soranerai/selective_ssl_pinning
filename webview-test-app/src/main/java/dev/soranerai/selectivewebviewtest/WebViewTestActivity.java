package dev.soranerai.selectivewebviewtest;

import android.app.Activity;
import android.os.Bundle;
import android.net.http.SslError;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Isolated host app for validating the module against an embedded WebView. */
public final class WebViewTestActivity extends Activity {
    private WebView webView;
    private EditText urlField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(8, 8, 8, 4);
        urlField = new EditText(this);
        urlField.setSingleLine(true);
        urlField.setHint("https://example.com");
        urlField.setText(getIntent().getDataString() != null
                ? getIntent().getDataString() : "https://self-signed.badssl.com/");
        controls.addView(urlField, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button open = new Button(this);
        open.setText("Открыть");
        controls.addView(open, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(controls);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        Button back = navButton("Назад");
        Button forward = navButton("Вперёд");
        Button reload = navButton("Обновить");
        navigation.addView(back); navigation.addView(forward); navigation.addView(reload);
        root.addView(navigation);

        TextView status = new TextView(this);
        status.setTextColor(Color.DKGRAY);
        status.setSingleLine(true);
        status.setPadding(12, 2, 12, 4);
        root.addView(status);

        webView = new WebView(this);
        // The production module never changes WebView settings. This isolated test host enables
        // JavaScript solely because the public target page renders its application shell with JS.
        webView.getSettings().setJavaScriptEnabled(true);
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
                urlField.setText(url);
                status.setText("Загружено: " + url);
                super.onPageFinished(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        open.setOnClickListener(v -> loadEnteredUrl(status));
        urlField.setOnEditorActionListener((v, actionId, event) -> { loadEnteredUrl(status); return true; });
        back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        forward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        reload.setOnClickListener(v -> webView.reload());
        webView.loadUrl(urlField.getText().toString());
    }

    private Button navButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        return button;
    }

    private void loadEnteredUrl(TextView status) {
        String value = urlField.getText().toString().trim();
        if (value.isEmpty()) return;
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) value = "https://" + value;
        status.setText("Загрузка: " + value);
        webView.loadUrl(value);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
