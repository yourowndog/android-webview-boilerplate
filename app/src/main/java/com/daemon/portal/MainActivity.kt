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

    private val chatHost = "chatgpt.com"
    private fun loginUrlFor(target: String) =
        "https://chatgpt.com/auth/login?return_to=" + Uri.encode(target)

    enum class AuthState { Unknown, LoggedOut, SeekingLogin, LoggedIn }
    private var authState = AuthState.Unknown

    private fun getStartUrl(): String =
        prefs.getString("start_url", BuildConfig.HOME_URL) ?: BuildConfig.HOME_URL
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

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

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
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                fileChooser.launch(arrayOf("*/*"))
                return true
            }

            override fun onCreateWindow(
                v: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = v
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url.toString()
                return if (u.startsWith("http://") || u.startsWith("https://")) {
                    v.loadUrl(u); true
                } else {
                    true
                }
            }

            override fun onReceivedHttpError(
                v: WebView,
                req: WebResourceRequest,
                resp: WebResourceResponse
            ) {
                if (req.isForMainFrame && req.url.host?.contains(chatHost) == true &&
                    req.url.encodedPath?.startsWith("/g/") == true &&
                    resp.statusCode in listOf(401, 403, 404)
                ) {
                    if (authState != AuthState.SeekingLogin) {
                        authState = AuthState.SeekingLogin
                        setTargetAfterLogin(req.url.toString())
                        v.loadUrl(loginUrlFor(getTargetAfterLogin() ?: BuildConfig.HOME_URL))
                    }
                }
            }

            override fun onPageFinished(v: WebView, url: String) {
                if (Uri.parse(url).host?.contains(chatHost) == true) {
                    checkSession { loggedIn ->
                        if (loggedIn) {
                            authState = AuthState.LoggedIn
                            getTargetAfterLogin()?.let { t ->
                                setTargetAfterLogin(null)
                                if (!url.startsWith(t)) v.postDelayed({ v.loadUrl(t) }, 150)
                            }
                        }
                    }
                }
            }
        }

        startFlow()
    }

    private fun checkSession(cb: (Boolean) -> Unit) {
        if (webView.url?.contains(chatHost) != true) {
            webView.loadUrl("https://chatgpt.com/")
            webView.postDelayed({ checkSession(cb) }, 250)
            return
        }
        val js = """
            (async () => {
              try {
                const r = await fetch('/api/auth/session', {credentials:'include'});
                return r.ok;
              } catch(e) { return false; }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js) { s -> cb(s?.contains("true") == true) }
    }

    private fun startFlow() {
        val target = getStartUrl()
        checkSession { loggedIn ->
            if (loggedIn) {
                authState = AuthState.LoggedIn
                webView.loadUrl(target)
            } else {
                authState = AuthState.LoggedOut
                setTargetAfterLogin(target)
                webView.loadUrl(loginUrlFor(target))
            }
        }
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
