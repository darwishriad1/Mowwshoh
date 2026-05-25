package com.example.bendervish

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// --- Models ---
data class Personality(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val role: String,
    val era: String
)

data class TribalEvent(
    val id: String,
    val title: String,
    val organizer: String,
    val date: String,
    val location: String,
    val type: String, // "gathering" | "celebration" | "wedding"
    var attendees: Int = 12
)

data class Message(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// --- Color Palette (Arabic Deep Heritage Concept: Dark Obsidian + Imperial Gold) ---
val PrimaryGold = Color(0xFFD4AF37) // Majestic traditional gold
val SecondaryGold = Color(0xFFC5A059) // Burnished amber
val ObsidianBg = Color(0xFF121212) // Pure dark basalt
val DesertCharcoal = Color(0xFF1A1A1A) // Muted container black
val GoldAccent = Color(0xFFFFDF7E) // Bright golden reflection
val TextWhite = Color(0xFFEFEFEF)
val TextMuted = Color(0xFFB0B0B0)

// --- ViewModel ---
class MainViewModel : ViewModel() {
    private val _personalities = MutableStateFlow<List<Personality>>(emptyList())
    val personalities: StateFlow<List<Personality>> = _personalities.asStateFlow()

    private val _events = MutableStateFlow<List<TribalEvent>>(emptyList())
    val events: StateFlow<List<TribalEvent>> = _events.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        loadData()
    }

    private fun loadData() {
        _personalities.value = listOf(
            Personality(
                "p1",
                "الشيخ عابد بن درويش الأحمدي",
                "كبير وعمدة قبيلة بن درويش (تاريخياً)",
                "عاش في القرن الماضي وكان رمزاً للحكمة والصلح بين القبائل، ومأوى لتقديم الضيافة وإصلاح ذات البين في منطقة الحجاز والمدينة المنورة.",
                "رائد الإصلاح والقضاء العشائري",
                "العهد القديم"
            ),
            Personality(
                "p2",
                "الأستاذ الدكتور سليم بن درويش",
                "مؤرخ وباحث في علم الأنساب والآثار",
                "صاحب كتاب 'تاريخ وعشائر الحجاز الأحمدية العريقة'، ساهم بأكثر من ثلاثين دراسة ميدانية لتوثيق السيرة التاريخية والآثار المتبقية للأجداد.",
                "شخصية أكاديمية وتربوية بارزة",
                "العهد الحديث"
            ),
            Personality(
                "p3",
                "الشاعر مساعد بن درويش الأحمدي",
                "شاعر القبيلة وفارس الكلمة",
                "صاحب القصائد الحماسية والوجدانية الخالدة التي تصف شرف الانتماء للقبيلة وفخر ديار الحجاز، يُلقي أشعاره في المحافل العامة والملتقيات الوطنية.",
                "الأدب والشعر الشعبي",
                "العهد الحديث"
            ),
            Personality(
                "p4",
                "المهندس فهد بن درويش العمري",
                "منسق ملتقى الأجيال وفاعل مجتمعي",
                "مبتكر وصاحب الرؤية التقنية التي تدعم التحول المعرفي والربط بين بطون وفروع العشيرة، يدير برامج الدعم الأكاديمي والمهني لشباب وشابات العشيرة.",
                "الابتكار وخدمة المجتمع",
                "العهد الحديث"
            )
        )

        _events.value = listOf(
            TribalEvent(
                "e1",
                "الملتقى السنوي الكبير لقبيلة بن درويش",
                "لجنة التواصل الاجتماعي بالعشيرة",
                "2026/06/15",
                "المدينة المنورة - قاعة الملوك",
                "gathering",
                142
            ),
            TribalEvent(
                "e2",
                "حفل تكريم المتفوقين وخريجي الجامعات",
                "صندوق العشيرة الثقافي",
                "2026/06/28",
                "ينبع البحر - منتجع آراك",
                "celebration",
                85
            ),
            TribalEvent(
                "e3",
                "زواج الشاب فيصل بن مساعد بن درويش",
                "عائلة الشاعر مساعد بن درويش",
                "2026/07/04",
                "جدة - قاعة ليلتي",
                "wedding",
                250
            )
        )

        _chatMessages.value = listOf(
            Message("welcome", "مرحباً بك يا سليل الأكرمين في مجلس قبيلة بن درويش الذكي. أنا مستشارك الرقمي، سلني عن تاريخ القبيلة، أنسابها، عاداتها وتقاليدها الأصيلة أو شعر الكرم والبطولات.", false)
        )
    }

    fun addEvent(title: String, organizer: String, date: String, location: String, type: String) {
        val newId = "e_${System.currentTimeMillis()}"
        val newEvent = TribalEvent(newId, title, organizer, date, location, type, 12)
        _events.value = listOf(newEvent) + _events.value
    }

    fun registerAttendance(eventId: String) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(attendees = it.attendees + 1) else it
        }
    }

    fun sendMessageToAI(userText: String) {
        if (userText.isBlank()) return

        val userMsgId = "msg_${System.currentTimeMillis()}"
        val userMsg = Message(userMsgId, userText, true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        val apiKey = BuildConfig.GEMINI_API_KEY

        val systemInstructionText = """
            أنت شيخ فاضل ومؤرخ حكيم تُمثل مجلس قبيلة بن درويش (الأحمدي العمري الحجازي). 
            تتحدث بلهجة عربية بليغة ملؤها الترحاب الدافئ والكرم والاعتزاز بالتراث والعروبة. 
            تُجيب بدقة وبصورة إيجابية فخورة عن أسئلة المستخدمين بخصوص تاريخ عائلة وقبيلة بن درويش، نسبهم إلى الأحمدي والعمري، فروعهم في ديار الحجاز والمدينة المنورة، شعرهم وعادات الضيافة والنخوة وإصلاح ذات البين.
            تذكر دائماً شعار العشيرة: (عهد الأجداد ووعد الأحفاد).
            إذا سألك أحد عن كرم القبيلة أو قصيدة عنها، ألقِ أبياتاً بليغة ملؤها الفخر. 
            إذا كان السؤال عاماً، وجهه بلطف ليتعرف على التراث والنسب من خلال تبويبات التطبيق.
        """.trimIndent()

        val lastMessages = _chatMessages.value.takeLast(6)

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                if (apiKey.isEmpty() || apiKey.contains("KEY_HERE") || apiKey.contains("DEFAULT_VALUE")) {
                    withContext(Dispatchers.Main) {
                        val errorId = "err_${System.currentTimeMillis()}"
                        _chatMessages.value = _chatMessages.value + Message(
                            errorId,
                            "تنبيه: مفتاح الذكاء الاصطناعي (API Key) لم يتم تهيئته بشكل كامل بعد في إعدادات التطبيق. يرجى توفيره عبر 'Secrets panel' لتتمكن من التحدث مع المجلس الذكي.",
                            false
                        )
                        _isChatLoading.value = false
                    }
                    return@launch
                }

                // Construct direct JSON for Gemini generateContent content parts manually
                val contentsArray = JSONArray()
                lastMessages.forEach { msg ->
                    val contentObj = JSONObject()
                    contentObj.put("role", if (msg.isUser) "user" else "model")
                    
                    val partsArray = JSONArray()
                    val partObj = JSONObject()
                    partObj.put("text", msg.content)
                    partsArray.put(partObj)
                    
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                }

                val rootJson = JSONObject()
                rootJson.put("contents", contentsArray)

                val systemInstructionObj = JSONObject()
                val insPartsArray = JSONArray()
                val insPartObj = JSONObject()
                insPartObj.put("text", systemInstructionText)
                insPartsArray.put(insPartObj)
                systemInstructionObj.put("parts", insPartsArray)
                rootJson.put("systemInstruction", systemInstructionObj)

                val generationConfig = JSONObject()
                generationConfig.put("temperature", 0.7)
                rootJson.put("generationConfig", generationConfig)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: $responseBodyStr")
                }

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val replyContent = firstCandidate.getJSONObject("content")
                val parts = replyContent.getJSONArray("parts")
                val replyText = parts.getJSONObject(0).getString("text")

                withContext(Dispatchers.Main) {
                    val replyId = "reply_${System.currentTimeMillis()}"
                    _chatMessages.value = _chatMessages.value + Message(replyId, replyText, false)
                    _isChatLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val replyId = "err_${System.currentTimeMillis()}"
                    _chatMessages.value = _chatMessages.value + Message(
                        replyId,
                        "بارك الله فيك، يبدو أن هناك عطلاً مؤقتاً في شبكة الاتصال بالمجلس الذكي. الرجاء المحاولة مرة أخرى لاحقاً. (التفاصيل: ${e.localizedMessage})",
                        false
                    )
                    _isChatLoading.value = false
                }
            }
        }
    }
}

