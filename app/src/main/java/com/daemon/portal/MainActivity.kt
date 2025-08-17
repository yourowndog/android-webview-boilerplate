package com.daemon.portal

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebStorage
import android.webkit.WebResourceResponse
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var statusView: TextView
    private var isLoggedIn = false
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingUrl: String? = null

    private val prefs by lazy { getSharedPreferences("portal", MODE_PRIVATE) }

    private fun getStartUrl(): String = prefs.getString("start_url", BuildConfig.HOME_URL)!!
    private fun setStartUrl(u: String) { prefs.edit().putString("start_url", u).apply() }
    private fun onLoggedIn() {
        isLoggedIn = true
        statusView.text = "Signed in"
        val target = pendingUrl ?: getStartUrl()
        pendingUrl = null
        webView.post { webView.loadUrl(target) }
    }

    private fun onLoggedOut() {
        isLoggedIn = false
        statusView.text = "Not signed in"
    }

    inner class JsBridge {
        @JavascriptInterface
        fun loggedIn() { runOnUiThread { onLoggedIn() } }

        @JavascriptInterface
        fun loggedOut() { runOnUiThread { onLoggedOut() } }
    }

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
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusView = TextView(this).apply {
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)
            text = "Checking..."
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.BLACK)
        }
        val reload = Button(this).apply {
            text = "Reload"
            setTextColor(Color.WHITE)
        }
        val home = Button(this).apply {
            text = "Home"
            setTextColor(Color.WHITE)
        }
        val openChrome = Button(this).apply {
            text = "Chrome"
            setTextColor(Color.WHITE)
        }
        val clearCookies = Button(this).apply {
            text = "Clear"
            setTextColor(Color.WHITE)
        }
        actions.addView(statusView)
        actions.addView(reload)
        actions.addView(home)
        actions.addView(openChrome)
        actions.addView(clearCookies)
        root.addView(actions)
        root.addView(webView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        setContentView(root)

        reload.setOnClickListener { webView.reload() }
        home.setOnClickListener { webView.loadUrl(getStartUrl()) }
        openChrome.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView.url ?: getStartUrl()))
            startActivity(intent)
        }
        clearCookies.setOnClickListener {
            CookieManager.getInstance().removeAllCookies { webView.loadUrl(BuildConfig.HOME_URL) }
        }

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

        webView.addJavascriptInterface(JsBridge(), "Android")

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
                if (request.isForMainFrame) {
                    val code = errorResponse.statusCode
                    val url = request.url.toString()
                    if (url.startsWith("https://chatgpt.com/g/") && (code == 401 || code == 403 || code == 404)) {
                        pendingUrl = url
                        view.loadUrl("https://chatgpt.com/")
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(
                    """
                    (async () => {
                      try {
                        const r = await fetch('/api/auth/session', {credentials:'include'});
                        if (r.ok) { Android.loggedIn(); } else { Android.loggedOut(); }
                      } catch(e) { Android.loggedOut(); }
                    })();
                    """.trimIndent(), null
                )
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
