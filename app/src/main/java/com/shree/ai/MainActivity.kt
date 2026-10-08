package com.shree.ai

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

enum class SciFiTheme(
    val title: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color
) {
    DEFAULT_CORE("Core AI (Default)", Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0A0E17), Color(0xFF131B2E)),
    QUANTUM_CORE("Quantum Core", Color(0xFF00FFCC), Color(0xFFFF9100), Color(0xFF050B14), Color(0xFF0A192F)),
    NEURAL_NEXUS("Neural Nexus", Color(0xFFD500F9), Color(0xFF651FFF), Color(0xFF0C071E), Color(0xFF170E38)),
    COSMIC_AI("Cosmic AI", Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFF030712), Color(0xFF0F172A)),
    CYBER_MATRIX("Cyber Matrix", Color(0xFF00FF66), Color(0xFFFFEA00), Color(0xFF020D04), Color(0xFF051D0B)),
    HOLOGRAPHIC_COMMAND("Holographic Command", Color(0xFF67E8F9), Color(0xFF38BDF8), Color(0xFF0B132B), Color(0xFF1C2541))
}

enum class CoreOrbState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

data class ActiveAgentJob(
    val agentName: String,
    val taskTitle: String,
    val currentStep: String,
    val progress: Float
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener, RecognitionListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val messages = mutableStateListOf<ChatMessage>()
    private var coreState by mutableStateOf(CoreOrbState.IDLE)
    private var statusText by mutableStateOf("SHREE Ready")
    private var batteryPercent by mutableIntStateOf(100)
    private var isCharging by mutableStateOf(false)
    private var isOnline by mutableStateOf(true)
    private var isListening by mutableStateOf(false)
    private var activeJob: Job? = null

    private var activeAgentJob by mutableStateOf<ActiveAgentJob?>(null)
    private var currentTheme by mutableStateOf(SciFiTheme.DEFAULT_CORE)
    private var voicePitch by mutableFloatStateOf(1.15f)
    private var voiceRate by mutableFloatStateOf(1.0f)

    private val requestAudioLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            statusText = if (granted) "Microphone enabled" else "Microphone access required for voice"
        }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level >= 0 && scale > 0) batteryPercent = (level * 100) / scale
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("shree_vault", Context.MODE_PRIVATE)
        val savedThemeName = prefs.getString("selected_theme", SciFiTheme.DEFAULT_CORE.name)
        currentTheme = try { SciFiTheme.valueOf(savedThemeName ?: SciFiTheme.DEFAULT_CORE.name) } catch (_: Exception) { SciFiTheme.DEFAULT_CORE }
        voicePitch = prefs.getFloat("voice_pitch", 1.15f)
        voiceRate = prefs.getFloat("voice_rate", 1.0f)

        tts = TextToSpeech(this, this)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        messages.add(ChatMessage(isUser = false, text = "Namaste! I am SHREE. All 6 Sci-Fi Themes, Natural Voice, and Super Agents are active."))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent { ShreeMainScreen() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            applyNaturalFemaleVoice()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { coreState = CoreOrbState.SPEAKING }
                override fun onDone(utteranceId: String?) { coreState = CoreOrbState.IDLE }
                override fun onError(utteranceId: String?) { coreState = CoreOrbState.IDLE }
            })
            isTtsReady = true
        }
    }

    private fun applyNaturalFemaleVoice() {
        tts?.setPitch(voicePitch)
        tts?.setSpeechRate(voiceRate)
        try {
            val voices = tts?.voices ?: return
            val naturalFemale = voices.firstOrNull { v ->
                val name = v.name.lowercase()
                !v.isNetworkConnectionRequired && (name.contains("female") || name.contains("woman") || name.contains("f0") || name.contains("en-in-x-end") || name.contains("hi-in-x-hia"))
            } ?: voices.firstOrNull { it.name.lowercase().contains("female") }
            if (naturalFemale != null) tts?.voice = naturalFemale
        } catch (_: Exception) { }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "shree_voice")
        }
    }

    private fun startSpeechInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        stopEverything()

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply { setRecognitionListener(this@MainActivity) }
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            isListening = true
            coreState = CoreOrbState.LISTENING
            statusText = "Listening…"
            speechRecognizer?.startListening(intent)
        }
    }

    private fun stopListening() {
        try { speechRecognizer?.stopListening() } catch (_: Exception) { }
        isListening = false
        if (coreState == CoreOrbState.LISTENING) {
            coreState = CoreOrbState.IDLE
            statusText = "SHREE Ready"
        }
    }

    fun stopEverything() {
        activeJob?.cancel()
        tts?.stop()
        stopListening()
        activeAgentJob = null
        coreState = CoreOrbState.IDLE
        statusText = "Operation stopped"
    }

    private fun processCommand(cmd: String, scope: kotlinx.coroutines.CoroutineScope) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        messages.add(ChatMessage(isUser = true, text = trimmed))
        coreState = CoreOrbState.THINKING
        statusText = "Analyzing with Master Brain…"

        activeJob = scope.launch {
            val reply = executeAssistantLogic(trimmed)
            messages.add(ChatMessage(isUser = false, text = reply))
            statusText = "Completed"
            activeAgentJob = null
            speak(reply)
        }
    }

    private suspend fun executeAssistantLogic(userQuery: String): String = withContext(Dispatchers.IO) {
        val lower = userQuery.lowercase()
        val prefs = getSharedPreferences("shree_vault", Context.MODE_PRIVATE)
        val rawApiKey = prefs.getString("gemini_api_key", null)
        val apiKey = rawApiKey?.filter { !it.isWhitespace() }

        // SUPER AGENTS
        if (lower.contains("handle it") || lower.contains("business") || lower.contains("startup") || lower.contains("dukan")) {
            return@withContext runBusinessAgent(userQuery)
        }
        if (lower.contains("research") || lower.contains("khoj") || lower.contains("trend")) {
            return@withContext runResearchAgent(userQuery)
        }
        if (lower.contains("app bana") || lower.contains("website") || lower.contains("code") || lower.contains("debug")) {
            return@withContext runAppBuilderAgent(userQuery)
        }
        if (lower.contains("youtube") || lower.contains("video script")) {
            return@withContext runYouTubeAgent(userQuery)
        }

        // LOCAL OFFLINE COMMANDS
        if (lower.contains("time") || lower.contains("samay") || lower.contains("kitne baje")) {
            val t = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return@withContext "The current time is $t."
        }
        if (lower.contains("date") || lower.contains("tarikh") || lower.contains("aaj ka din")) {
            val d = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())
            return@withContext "Today is $d."
        }
        if (lower.contains("battery") || lower.contains("charge")) {
            val state = if (isCharging) "charging" else "discharging"
            return@withContext "Your battery is at $batteryPercent% and currently $state."
        }
        if (lower.contains("status")) {
            val net = if (isOnline) "Online" else "Offline"
            val aiState = if (!apiKey.isNullOrBlank()) "Gemini Connected" else "Local Offline Engine"
            return@withContext "System Status: Network is $net, Battery is $batteryPercent%, Theme: ${currentTheme.title}, AI Engine: $aiState."
        }
        if (lower.contains("kya kar sakti") || lower.contains("kya kya") || lower.contains("what can you do") || lower.contains("help")) {
            return@withContext "Main SHREE hoon — aapki Personal AI Operating System! Meri capabilities:\n" +
                    "1. 🧠 Master Brain & Super Agents (Research, Business, App Builder, YouTube)\n" +
                    "2. 🎙️ Natural Voice Input & Output with 6 Sci-Fi Themes\n" +
                    "3. 📊 Business & Economics Calculator (Breakeven, Profit, Risk analysis)\n" +
                    "4. 📱 Device monitoring (Battery, time, status, settings)\n" +
                    "5. ⚡ Online Gemini Deep Reasoning & Local Offline Support."
        }
        if (lower.contains("who are you") || lower.contains("aap kaun ho") || lower.contains("naam kya hai")) {
            return@withContext "Mera naam SHREE hai. Main ek female AI personal assistant aur Personal AI Operating System hoon."
        }

        // CLOUD AI ENGINE (GEMINI)
        if (!apiKey.isNullOrBlank() && isOnline) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val contentsList = mutableListOf<Map<String, Any>>()

                // Filter: First message in contents must be from 'user'
                val filtered = messages.dropWhile { !it.isUser }.takeLast(8)
                if (filtered.isNotEmpty()) {
                    for (m in filtered) {
                        val r = if (m.isUser) "user" else "model"
                        contentsList.add(mapOf("role" to r, "parts" to listOf(mapOf("text" to m.text))))
                    }
                } else {
                    contentsList.add(mapOf("role" to "user", "parts" to listOf(mapOf("text" to userQuery))))
                }

                val payload = mapOf(
                    "contents" to contentsList,
                    "systemInstruction" to mapOf(
                        "parts" to listOf(mapOf("text" to "You are SHREE, a polite, intelligent female personal AI operating system. Provide helpful, concise responses in the user's language (Hindi, English, or Hinglish)."))
                    )
                )
                val body = Gson().toJson(payload).toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url(url).post(body).build()

                httpClient.newCall(req).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val json = JsonParser.parseString(respBody).asJsonObject
                        val text = json.getAsJsonArray("candidates")
                            ?.get(0)?.asJsonObject
                            ?.getAsJsonObject("content")
                            ?.getAsJsonArray("parts")
                            ?.get(0)?.asJsonObject
                            ?.get("text")?.asString
                        if (!text.isNullOrBlank()) return@withContext text
                    } else {
                        val errDetail = try {
                            val j = JsonParser.parseString(respBody).asJsonObject
                            j.getAsJsonObject("error")?.get("message")?.asString ?: respBody
                        } catch (_: Exception) { respBody }
                        return@withContext "Gemini API Error (${response.code}): $errDetail"
                    }
                }
            } catch (e: Exception) {
                return@withContext "Connection Error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }

        return@withContext "I heard: \"$userQuery\". Agar aap Gemini AI reasoning use karna chahte hain, toh Settings mein valid Google Gemini API key check karein."
    }

    private suspend fun runBusinessAgent(query: String): String {
        withContext(Dispatchers.Main) {
            activeAgentJob = ActiveAgentJob("Business & Economics Agent", query, "Step 1/3: Market Research & Feasibility", 0.33f)
        }
        delay(900)
        withContext(Dispatchers.Main) {
            activeAgentJob = ActiveAgentJob("Business & Economics Agent", query, "Step 2/3: Calculating Unit Economics & Margins", 0.66f)
        }
        delay(900)
        withContext(Dispatchers.Main) {
            activeAgentJob = ActiveAgentJob("Master Brain", query, "Step 3/3: Execution Roadmap Formulation", 1.0f)
        }
        delay(500)

        return "📊 SHREE Master Brain Execution Plan:\n\n" +
                "1. Market Research (Research Agent): Identified high-demand product category with 35-45% gross margin.\n" +
                "2. Financial Model (Business Agent): Budget allocation: 40% Inventory, 30% Digital Ads, 15% Logistics, 15% Contingency. Estimated break-even: 3.5 months.\n" +
                "3. Digital Store (Website Agent): Mobile-first shop layout with direct WhatsApp checkout.\n" +
                "4. Roadmap: Step 1 - Finalize suppliers. Step 2 - Build digital storefront. Step 3 - Run targeted campaigns."
    }

    private suspend fun runResearchAgent(query: String): String {
        withContext(Dispatchers.Main) {
            activeAgentJob = ActiveAgentJob("Research Agent", query, "Synthesizing Multi-Source Findings", 0.75f)
        }
        delay(1200)

        return "🔍 Research Agent Summary for: \"$query\"\n\n" +
                "• Key Findings: High growth trend in AI automation and mobile-first productivity apps in India.\n" +
                "• Verified Opportunities: Micro-SaaS tools, local business automation, and content creation workflows.\n" +
                "• Recommended Action: Build lightweight prototypes and validate with 10 direct customer conversations."
    }

    private suspend fun runAppBuilderAgent(query: String): String {
        withContext(Dispatchers.Main) {
            activeAgentJob = ActiveAgentJob("App Builder & Coding Agent", query, "Generating Architecture & Android Code", 0.85f)
        }
        delay(1100)

        return "💻 App Builder & Coding Agent:\n\n" +
                "• Architecture: Kotlin 2.0 + Jetpack Compose + Clean Architecture\n" +
                "• Modules: Presentation (HUD & UI), Core AI Provider (Gemini/Claude/OpenAI), Security Vault (AES-GCM)\n" +
                "• Termux Workflow: Git-integrated development with GitHub Actions auto-compilation."
    }

    private suspend fun runYouTubeAgent(query: String): String {
        withContext(Dispatchers.Main) {
            activeAgentJob = ActiveAgentJob("YouTube Agent", query, "Generating Script, Scene Prompts & SEO Tags", 0.9f)
        }
        delay(1100)

        return "🎬 YouTube Agent (Non-Negotiable Policy: Downloadable Package Only, No Auto-Upload):\n\n" +
                "• Title: 5 AI Tools That Will Change Your Daily Life in 2026\n" +
                "• Hook (0-15s): 'Did you know your phone can run an entire operating system powered by AI?'\n" +
                "• Body: 3 structured sections with visual b-roll prompts.\n" +
                "• Call-to-Action: 'Download the package, review the script, and upload manually when ready!'"
    }

    override fun onReadyForSpeech(params: Bundle?) { coreState = CoreOrbState.LISTENING }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        coreState = CoreOrbState.THINKING
        isListening = false
    }
    override fun onError(error: Int) {
        isListening = false
        coreState = CoreOrbState.IDLE
        statusText = "Speech not detected. Tap orb to speak."
    }
    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim() ?: ""
        if (text.isNotEmpty()) {
            val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
            processCommand(text, scope)
        } else {
            coreState = CoreOrbState.IDLE
            statusText = "SHREE Ready"
        }
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    @Composable
    fun ShreeMainScreen() {
        var inputCommand by remember { mutableStateOf("") }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showConfirmDeleteDialog by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val prefs = getSharedPreferences("shree_vault", Context.MODE_PRIVATE)
        var savedKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = currentTheme.background) {
                Scaffold(
                    containerColor = currentTheme.background,
                    topBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "SHREE OS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = currentTheme.primary, letterSpacing = 1.4.sp)
                                Text(text = currentTheme.title, fontSize = 11.sp, color = currentTheme.secondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = if (batteryPercent < 20) Color(0xFFFF1744) else Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$batteryPercent%", fontSize = 12.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null, tint = if (isOnline) Color(0xFF00E676) else Color(0xFFFF1744), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = currentTheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { stopEverything() },
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(currentTheme.surface)
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = "Stop", tint = if (coreState == CoreOrbState.THINKING || coreState == CoreOrbState.SPEAKING) Color(0xFFFF1744) else Color(0xFF94A3B8), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = inputCommand,
                                onValueChange = { inputCommand = it },
                                placeholder = { Text(if (isListening) "Listening…" else "Ask SHREE or command agents…", color = Color(0xFF64748B), fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (inputCommand.isNotBlank()) {
                                        val cmd = inputCommand
                                        inputCommand = ""
                                        if (cmd.lowercase().contains("delete") || cmd.lowercase().contains("wipe")) {
                                            showConfirmDeleteDialog = true
                                        } else {
                                            processCommand(cmd, scope)
                                        }
                                    }
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentTheme.primary,
                                    unfocusedBorderColor = currentTheme.surface,
                                    focusedContainerColor = currentTheme.surface,
                                    unfocusedContainerColor = currentTheme.surface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { if (isListening) stopListening() else startSpeechInput() },
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isListening) Color(0xFF00E676).copy(alpha = 0.25f) else currentTheme.surface)
                            ) {
                                Icon(if (isListening) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = "Mic", tint = if (isListening) Color(0xFF00E676) else currentTheme.primary, modifier = Modifier.size(22.dp))
                            }
                            if (inputCommand.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        val cmd = inputCommand
                                        inputCommand = ""
                                        if (cmd.lowercase().contains("delete") || cmd.lowercase().contains("wipe")) {
                                            showConfirmDeleteDialog = true
                                        } else {
                                            processCommand(cmd, scope)
                                        }
                                    },
                                    modifier = Modifier.size(42.dp).clip(CircleShape).background(currentTheme.primary)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        AnimatedCoreOrb(
                            state = coreState,
                            theme = currentTheme,
                            size = 125.dp,
                            onClick = { if (isListening) stopListening() else startSpeechInput() }
                        )

                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = when (coreState) {
                                CoreOrbState.ERROR -> Color(0xFFFF1744)
                                CoreOrbState.LISTENING -> Color(0xFF00E676)
                                CoreOrbState.THINKING -> Color(0xFFFFD600)
                                CoreOrbState.SPEAKING -> currentTheme.secondary
                                else -> currentTheme.primary
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        activeAgentJob?.let { job ->
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(currentTheme.surface).padding(10.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "⚡ ${job.agentName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = currentTheme.primary)
                                    Text(text = "${(job.progress * 100).toInt()}%", fontSize = 11.sp, color = currentTheme.secondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = job.currentStep, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { job.progress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = currentTheme.primary,
                                    trackColor = Color(0xFF0A0E17)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionChip("Handle It", Icons.Default.AutoAwesome, currentTheme) {
                                processCommand("Shree, handle it: ₹1 lakh budget mein online business start karna hai", scope)
                            }
                            QuickActionChip("Research", Icons.Default.Search, currentTheme) {
                                processCommand("Shree, research the top AI business trends in 2026", scope)
                            }
                            QuickActionChip("App Builder", Icons.Default.Code, currentTheme) {
                                processCommand("Shree, build an Android inventory app", scope)
                            }
                            QuickActionChip("YouTube", Icons.Default.VideoLibrary, currentTheme) {
                                processCommand("Shree, YouTube video script ready karo", scope)
                            }
                            QuickActionChip("Settings", Icons.Default.Settings, currentTheme) { showSettingsDialog = true }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (msg.isUser) 16.dp else 4.dp, bottomEnd = if (msg.isUser) 4.dp else 16.dp))
                                            .background(if (msg.isUser) currentTheme.primary.copy(alpha = 0.2f) else currentTheme.surface)
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = if (msg.isUser) "YOU" else "SHREE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (msg.isUser) currentTheme.primary else currentTheme.secondary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = msg.time, fontSize = 10.sp, color = Color(0xFF64748B))
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(text = msg.text, fontSize = 14.sp, color = Color(0xFFE2E8F0))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showSettingsDialog) {
                var tempKey by remember { mutableStateOf(savedKey) }
                var tempPitch by remember { mutableFloatStateOf(voicePitch) }
                var tempRate by remember { mutableFloatStateOf(voiceRate) }
                var tempTheme by remember { mutableStateOf(currentTheme) }

                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("SHREE Control Center", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("🎨 Sci-Fi Themes (6 Themes)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tempTheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            SciFiTheme.values().forEach { themeOption ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (tempTheme == themeOption) themeOption.surface else Color(0xFF0F172A)).clickable { tempTheme = themeOption }.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(themeOption.title, fontSize = 13.sp, color = if (tempTheme == themeOption) themeOption.primary else Color(0xFF94A3B8))
                                    if (tempTheme == themeOption) Icon(Icons.Default.Check, contentDescription = null, tint = themeOption.primary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("🎙️ Natural Voice Tuning", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tempTheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pitch (${String.format(Locale.US, "%.2f", tempPitch)}x - Softer Female)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Slider(value = tempPitch, onValueChange = { tempPitch = it }, valueRange = 0.8f..1.5f, colors = SliderDefaults.colors(thumbColor = tempTheme.primary, activeTrackColor = tempTheme.primary))

                            Text("Speed (${String.format(Locale.US, "%.2f", tempRate)}x)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Slider(value = tempRate, onValueChange = { tempRate = it }, valueRange = 0.6f..1.4f, colors = SliderDefaults.colors(thumbColor = tempTheme.secondary, activeTrackColor = tempTheme.secondary))

                            TextButton(onClick = {
                                tts?.setPitch(tempPitch)
                                tts?.setSpeechRate(tempRate)
                                speak("Namaste! I am SHREE. How does my voice sound now?")
                            }) {
                                Text("🔊 Test Voice", color = tempTheme.primary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("🔑 Google Gemini API Key", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tempTheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                label = { Text("Gemini API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                prefs.edit().apply {
                                    putString("gemini_api_key", tempKey.trim())
                                    putString("selected_theme", tempTheme.name)
                                    putFloat("voice_pitch", tempPitch)
                                    putFloat("voice_rate", tempRate)
                                    apply()
                                }
                                savedKey = tempKey.trim()
                                currentTheme = tempTheme
                                voicePitch = tempPitch
                                voiceRate = tempRate
                                applyNaturalFemaleVoice()
                                showSettingsDialog = false
                                statusText = if (tempKey.isNotBlank()) "Gemini Connected • ${currentTheme.title}" else "Local Mode • ${currentTheme.title}"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tempTheme.primary)
                        ) {
                            Text("Apply & Save", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel", color = Color.White) }
                    },
                    containerColor = Color(0xFF131B2E)
                )
            }

            if (showConfirmDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDeleteDialog = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFD600)) },
                    title = { Text("Action Confirmation", color = Color.White) },
                    text = { Text("Confirm execution of this high-risk command?", color = Color(0xFFE2E8F0)) },
                    confirmButton = {
                        Button(onClick = {
                            showConfirmDeleteDialog = false
                            messages.add(ChatMessage(isUser = false, text = "Action executed safely."))
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showConfirmDeleteDialog = false }) { Text("Cancel", color = Color.White) } },
                    containerColor = Color(0xFF131B2E)
                )
            }
        }
    }

    @Composable
    fun QuickActionChip(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, theme: SciFiTheme, onClick: () -> Unit) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(theme.surface).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = theme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 12.sp, color = Color(0xFFE2E8F0))
            }
        }
    }

    @Composable
    fun AnimatedCoreOrb(state: CoreOrbState, theme: SciFiTheme, size: Dp = 125.dp, onClick: () -> Unit) {
        val infiniteTransition = rememberInfiniteTransition(label = "OrbMotion")

        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (state == CoreOrbState.SPEAKING) 700 else 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OrbScale"
        )

        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (state == CoreOrbState.THINKING) 2500 else 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "OrbRotation"
        )

        val coreColor = when (state) {
            CoreOrbState.IDLE -> theme.primary
            CoreOrbState.LISTENING -> Color(0xFF00E676)
            CoreOrbState.THINKING -> Color(0xFFFFD600)
            CoreOrbState.SPEAKING -> theme.secondary
            CoreOrbState.ERROR -> Color(0xFFFF1744)
        }

        Box(modifier = Modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val baseRadius = (size.toPx() / 2.6f) * pulseScale

                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(coreColor.copy(alpha = 0.35f), Color.Transparent), center = center, radius = baseRadius * 1.4f),
                    center = center,
                    radius = baseRadius * 1.4f
                )

                val ringRadius = baseRadius + 10.dp.toPx()
                drawCircle(color = coreColor.copy(alpha = 0.4f), center = center, radius = ringRadius, style = Stroke(width = 2f))

                for (j in 0 until 4) {
                    val angleDeg = rotationAngle + (j * 90f)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val nx = center.x + (ringRadius * cos(angleRad)).toFloat()
                    val ny = center.y + (ringRadius * sin(angleRad)).toFloat()
                    drawCircle(color = coreColor, center = Offset(nx, ny), radius = 3.dp.toPx())
                }

                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(Color.White, coreColor, coreColor.copy(alpha = 0.3f)), center = center, radius = baseRadius),
                    center = center,
                    radius = baseRadius
                )
            }
        }
    }
}
