package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : ComponentActivity() {

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = if (result.data == null) {
                null
            } else {
                val dataString = result.data?.dataString
                val clipData = result.data?.clipData
                if (clipData != null) {
                    val uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    uris
                } else if (dataString != null) {
                    arrayOf(Uri.parse(dataString))
                } else {
                    null
                }
            }
            uploadMessage?.onReceiveValue(results)
        } else {
            uploadMessage?.onReceiveValue(null)
        }
        uploadMessage = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF4F3EF))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    BookWebViewContainer(
                        onOpenFileChooser = { filePathCallback, fileChooserParams ->
                            uploadMessage?.onReceiveValue(null)
                            uploadMessage = filePathCallback
                            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "image/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            try {
                                fileChooserLauncher.launch(intent)
                            } catch (e: Exception) {
                                uploadMessage?.onReceiveValue(null)
                                uploadMessage = null
                            }
                        }
                    )
                }
            }
        }
    }
}

class WebDbInterface(private val context: Context, private val webView: WebView) {
    private val fileName = "darwish_app_state.json"

    private fun findActivity(ctx: Context): Activity? {
        var currentContext = ctx
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            val base = currentContext.baseContext
            if (base == currentContext || base == null) {
                break
            }
            currentContext = base
        }
        return null
    }

    @JavascriptInterface
    fun saveAppState(stateJson: String?) {
        if (stateJson.isNullOrEmpty()) return
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(stateJson.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun getAppState(): String {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (file != null && file.exists()) {
                context.openFileInput(fileName).use { input ->
                    input.bufferedReader().use { it.readText() }
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    @JavascriptInterface
    fun printPdf() {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            try {
                val activity = findActivity(context)
                if (activity != null) {
                    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                    if (printManager != null) {
                        val jobName = "Darwish_Encyclopedia_Document"
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun generatePagesWithAi(rawText: String) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                webView.evaluateJavascript(
                    "javascript:onAiPagesGenerated(false, 'API_KEY_MISSING')",
                    null
                )
            }
            return
        }

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        Thread {
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val systemPrompt = "أنت مؤرخ خبير ومحرر أدبي رفيع لموسوعة 'آل بن درويش' التاريخية التراثية. مهمتك هي استلام المعلومات التاريخية أو السير أو الحكايات المدخلة، وإعادة صياغتها وتنظيمها وصقلها بلغة عربية فصحى وقورة وممتازة تليق بكتب الأنساب والتاريخ والتراث، ثم تقسيمها إلى صفحات متناسقة ومناسبة تماماً بحسب كمية وحجم المعلومات. يجب أن ترجع النتيجة كـ JSON Array من الصفحات، كل صفحة بها الحقول التالية بالدقة الحرفية:\n1. 'title': عنوان جزيل ومناسب للصفحة (مثال: 'مآثر آل بن درويش في الكرم' أو 'سيرة الشيخ عاطف بن طالب').\n2. 'content': النص السردي المكتوب بعناية فائقة مقسماً ومؤطراً داخل وسوم فقرات HTML قياسية <p>...</p> فقط لكي يظهر متباعداً وجميلاً بداخل الموسوعة.\nلا تضف أي نصوص أو شروح خارج مصفوفة الـ JSON."

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", rawText)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemPrompt)
                            })
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val responseJson = JSONObject(responseBody)
                        val candidates = responseJson.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val firstPart = parts?.optJSONObject(0)
                        val textResult = firstPart?.optString("text")

                        if (!textResult.isNullOrEmpty()) {
                            // سنقوم بترميز النص بشكل آمن لتجنب مشكلات الهروب في السلاسل النصية للجافا سكريبت
                            val escapedText = textResult
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\"", "\\\"")
                                .replace("\r", "\\r")
                                .replace("\n", "\\n")

                            mainHandler.post {
                                webView.evaluateJavascript(
                                    "javascript:onAiPagesGenerated(true, \"$escapedText\")",
                                    null
                                )
                            }
                        } else {
                            mainHandler.post {
                                webView.evaluateJavascript(
                                    "javascript:onAiPagesGenerated(false, 'المحتوى المرتجع فارغ من الذكاء الاصطناعي')",
                                    null
                                )
                            }
                        }
                    } else {
                        mainHandler.post {
                            webView.evaluateJavascript(
                                "javascript:onAiPagesGenerated(false, 'استجابة فارغة من خادم الذكاء الاصطناعي')",
                                null
                            )
                        }
                    }
                } else {
                    val errMsg = "خطأ من ملقم الذكاء الاصطناعي رمز: ${response.code}"
                    mainHandler.post {
                        webView.evaluateJavascript(
                            "javascript:onAiPagesGenerated(false, '$errMsg')",
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: "حدث خطأ غير متوقع أثناء الاتصال بالذكاء الاصطناعي"
                val cleanErrorMsg = errorMsg.replace("'", "\\'").replace("\"", "\\\"")
                mainHandler.post {
                    webView.evaluateJavascript(
                        "javascript:onAiPagesGenerated(false, '$cleanErrorMsg')",
                        null
                    )
                }
            }
        }.start()
    }

    @JavascriptInterface
    fun organizeBookWithAi(pagesJson: String) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                webView.evaluateJavascript(
                    "javascript:onBookOrganized(false, 'API_KEY_MISSING')",
                    null
                )
            }
            return
        }

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        Thread {
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val systemPrompt = """
                    أنت خبير فني ومؤرخ في تنظيم وتبويب المخطوطات والكتب التاريخية الكبرى لقبيلة آل بن درويش العمري.
                    مهمتك السامية هي استلام مصفوفة صفحات الكتاب المدخلة بصيغة JSON، قراءة عنوان ومحتوى كل صفحة بدقة، ثم القيام بما يلي:
                    1. تصنيف كل صفحة تلقائياً في المكان الأنسب لها بوضع قيمة مفتاح 'chapter' لتكون واحدة من القيم التالية حصراً:
                       - 'intro' (تمهيد ومقدمات ومراجعات الكتاب العامة)
                       - 'chapter1' (الباب الأول: الجذور والرجال - مخصص لتراجم وسير فرسان ورموز آل بن درويش)
                       - 'genealogy' (شجرة النسب والفرع - لتوثيقات النسب وسلاسل الأجداد والفروع)
                       - 'chapter2' (الباب الثاني: العرف والنظام القبلي - للعهود، المواثيق، الدساتير والأنظمة الداخلية)
                       - 'chapter3' (الباب الثالث: الأرض والديار - للجغرافيا، مواطن الأجداد بيافع، الحصون والأراضي التاريخية)
                       - 'chapter4' (الباب الرابع: ملامح من زمان الأجداد - للحكايات القديمة، المعارك، البطولات والمآثر التاريخية المروية)
                    2. إعادة ترتيب الصفحات بشكل منطقي وسلس ومتسلسل يضمن انتقال القارئ بينها بانسجام.
                    3. صقل العناوين والمحتويات بلطف وإتقان لإعطائها طابعاً جليلاً ووقوراً فصيحاً دون تدمير التفاصيل الأصلية.
                    4. يجب إرجاع النتيجة كمصفوفة JSON صالحة بالكامل ومطابقة للمصفوفة المدخلة، مع تزويد كل صفحة بحقل 'chapter' المحدث وجعلها مرتبة ترتيباً صحيحاً.
                    لا تضف أي شرح أو تنسيق خارج مصفوفة الـ JSON على الإطلاق.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", pagesJson)
                                })
                            })
                        })
                    })
                }
                requestJson.put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
                requestJson.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val responseJson = JSONObject(responseBody)
                        val candidates = responseJson.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val firstPart = parts?.optJSONObject(0)
                        val textResult = firstPart?.optString("text")

                        if (!textResult.isNullOrEmpty()) {
                            val escapedText = textResult
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\"", "\\\"")
                                .replace("\r", "\\r")
                                .replace("\n", "\\n")

                            mainHandler.post {
                                webView.evaluateJavascript(
                                    "javascript:onBookOrganized(true, \"$escapedText\")",
                                    null
                                )
                            }
                        } else {
                            mainHandler.post {
                                webView.evaluateJavascript(
                                    "javascript:onBookOrganized(false, 'المحتوى المرتجع فارغ من الذكاء الاصطناعي')",
                                    null
                                )
                            }
                        }
                    } else {
                        mainHandler.post {
                            webView.evaluateJavascript(
                                "javascript:onBookOrganized(false, 'استجابة فارغة من خادم الذكاء الاصطناعي')",
                                null
                            )
                        }
                    }
                } else {
                    val errMsg = "خطأ من ملقم الذكاء الاصطناعي رمز: ${response.code}"
                    mainHandler.post {
                        webView.evaluateJavascript(
                            "javascript:onBookOrganized(false, '$errMsg')",
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: "حدث خطأ غير متوقع أثناء ترتيب الموسوعة بالذكاء الاصطناعي"
                val cleanErrorMsg = errorMsg.replace("'", "\\'").replace("\"", "\\\"")
                mainHandler.post {
                    webView.evaluateJavascript(
                        "javascript:onBookOrganized(false, '$cleanErrorMsg')",
                        null
                    )
                }
            }
        }.start()
    }
}

@Composable
fun BookWebViewContainer(
    onOpenFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams?) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        if (filePathCallback != null) {
                            onOpenFileChooser(filePathCallback, fileChooserParams)
                            return true
                        }
                        return false
                    }
                }
                
                addJavascriptInterface(WebDbInterface(context, this), "AndroidDb")
                loadUrl("file:///android_asset/index.html")
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
