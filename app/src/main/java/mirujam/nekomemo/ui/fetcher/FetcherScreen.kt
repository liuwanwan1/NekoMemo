package mirujam.nekomemo.ui.fetcher

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mirujam.nekomemo.navigation.Route
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.component.LocalSnackbarHostState

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FetcherScreen(
    navController: NavHostController,
    viewModel: FetcherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val parseResult by viewModel.parseResult.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val navigateToExtract by viewModel.navigateToExtract.collectAsState()

    var showHtmlSheet by rememberSaveable { mutableStateOf(false) }
    var htmlContent by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    var isLoading by rememberSaveable { mutableStateOf(false) }
    var loadProgress by rememberSaveable { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val isSnackbarVisible = snackbarHostState.currentSnackbarData != null
    val fabPadding by animateDpAsState(targetValue = if (isSnackbarVisible) 64.dp else 0.dp, label = "fabPadding")

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewState by rememberSaveable { mutableStateOf<Bundle?>(null) }

    LaunchedEffect(navigateToExtract) {
        if (navigateToExtract) {
            val json = viewModel.getExtractedJson()
            if (json != null) {
                Log.d("FetcherScreen", "Storing JSON in SharedDataStore, length: ${json.length}")
                val success = viewModel.saveToSharedDataStore(json)
                if (success) {
                    Log.d("FetcherScreen", "JSON saved successfully")
                    navController.navigate(Route.Extract.route)
                } else {
                    Log.e("FetcherScreen", "Failed to save JSON")
                    snackbarHostState.showSnackbar("Failed to save extracted data")
                }
            } else {
                Log.w("FetcherScreen", "No JSON data available")
            }
            viewModel.onNavigatedToExtract()
        }
    }

    LaunchedEffect(parseResult) {
        parseResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResult()
        }
    }

    if (showHtmlSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHtmlSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val ctx = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HTML Source",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("HTML Source", htmlContent))
                            Toast.makeText(ctx, "HTML copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy HTML",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = htmlContent.ifEmpty { "No HTML content available" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Route.Fetcher.title,
                actions = {
                    IconButton(onClick = { webViewRef?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                        val decoded = viewModel.decodeHtml(html)
                        coroutineScope.launch(Dispatchers.Main) {
                            htmlContent = decoded
                            showHtmlSheet = true
                        }
                    } }) {
                        Icon(imageVector = Icons.Outlined.Code, contentDescription = "View HTML")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    webViewRef?.let { webView ->
                        viewModel.clearResult()
                        webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                            val decoded = viewModel.decodeHtml(html)
                            if (decoded.isNotBlank()) {
                                viewModel.parseHtml(decoded)
                            }
                        }
                    }
                },
                modifier = Modifier.padding(bottom = fabPadding),
                shape = MaterialTheme.shapes.small,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.Description, "Extract", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress.toFloat() / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                if (isParsing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.defaultTextEncodingName = "utf-8"

                                val zhHeaders = mapOf("Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8")

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val url = request.url.toString()
                                        if (!url.startsWith("http://") && !url.startsWith("https://")) return true
                                        view.loadUrl(url, zhHeaders)
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        loadProgress = 0
                                    }

                                    override fun onPageFinished(view: WebView, url: String?) {
                                        isLoading = false
                                        loadProgress = 100
                                        url?.let { viewModel.setCurrentUrl(it) }
                                    }

                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                        if (request.isForMainFrame) isLoading = false
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                                        loadProgress = newProgress
                                        if (newProgress == 100) isLoading = false
                                    }
                                }

                                val savedState = webViewState
                                if (savedState != null) {
                                    restoreState(savedState)
                                } else {
                                    loadUrl(currentUrl, zhHeaders)
                                }
                            }.also {
                                webViewRef = it
                            }
                        },
                        update = { webView -> }
                    )

                    DisposableEffect(Unit) {
                        onDispose {
                            webViewRef?.let { webView ->
                                val state = Bundle()
                                webView.saveState(state)
                                webViewState = state
                                webView.destroy()
                            }
                            webViewRef = null
                        }
                    }

                    if (isLoading && loadProgress > 0 && loadProgress < 100) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                BackHandler(enabled = webViewRef?.canGoBack() == true) {
                    webViewRef?.goBack()
                }
            }
        }
    }
}
