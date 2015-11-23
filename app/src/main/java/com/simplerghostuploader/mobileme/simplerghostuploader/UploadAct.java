package com.simplerghostuploader.mobileme.simplerghostuploader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class UploadAct extends AppCompatActivity {

    public static String URL_RGHOST = "http://rghost.ru/";
    public static String URL_TEMPLATE_FINISH = "rghost.ru/";
    public static String URL_TEMPLATE_FINISH_2 = "rghost.net/";

    public static  int INPUT_FILE_REQUEST_CODE = 1;

    public final static int MODE_START = 1;
    public final static int MODE_START_CHOOSE = 2;
    public final static int MODE_CHOOSED = 3;
    public final static int MODE_START_SEND = 4;
    public final static int MODE_END_SEND = 5;

    private int mode = MODE_START;

    private WebView webView;
    private String resultURL = null;
    private String fileName = null;

    private EditText edResult;
    private TextView tvFileName;
    private Button btnChoose;
    private Button btnSend;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    private ValueCallback<Uri[]> mFilePathCallback;
    private ValueCallback<Uri> mUploadMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        edResult = (EditText)findViewById(R.id.etResult);
        tvFileName = (TextView)findViewById(R.id.tvFileName);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        btnChoose = (Button)findViewById(R.id.btnChoose);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendMailIntent = new Intent(Intent.ACTION_SEND);
                sendMailIntent.putExtra(Intent.EXTRA_SUBJECT, "URL fo file on RGhost.ru");
                sendMailIntent.putExtra(Intent.EXTRA_TEXT, resultURL);
                sendMailIntent.setType("text/plain");

                startActivity(Intent.createChooser(sendMailIntent, "Share URL"));

            }
        });

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send message to webview - click choose button by javascript
                setModeAndUpdateDisplay(MODE_START_CHOOSE);
                webView.loadUrl("javascript:{ var b=document.getElementById('choose'); b.click(); }");

            }
        });

        btnSend = (Button)findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // click on button send by javascript
                setModeAndUpdateDisplay(MODE_START_SEND);
                webView.loadUrl("javascript:{ var b=document.getElementById('commit'); b.click(); }");
            }
        });
        initActivity();

    }

    private void initActivity() {
        setModeAndUpdateDisplay(MODE_START);
        // set up an webView object
        setupWebView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_upload, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // clear form and mode
            initActivity();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setupWebView() {
        // prepare webview object
        //webView = (WebView)findViewById(R.id.webView);  //new WebView(this);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        // set object that handle finish parsing job
        webView.addJavascriptInterface(new LocalJavaScriptInterface(this), "GetResultUrlHandler");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // check - if page - uploaded, then - grab the url
                if ((url.contains(URL_TEMPLATE_FINISH) || url.contains(URL_TEMPLATE_FINISH_2)) && url.length() > 18) {
                    webView.loadUrl("javascript:window.GetResultUrlHandler.grabURL" +
                            "(document.getElementById('actions').getElementsByClassName('download')[0].getAttribute('href'));");
                }

            }
        });

        webView.setWebChromeClient(new WebChromeClient() {


            private void innerCallChooser() {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("file/*");
                UploadAct.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), INPUT_FILE_REQUEST_CODE);
            }
            //The undocumented magic method override
            //Eclipse will swear at you if you try to put @Override here
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {

                mUploadMessage = uploadMsg;
                innerCallChooser();
            }

            // For Android 3.0+
            public void openFileChooser( ValueCallback uploadMsg, String acceptType ) {
                mUploadMessage = uploadMsg;
                innerCallChooser();
            }

            //For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                mUploadMessage = uploadMsg;
                innerCallChooser();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // call standard file chooser and return choosed file
                // Double check that we don't have any existing callbacks
                if(mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                innerCallChooser();

                return true;
            }
        });

        if(webView.getUrl() == null) {
            // start load url in background
            webView.loadUrl(URL_RGHOST);
        }

    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || (mFilePathCallback == null && mUploadMessage == null)) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                    this.fileName = results[0].toString();
                }
        }

        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(results);
        }
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(results[0]);
        }

        mFilePathCallback = null;
        mUploadMessage = null;
        if (this.fileName == null || "".equals(this.fileName)) {
            setModeAndUpdateDisplay(MODE_START);
        } else {
            setModeAndUpdateDisplay(MODE_CHOOSED);
        }
        return;
    }

    class LocalJavaScriptInterface {

        private Context ctx;

        LocalJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void grabURL(String url) {
            setResultURL(url);
        }

        @JavascriptInterface
        public void showHTML(String html) {
            Log.v("...", "html="+html);
        }

    }

    private void setResultURL(String url) {
        this.resultURL = url;
        setModeAndUpdateDisplay(MODE_END_SEND);
    }


    public void setModeAndUpdateDisplay (int newMode) {
        this.mode = newMode;
        edResult.post(new Runnable() {
            @Override
            public void run() {

                switch (mode) {
                    case MODE_START:
                    case MODE_START_CHOOSE:
                        btnChoose.setVisibility(View.VISIBLE);
                        btnSend.setVisibility(View.GONE);
                        tvFileName.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        edResult.setVisibility(View.GONE);
                        fab.setVisibility(View.GONE);
                        break;
                    case MODE_CHOOSED:
                        btnChoose.setVisibility(View.GONE);
                        btnSend.setVisibility(View.VISIBLE);
                        tvFileName.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        edResult.setVisibility(View.GONE);
                        fab.setVisibility(View.GONE);
                        break;
                    case MODE_START_SEND:
                        btnChoose.setVisibility(View.GONE);
                        btnSend.setVisibility(View.GONE);
                        tvFileName.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        edResult.setVisibility(View.GONE);
                        fab.setVisibility(View.GONE);
                        break;
                    case MODE_END_SEND:
                        btnChoose.setVisibility(View.GONE);
                        btnSend.setVisibility(View.GONE);
                        tvFileName.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        edResult.setVisibility(View.VISIBLE);
                        fab.setVisibility(View.VISIBLE);
                        break;

                }
                edResult.setText(UploadAct.this.resultURL);
                tvFileName.setText(UploadAct.this.fileName);
                edResult.selectAll();
            }
        });
    }

}
