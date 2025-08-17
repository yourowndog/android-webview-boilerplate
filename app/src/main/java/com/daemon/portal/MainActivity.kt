package com.daemon.portal

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val prefs by lazy { getSharedPreferences("portal", MODE_PRIVATE) }

    private fun getStartUrl(): String = prefs.getString("start_url", BuildConfig.HOME_URL)!!
    private fun setStartUrl(u: String) { prefs.edit().putString("start_url", u).apply() }
    private fun getTargetAfterLogin(): String? = prefs.getString("target_after_login", null)
    private fun setTargetAfterLogin(u: String?) { prefs.edit().putString("target_after_login", u).apply() }

    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            filePathCallback?.onReceiveValue(uris.toTypedArray())
            filePathCallback = null
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ))

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.allowFileAccess = true

        if (BuildConfig.FORCE_DESKTOP_MODE) {
            settings.userAgentString =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        }

        if (BuildConfig.ALLOW_THIRD_PARTY_COOKIES) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }

            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                fileChooser.launch(arrayOf("*/*"))
                return true
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                val href = view?.hitTestResult?.extra
                if (href != null) {
                    view.loadUrl(href)
                }
                return false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse,
            ) {
                val url = request.url
                if (
                    request.isForMainFrame &&
                    url.host?.endsWith("chatgpt.com") == true &&
                    url.path?.startsWith("/g/") == true &&
                    errorResponse.statusCode in setOf(401, 403, 404)
                ) {
                    setTargetAfterLogin(url.toString())
                    view.loadUrl(BuildConfig.HOME_URL)
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                val uri = Uri.parse(url)
                val target = getTargetAfterLogin()
                if (uri.host == "chatgpt.com" && target != null && !url.contains("/g/")) {
                    setTargetAfterLogin(null)
                    view.postDelayed({ view.loadUrl(target) }, 300)
                }
            }
        }

        webView.loadUrl(getStartUrl())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_set_url) {
            val input = EditText(this)
            input.setText(getStartUrl())
            AlertDialog.Builder(this)
                .setTitle("Set Portal URL")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val newUrl = input.text.toString()
                    if (newUrl.startsWith("https://")) {
                        setStartUrl(newUrl)
                        webView.loadUrl(newUrl)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
