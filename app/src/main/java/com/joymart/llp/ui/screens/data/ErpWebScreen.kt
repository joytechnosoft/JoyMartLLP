package com.joymart.llp.ui.screens.data

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ErpWebScreen(onBack:()->Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("ERP Web Workspace", modifier = Modifier.weight(1f).padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, "Reload") }
        }
        if (loading) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> WebView(context).apply {
                webView = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                webChromeClient = object : WebChromeClient() { override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress; loading = newProgress < 100 } }
                webViewClient = object : WebViewClient() { override fun onPageFinished(view: WebView?, url: String?) { loading = false } }
                // Uses the existing Laravel web login/session. Native API token is not exposed to JavaScript.
                loadUrl("https://joymartllp.com/core/login")
            } },
            update = { webView = it }
        )
    }
}
