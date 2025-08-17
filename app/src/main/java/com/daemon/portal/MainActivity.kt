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
import android.webkit.WebStorage
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val prefs by lazy { getSharedPreferences("portal", MODE_PRIVATE) }

    enum class AuthState { Unknown, LoggedOut, SeekingLogin, LoggedIn }

    private var authState: AuthState = AuthState.Unknown
    private var targetAfterLogin: String? = null

    private fun getStartUrl(): String = prefs.getString("start_url", BuildConfig.HOME_URL) ?: BuildConfig.HOME_URL
    private fun setStartUrl(u: String) { prefs.edit().putString("start_url", u).apply() }
    private fun setTargetAfterLogin(u: String?) { prefs.edit().putString("target_after_login", u).apply() }
    private fun getTargetAfterLogin(): String? = prefs.getString("target_after_login", null)

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

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, BuildConfig.ALLOW_THIRD_PARTY_COOKIES)
        WebStorage.getInstance()

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
                    webView.loadUrl(href)
                }
                return false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url.toString()
                if (url.startsWith("intent://") || url.startsWith("android-app://")) {
                    return true
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url)
                }
                return true
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse,
            ) {
                if (request.isForMainFrame) {
                    val code = errorResponse.statusCode
                    val url = request.url
                    if (url.host == "chatgpt.com" && url.path?.startsWith("/g/") == true &&
                        (code == 401 || code == 403 || code == 404)
                    ) {
                        if (authState != AuthState.SeekingLogin) {
                            authState = AuthState.SeekingLogin
                            setTargetAfterLogin(request.url.toString())
                            webView.loadUrl("file:///android_asset/landing.html")
                        }
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                evaluateSession { loggedIn ->
                    if (loggedIn) {
                        if (authState != AuthState.LoggedIn) authState = AuthState.LoggedIn
                        getTargetAfterLogin()?.let { t ->
                            setTargetAfterLogin(null)
                            if (!view.url.orEmpty().startsWith(t)) {
                                view.postDelayed({ view.loadUrl(t) }, 200)
                            }
                        }
                    } else {
                        if (authState == AuthState.Unknown) authState = AuthState.LoggedOut
                    }
                }
            }
        }

        preflightAndNavigate(getStartUrl())
    }

    private fun preflightAndNavigate(target: String) {
        evaluateSession { loggedIn ->
            if (loggedIn) {
                authState = AuthState.LoggedIn
                webView.loadUrl(target)
            } else {
                authState = AuthState.LoggedOut
                setTargetAfterLogin(target)
                webView.loadUrl("file:///android_asset/landing.html")
            }
        }
    }

    private fun evaluateSession(cb: (Boolean) -> Unit) {
        val probe = "https://chatgpt.com/"
        val js = """
          (async () => {
            try {
              const r = await fetch('/api/auth/session', {credentials:'include'});
              return r.ok;
            } catch (e) { return false; }
          })();
        """.trimIndent()
        if (webView.url == null) webView.loadUrl(probe)
        webView.postDelayed({ webView.evaluateJavascript(js) { s -> cb(s?.contains("true") == true) } }, 300)
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
