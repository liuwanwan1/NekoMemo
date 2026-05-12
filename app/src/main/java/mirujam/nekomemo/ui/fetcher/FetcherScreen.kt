package mirujam.nekomemo.ui.fetcher

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.theme.ButtonShapes
import mirujam.nekomemo.ui.theme.DialogShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FetcherScreen(
    viewModel: FetcherViewModel = hiltViewModel()
) {
    val isParsing by viewModel.isParsing.collectAsState()
    val parseResult by viewModel.parseResult.collectAsState()
    val parsedQuestions by viewModel.parsedQuestions.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showHtmlSheet by rememberSaveable { mutableStateOf(false) }
    var htmlContent by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    var bankTitle by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("General") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var loadProgress by rememberSaveable { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(text = "Save Question Bank") },
            text = {
                Column {
                    OutlinedTextField(
                        value = bankTitle,
                        onValueChange = { bankTitle = it },
                        label = { Text("Bank Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${parsedQuestions.size} questions will be saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveQuestions(bankTitle, category)
                        showSaveDialog = false
                        bankTitle = ""
                        category = "General"
                    },
                    enabled = bankTitle.isNotBlank(),
                    shape = ButtonShapes
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = DialogShapes
        )
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
                Text(
                    text = "HTML Source",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
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
                title = "Question Fetcher",
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                                val decoded = if (html != null) {
                                    try {
                                        org.json.JSONObject("{\"v\":$html}").getString("v")
                                    } catch (_: Exception) {
                                        html
                                    }
                                } else ""
                                coroutineScope.launch(Dispatchers.Main) {
                                    htmlContent = decoded
                                    showHtmlSheet = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = "View HTML"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    webViewRef?.let { webView ->
                        viewModel.clearResult()
                        webView.evaluateJavascript(JS_INJECTION_SCRIPT, null)
                    }
                },
                shape = MaterialTheme.shapes.small,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Parse Questions",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress.toFloat() / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                if (isParsing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            val locale = Locale.CHINA
                            Locale.setDefault(locale)
                            val config = Configuration(context.resources.configuration)
                            config.setLocale(locale)
                            val localizedContext = context.createConfigurationContext(config)

                            viewModel.getOrCreateWebView {
                                WebView(localizedContext).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView,
                                            request: WebResourceRequest
                                        ): Boolean {
                                            val url = request.url.toString()
                                            return !url.startsWith("http://") && !url.startsWith("https://")
                                        }

                                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                            isLoading = true
                                            loadProgress = 0
                                        }

                                        override fun onPageFinished(view: WebView, url: String?) {
                                            isLoading = false
                                            loadProgress = 100
                                            url?.let { viewModel.setUrlInput(it) }
                                        }

                                        override fun onReceivedError(
                                            view: WebView,
                                            request: WebResourceRequest,
                                            error: WebResourceError
                                        ) {
                                            if (request.isForMainFrame) {
                                                isLoading = false
                                            }
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                                            loadProgress = newProgress
                                            if (newProgress == 100) {
                                                isLoading = false
                                            }
                                        }
                                    }

                                    addJavascriptInterface(
                                        QuestionParserInterface { jsonString ->
                                            val questions = parseJsonQuestions(jsonString)
                                            viewModel.onQuestionsParsed(questions)
                                            if (questions.isNotEmpty()) {
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    showSaveDialog = true
                                                }
                                            }
                                        },
                                        "QuestionParser"
                                    )
                                    loadUrl(currentUrl)
                                }
                            }.also { webViewRef = it }
                        },
                        update = { webView ->
                            pendingUrl?.let { url ->
                                webView.loadUrl(url)
                                pendingUrl = null
                            }
                        },
                        onRelease = { _ ->
                            // Don't destroy here to keep it alive in ViewModel
                            webViewRef = null
                        }
                    )

                    if (isLoading && loadProgress > 0 && loadProgress < 100) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                BackHandler(enabled = webViewRef?.canGoBack() == true) {
                    webViewRef?.goBack()
                }

                if (parsedQuestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Found ${parsedQuestions.size} questions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ButtonShapes
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SaveAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save to Library")
                            }
                        }
                    }
                }

                parseResult?.let { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

private class QuestionParserInterface(
    private val onResult: (String) -> Unit
) {
    @JavascriptInterface
    fun onQuestionsExtracted(jsonString: String) {
        onResult(jsonString)
    }
}

private fun parseJsonQuestions(jsonString: String): List<ParsedQuestion> {
    return try {
        val array = org.json.JSONArray(jsonString)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val optionsArray = obj.optJSONArray("options")
            val options = if (optionsArray != null) {
                (0 until optionsArray.length()).map { j -> optionsArray.getString(j) }
            } else emptyList()
            ParsedQuestion(
                text = obj.optString("text", ""),
                options = options,
                correctIndex = obj.optInt("correctIndex", 0)
            )
        }.filter { it.text.isNotBlank() && it.options.isNotEmpty() }
    } catch (_: Exception) {
        emptyList()
    }
}

private const val JS_INJECTION_SCRIPT = """
(function() {
    var questions = [];
    var questionDivs = document.querySelectorAll('.TiMu');
    if (questionDivs.length === 0) {
        questionDivs = document.querySelectorAll('[class*="question"]');
    }
    if (questionDivs.length === 0) {
        questionDivs = document.querySelectorAll('[class*="timu"]');
    }
    questionDivs.forEach(function(div) {
        var textEl = div.querySelector('.Zy_TItle, [class*="title"], h3, h2');
        var text = textEl ? textEl.innerText.trim() : '';
        var options = [];
        var optionEls = div.querySelectorAll('ul li, .Zy_ulTop li, [class*="option"]');
        optionEls.forEach(function(opt) {
            var optText = opt.innerText.trim();
            if (optText) options.push(optText);
        });
        var correctIndex = 0;
        var correctEl = div.querySelector('.Py_answer .colorDeepGreen, [class*="correct"], [class*="answer"]');
        if (correctEl) {
            var correctText = correctEl.innerText.trim();
            var letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
            for (var i = 0; i < letters.length; i++) {
                if (correctText.indexOf(letters[i]) >= 0) {
                    correctIndex = i;
                    break;
                }
            }
        }
        if (text) {
            questions.push({
                text: text,
                options: options,
                correctIndex: correctIndex
            });
        }
    });
    QuestionParser.onQuestionsExtracted(JSON.stringify(questions));
})();
"""