// --- Navigation Routes ---
enum class AppScreen(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "الرئيسية", Icons.Default.Home),
    HERITAGE("heritage", "التراث", Icons.Default.Info),
    PERSONALITIES("personalities", "الشخصيات", Icons.Default.Person),
    EVENTS("events", "المناسبات", Icons.Default.Notifications),
    COUNCIL("council", "المجلس", Icons.Default.Send)
}

// --- Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = PrimaryGold,
                    secondary = SecondaryGold,
                    background = ObsidianBg,
                    surface = DesertCharcoal,
                    onPrimary = Color.Black,
                    onBackground = TextWhite,
                    onSurface = TextWhite
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainLayout()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "شعار الموسوعة",
                            tint = PrimaryGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "موسوعة قبيلة بن درويش",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PrimaryGold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DesertCharcoal
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DesertCharcoal,
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                AppScreen.values().forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (selected) PrimaryGold else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) PrimaryGold else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF2C2514)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreen.HOME.route) {
                HomeScreen(viewModel, onNavigateToAbout = { navController.navigate(AppScreen.HERITAGE.route) })
            }
            composable(AppScreen.HERITAGE.route) {
                HeritageScreen()
            }
            composable(AppScreen.PERSONALITIES.route) {
                PersonalitiesScreen(viewModel)
            }
            composable(AppScreen.EVENTS.route) {
                EventsScreen(viewModel)
            }
            composable(AppScreen.COUNCIL.route) {
                CouncilScreen(viewModel)
            }
        }
    }
}

