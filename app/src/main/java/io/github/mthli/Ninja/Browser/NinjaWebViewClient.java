package io.github.mthli.Ninja.Browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.erish.wingbrowser.R;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;

import io.github.mthli.Ninja.Unit.BrowserUnit;
import io.github.mthli.Ninja.Unit.IntentUnit;
import io.github.mthli.Ninja.View.NinjaWebView;

public class NinjaWebViewClient extends WebViewClient {
    private NinjaWebView ninjaWebView;
    private Context context;
    private AdBlock adBlock;

    private String BANK_TID = "";

    final int RESCODE = 1;
    final String ISP_LINK =  "market://details?id=kvp.jjy.MispAndroid320";				// ISP 설치 링크
    final String NICE_URL = "https://web.nicepay.co.kr/smart/interfaceURL.jsp";			// NICEPAY SMART 요청 URL
    final String NICE_BANK_URL = "https://web.nicepay.co.kr/smart/bank/payTrans.jsp";	// 계좌이체 거래 요청 URL
    final String KFTC_LINK = "market://details?id=com.kftc.bankpay.android";			//	금융결제원 설치 링크

    private boolean white;
    public void updateWhite(boolean white) {
        this.white = white;
    }

    private boolean enable;
    public void enableAdBlock(boolean enable) {
        this.enable = enable;
    }

    public NinjaWebViewClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
        this.context = ninjaWebView.getContext();
        this.adBlock = ninjaWebView.getAdBlock();
        this.white = false;
        this.enable = true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        if (view.getTitle() == null || view.getTitle().isEmpty()) {
            ninjaWebView.update(context.getString(R.string.album_untitled), url);
        } else {
            ninjaWebView.update(view.getTitle(), url);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (!ninjaWebView.getSettings().getLoadsImagesAutomatically()) {
            ninjaWebView.getSettings().setLoadsImagesAutomatically(true);
        }

        if (view.getTitle() == null || view.getTitle().isEmpty()) {
            ninjaWebView.update(context.getString(R.string.album_untitled), url);
        } else {
            ninjaWebView.update(view.getTitle(), url);
        }

        if (ninjaWebView.isForeground()) {
            ninjaWebView.invalidate();
        } else {
            ninjaWebView.postInvalidate();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        final WebView targetWebView = view;
        Intent intent;

        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("javascript:")) {
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ex) {
                return false;
            }

            Uri uri = Uri.parse(intent.getDataString());
            intent = new Intent(Intent.ACTION_VIEW, uri);

            try {
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                if(url.contains("ispmobile")) {
//                    TODO : ispmobie exception
                } else if( url.startsWith("intent://")) {//intent 형태의 스키마 처리
                    try {
                        Intent excepIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        String packageNm = excepIntent.getPackage();

                        excepIntent = new Intent(Intent.ACTION_VIEW);
                        excepIntent.setData(Uri.parse("market://search?q=pname:"+ packageNm));
                        context.startActivity(excepIntent);

                        return true;
                    } catch (URISyntaxException e1) {}
                } else if (url != null	&& (url.contains("vguard")
                        || url.contains("droidxantivirus")
                        || url.contains("lottesmartpay")
                        || url.contains("smshinhancardusim://")
                        || url.contains("shinhan-sr-ansimclick")
                        || url.contains("v3mobile")
                        || url.endsWith(".apk")
                        || url.contains("smartwall://")
                        || url.contains("appfree://")
                        || url.contains("market://")
                        || url.contains("ansimclick://")
                        || url.contains("ansimclickscard")
                        || url.contains("ansim://")
                        || url.contains("mpocket")
                        || url.contains("mvaccine")
                        || url.contains("market.android.com")
                        || url.contains("http://m.ahnlab.com/kr/site/download"))) {

                    try{
                        try {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        } catch (URISyntaxException ex) {
                            Log.i("SHS", "Bad URI " + url + ":" + ex.getMessage());
                            return false;
                        }

                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(intent);
                        return true;
                    } catch (ActivityNotFoundException ee) {
                        Log.i("SHS", "Activity Exception "+e.getMessage());
                        e.printStackTrace();
                        return false;
                    } catch (Exception ee) {
                        Log.i("SHS", "Exception "+e.getMessage());
                        return false;
                    }
                }
            }
        }

        return false;
    }

    @Deprecated
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (enable && !white && adBlock.isAd(url)) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }

        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (enable && !white && adBlock.isAd(request.getUrl().toString())) {
                return new WebResourceResponse(
                        BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                        BrowserUnit.URL_ENCODING,
                        new ByteArrayInputStream("".getBytes())
                );
            }
        }

        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message dontResend, final Message resend) {
        Context holder = IntentUnit.getContext();
        if (holder == null || !(holder instanceof Activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(holder);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_title_resubmission);
        builder.setMessage(R.string.dialog_content_resubmission);
        builder.setPositiveButton(R.string.dialog_button_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resend.sendToTarget();
            }
        });
        builder.setNegativeButton(R.string.dialog_button_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dontResend.sendToTarget();
            }
        });

        builder.create().show();
    }

    @Override
    public void onReceivedSslError(WebView view, @NonNull final SslErrorHandler handler, SslError error) {
        Context holder = IntentUnit.getContext();
        if (holder == null || !(holder instanceof Activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(holder);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_title_warning);
        builder.setMessage(R.string.dialog_content_ssl_error);
        builder.setPositiveButton(R.string.dialog_button_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.proceed();
            }
        });
        builder.setNegativeButton(R.string.dialog_button_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
            dialog.show();
        } else {
            handler.proceed();
        }
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host, String realm) {
        Context holder = IntentUnit.getContext();
        if (holder == null || !(holder instanceof Activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(holder);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_title_sign_in);

        LinearLayout signInLayout = (LinearLayout) LayoutInflater.from(holder).inflate(R.layout.dialog_sign_in, null, false);
        final EditText userEdit = (EditText) signInLayout.findViewById(R.id.dialog_sign_in_username);
        final EditText passEdit = (EditText) signInLayout.findViewById(R.id.dialog_sign_in_password);
        passEdit.setTypeface(Typeface.DEFAULT);
        passEdit.setTransformationMethod(new PasswordTransformationMethod());
        builder.setView(signInLayout);

        builder.setPositiveButton(R.string.dialog_button_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String user = userEdit.getText().toString().trim();
                String pass = passEdit.getText().toString().trim();
                handler.proceed(user, pass);
            }
        });

        builder.setNegativeButton(R.string.dialog_button_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.cancel();
            }
        });

        builder.create().show();
    }
}