// --- SCREEN 1: HOME ---
@Composable
fun HomeScreen(viewModel: MainViewModel, onNavigateToAbout: () -> Unit) {
    val eventList by viewModel.events.collectAsState()
    val peopleList by viewModel.personalities.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2E2405), Color(0xFF141414))
                    )
                )
                .border(1.dp, PrimaryGold, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .testTag("home_hero_card"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "« عهد الأجداد ووعد الأحفاد »",
                    fontSize = 14.sp,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "موسوعة قبيلة بن درويش",
                    fontSize = 22.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "العشيرة الأحمدية العمرية الخالدة",
                    fontSize = 13.sp,
                    color = SecondaryGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = PrimaryGold.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "مرحباً بكم في الصرح الرقمي الموثق لقبيلة بن درويش الأحمدي العمري، ديارهم وأمجادهم وقيمهم الأصيلة الممتدة من عمق الحجاز الأصيل.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DesertCharcoal),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, SecondaryGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "شعر",
                        tint = PrimaryGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "من أدبيات القبيلة",
                        color = PrimaryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "بني درويش كالأهرام فخراً ... تفيض ملاحم الكرم الصريح\nلهم في الحادثات عريض جاهٍ ... ونبع العهد في الصدر الفسيح",
                    color = TextWhite,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesertCharcoal)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${peopleList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    Text(text = "رواة ومؤرخين", fontSize = 12.sp, color = TextMuted)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesertCharcoal)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${eventList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    Text(text = "فعاليات نشطة", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNavigateToAbout,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("explore_history_button")
        ) {
            Text(text = "تصفّح شجرة النسب وعمق التاريخ العربي", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// --- SCREEN 2: HERITAGE ---
@Composable
fun HeritageScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "النسب والجذور التاريخية",
            fontSize = 18.sp,
            color = PrimaryGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "شجرة قبيلة بن درويش الموثقة في فروع الأحمدي العمري من بني مسروح الحربية العريقة في ربوع الحجاز والمدينة المنورة.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DesertCharcoal)
                .border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "أصول النسب العشائري:",
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                GenealogyNode("الجد الأكبر: مسروح الحربي", "مؤسس حلف القبائل الأكبر")
                GenealogyConnector()
                GenealogyNode("الفرع الأساسي: بني عمرو", "قبائل الحجاز الأصيلة")
                GenealogyConnector()
                GenealogyNode("العشيرة: بني أحمد (الأحمدي)", "أهل ينبع النخل ووادي الصفراء")
                GenealogyConnector()
                GenealogyNode("النسب الموثق: بن درويش", "سلالة النبلاء وفرسان الحكمة والصلح")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ديار ومواطن قبيلة بن درويش",
            fontSize = 16.sp,
            color = PrimaryGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DesertCharcoal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "• وادي الصفراء:",
                    color = SecondaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "العمق التاريخي المستقر والواحات الزراعية المروية ببركة الأجداد.",
                    color = TextWhite,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 12.dp)
                )

                Text(
                    text = "• المدينة المنورة وبطحاء الحجاز:",
                    color = SecondaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "العاصمة الإيمانية والربيع الحجازي الذي استقر به الأحفاد لطلب العلم ومجالس الأدب والخدمة العامة.",
                    color = TextWhite,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun GenealogyNode(title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(PrimaryGold)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun GenealogyConnector() {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .width(2.dp)
            .height(20.dp)
            .background(SecondaryGold.copy(alpha = 0.5f))
    )
}

// --- SCREEN 3: PERSONALITIES ---
@Composable
fun PersonalitiesScreen(viewModel: MainViewModel) {
    val personalities by viewModel.personalities.collectAsState()
    var selectedPerson by remember { mutableStateOf<Personality?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "شخصيات بارزة وأعلام القبيلة",
            fontSize = 18.sp,
            color = PrimaryGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "رجال في الذاكرة كتبوا بمداد الفخر والوفاء سيرة قبيلتهم بن درويش الأحمدي المعماري.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(personalities) { person ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DesertCharcoal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPerson = person }
                        .border(
                            1.dp,
                            if (selectedPerson?.id == person.id) PrimaryGold else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("person_item_${person.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = PrimaryGold.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = person.name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                            Text(text = person.title, color = SecondaryGold, fontSize = 12.sp)
                            Text(text = person.role, color = TextMuted, fontSize = 11.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "التفاصيل",
                            tint = SecondaryGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        selectedPerson?.let { person ->
            Dialog(onDismissRequest = { selectedPerson = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryGold, RoundedCornerShape(16.dp))
                        .testTag("person_details_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DesertCharcoal)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = PrimaryGold
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "",
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = person.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = person.title,
                            fontSize = 12.sp,
                            color = SecondaryGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = person.era,
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .border(0.5.dp, TextMuted, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = person.description,
                            color = TextWhite,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { selectedPerson = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "إغلاق التفاصيل", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 4: EVENTS ---
@Composable
fun EventsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var eventTitle by remember { mutableStateOf("") }
    var eventOrganizer by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("gathering") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الأخبار والمناسبات للقبيلة",
                    fontSize = 18.sp,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "منبر التواصل والرحم لأبناء العشيرة",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_event_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "إعلان مناسبة", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(events) { tribalEvent ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DesertCharcoal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            0.5.dp,
                            when (tribalEvent.type) {
                                "wedding" -> Color(0xFFFF8F00).copy(alpha = 0.5f)
                                "celebration" -> PrimaryGold.copy(alpha = 0.5f)
                                else -> SecondaryGold.copy(alpha = 0.5f)
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tribalEvent.title,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (tribalEvent.type) {
                                        "wedding" -> "حفل زواج"
                                        "celebration" -> "تكريم ونجاح"
                                        else -> "ملتقى عام"
                                    },
                                    color = PrimaryGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "المعلن: ${tribalEvent.organizer}", color = TextMuted, fontSize = 12.sp)
                        Text(text = "التاريخ: ${tribalEvent.date}", color = TextMuted, fontSize = 12.sp)
                        Text(text = "الموقع: ${tribalEvent.location}", color = TextWhite, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = TextMuted.copy(alpha = 0.15f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الحضور: ${tribalEvent.attendees} سليل",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = {
                                    viewModel.registerAttendance(tribalEvent.id)
                                    Toast.makeText(context, "تم تسجيل رغبة حضورك بنجاح! ننتظر لقاك بفخر.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier
                                    .border(0.5.dp, PrimaryGold, RoundedCornerShape(6.dp))
                                    .height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(text = "تسجيل حضور", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryGold, RoundedCornerShape(16.dp))
                        .testTag("add_event_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DesertCharcoal)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "إرسال إعلان مناسبة للعموم", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = { Text("عنوان المناسبة (مثال: زواج فلان)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_event_title"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, cursorColor = PrimaryGold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = eventOrganizer,
                            onValueChange = { eventOrganizer = it },
                            label = { Text("اسم المعلن / العائلة الكريمة") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_event_organizer"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, cursorColor = PrimaryGold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = { eventDate = it },
                            label = { Text("التاريخ (مثال: 1447/12/15 هـ)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_event_date"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, cursorColor = PrimaryGold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = eventLocation,
                            onValueChange = { eventLocation = it },
                            label = { Text("الموقع والقاعة (المدينة والحي)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_event_location"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, cursorColor = PrimaryGold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("gathering" to "ملتقى", "wedding" to "زواج", "celebration" to "تكريم").forEach { pair ->
                                val isSelected = eventType == pair.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) PrimaryGold else Color(0xFF2E2E2E))
                                        .clickable { eventType = pair.first }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.second,
                                        color = if (isSelected) Color.Black else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "إلغاء")
                            }
                            Button(
                                onClick = {
                                    if (eventTitle.isNotBlank() && eventOrganizer.isNotBlank()) {
                                        viewModel.addEvent(
                                            title = eventTitle,
                                            organizer = eventOrganizer,
                                            date = if (eventDate.isBlank()) "مستقبلاً" else eventDate,
                                            location = if (eventLocation.isBlank()) "ديار الحجاز" else eventLocation,
                                            type = eventType
                                        )
                                        Toast.makeText(context, "تم إرسال إعلانك ليظهر في منبر العشيرة!", Toast.LENGTH_SHORT).show()
                                        showAddDialog = false
                                        // Reset fields
                                        eventTitle = ""
                                        eventOrganizer = ""
                                        eventDate = ""
                                        eventLocation = ""
                                    } else {
                                        Toast.makeText(context, "يرجى تعبئة الحقول الأساسية أولاً", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "نشر الإعلان", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 5: COUNCIL ---
@Composable
fun CouncilScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF241D08))
                .padding(12.dp)
                .testTag("council_banner")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(6.dp),
                    shape = CircleShape,
                    color = PrimaryGold
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "المجلس التقليدي الرقمي المعتمد على الذكاء الاصطناعي",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (message.isUser) 12.dp else 0.dp,
                                    bottomEnd = if (message.isUser) 0.dp else 12.dp
                                )
                            )
                            .background(if (message.isUser) PrimaryGold else DesertCharcoal)
                            .border(
                                0.5.dp,
                                if (message.isUser) Color.Transparent else SecondaryGold.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Column {
                            Text(
                                text = if (message.isUser) "أنت" else "المجلس الرقمي الحكيم",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.isUser) Color.Black.copy(alpha = 0.6f) else SecondaryGold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = message.content,
                                fontSize = 13.sp,
                                color = if (message.isUser) Color.Black else TextWhite,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DesertCharcoal)
                                .padding(12.dp)
                        ) {
                            Text(text = "الشيخ يكتب رده بحكمة ووقار...", fontSize = 12.sp, color = SecondaryGold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("سل حكيم المجلس عن عادات وديار القبيلة...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    cursorColor = PrimaryGold,
                    unfocusedBorderColor = SecondaryGold.copy(alpha = 0.5f)
                ),
                maxLines = 2
            )
            FloatingActionButton(
                onClick = {
                    if (inputQuery.isNotBlank() && !isLoading) {
                        viewModel.sendMessageToAI(inputQuery)
                        inputQuery = ""
                    }
                },
                containerColor = PrimaryGold,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("send_query_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "بحث المجلس وإرسال",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
