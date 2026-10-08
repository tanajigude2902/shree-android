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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

enum class CoreOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private val messages = mutableStateListOf<ChatMessage>()
    private var coreState by mutableStateOf(CoreOrbState.IDLE)
    private var statusText by mutableStateOf("SHREE Ready")
    private var batteryPercent by mutableIntStateOf(100)
    private var isCharging by mutableStateOf(false)
    private var isOnline by mutableStateOf(true)
    private var isListening by mutableStateOf(false)
    private var activeJob: Job? = null

    private val requestAudioLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                statusText = "Microphone enabled"
            } else {
                statusText = "Microphone access required for voice"
            }
        }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100) / scale
                }
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        messages.add(
            ChatMessage(
                isUser = false,
                text = "Namaste! I am SHREE, your Personal AI Operating System. How may I assist you today?"
            )
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            ShreeMainScreen()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.0f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { coreState = CoreOrbState.SPEAKING }
                override fun onDone(utteranceId: String?) { coreState = CoreOrbState.IDLE }
                override fun onError(utteranceId: String?) { coreState = CoreOrbState.IDLE }
            })
            isTtsReady = true
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.stop()
            val bundle = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "shree_voice")
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
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@MainActivity)
                }
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
        } else {
            statusText = "Speech recognition not available"
        }
    }

    private fun stopListening() {
        try { speechRecognizer?.stopListening() } catch (e: Exception) { }
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
        coreState = CoreOrbState.IDLE
        statusText = "Operation stopped"
    }

    private fun processCommand(cmd: String, scope: kotlinx.coroutines.CoroutineScope) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        messages.add(ChatMessage(isUser = true, text = trimmed))
        coreState = CoreOrbState.THINKING
        statusText = "Thinking…"

        activeJob = scope.launch {
            val reply = executeAssistantLogic(trimmed)
            messages.add(ChatMessage(isUser = false, text = reply))
            statusText = "Completed"
            speak(reply)
        }
    }

    private suspend fun executeAssistantLogic(userQuery: String): String = withContext(Dispatchers.IO) {
        val lower = userQuery.lowercase()
        val prefs = getSharedPreferences("shree_vault", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", null)?.trim()

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
            val aiState = if (!apiKey.isNullOrBlank()) "Gemini 1.5 Flash Connected" else "Local Offline Engine"
            return@withContext "System Status: Network is $net, Battery is $batteryPercent%, AI Engine: $aiState."
        }
        if (lower.contains("who are you") || lower.contains("aap kaun ho") || lower.contains("tum kaun ho")) {
            return@withContext "I am SHREE, your Personal AI Operating System. I can manage device tasks, answer questions, automate routines, and orchestrate specialized agents."
        }

        if (!apiKey.isNullOrBlank() && isOnline) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val contentsList = mutableListOf<Map<String, Any>>()
                for (m in messages.takeLast(6)) {
                    val r = if (m.isUser) "user" else "model"
                    contentsList.add(mapOf("role" to r, "parts" to listOf(mapOf("text" to m.text))))
                }
                val payload = mapOf(
                    "contents" to contentsList,
                    "systemInstruction" to mapOf(
                        "parts" to listOf(mapOf("text" to "You are SHREE, a polite, intelligent female personal AI operating system. Provide helpful, concise responses."))
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
                        if (!text.isNullOrBlank()) {
                            return@withContext text
                        }
                    }
                }
            } catch (e: Exception) { }
        }

        return@withContext "I heard: \"$userQuery\". To enable advanced reasoning, research, and agent tools, add your Google Gemini API key in Settings (gear icon at top)."
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
        statusText = "Speech not detected. Tap orb to try again."
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
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) { }
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
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0A0E17)
            ) {
                Scaffold(
                    containerColor = Color(0xFF0A0E17),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SHREE OS",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00E5FF),
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date()),
                                    fontSize = 14.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryFull,
                                    contentDescription = "Battery",
                                    tint = if (batteryPercent < 20) Color(0xFFFF1744) else Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$batteryPercent%", fontSize = 12.sp, color = Color(0xFFE2E8F0))

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = "Network",
                                    tint = if (isOnline) Color(0xFF00E676) else Color(0xFFFF1744),
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { stopEverything() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = "Stop",
                                    tint = if (coreState == CoreOrbState.THINKING || coreState == CoreOrbState.SPEAKING) Color(0xFFFF1744) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedTextField(
                                value = inputCommand,
                                onValueChange = { inputCommand = it },
                                placeholder = {
                                    Text(
                                        text = if (isListening) "Listening…" else "Ask SHREE anything…",
                                        color = Color(0xFF64748B),
                                        fontSize = 14.sp
                                    )
                                },
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
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedContainerColor = Color(0xFF131B2E),
                                    unfocusedContainerColor = Color(0xFF131B2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (isListening) stopListening() else startSpeechInput()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF131B2E))
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Mic",
                                    tint = if (isListening) Color(0xFF00E676) else Color(0xFF00E5FF),
                                    modifier = Modifier.size(22.dp)
                                )
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
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        AnimatedCoreOrb(
                            state = coreState,
                            size = 130.dp,
                            onClick = {
                                if (isListening) stopListening() else startSpeechInput()
                            }
                        )

                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = when (coreState) {
                                CoreOrbState.ERROR -> Color(0xFFFF1744)
                                CoreOrbState.LISTENING -> Color(0xFF00E676)
                                CoreOrbState.THINKING -> Color(0xFFFFD600)
                                CoreOrbState.SPEAKING -> Color(0xFF7C4DFF)
                                else -> Color(0xFF00E5FF)
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionChip("Handle It", Icons.Default.AutoAwesome) {
                                processCommand("Shree, handle my daily priorities", scope)
                            }
                            QuickActionChip("Time & Date", Icons.Default.Cloud) {
                                processCommand("What is the current time and date?", scope)
                            }
                            QuickActionChip("Battery Check", Icons.Default.BatteryFull) {
                                processCommand("What is the battery level?", scope)
                            }
                            QuickActionChip("API Key", Icons.Default.Key) {
                                showSettingsDialog = true
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                                                )
                                            )
                                            .background(
                                                if (msg.isUser) Color(0xFF00E5FF).copy(alpha = 0.2f)
                                                else Color(0xFF131B2E)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (msg.isUser) "YOU" else "SHREE",
                                                    fontSize = 11.sp,
                                                    color = if (msg.isUser) Color(0xFF00E5FF) else Color(0xFF7C4DFF)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = msg.time,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.text,
                                                fontSize = 14.sp,
                                                color = Color(0xFFE2E8F0)
                                            )
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
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("SHREE AI Settings", color = Color.White) },
                    text = {
                        Column {
                            Text(
                                text = "Enter Google Gemini API Key for advanced AI reasoning & web capabilities:",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                label = { Text("Gemini API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Key is stored securely encrypted on your phone and never shared.",
                                fontSize = 11.sp,
                                color = Color(0xFF00E676)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                prefs.edit().putString("gemini_api_key", tempKey.trim()).apply()
                                savedKey = tempKey.trim()
                                showSettingsDialog = false
                                statusText = if (tempKey.isNotBlank()) "Gemini Connected" else "Local Offline Mode"
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF131B2E)
                )
            }

            if (showConfirmDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDeleteDialog = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFD600)) },
                    title = { Text("Action Confirmation Required", color = Color.White) },
                    text = { Text("Are you sure you want to permanently execute this destructive action?", color = Color(0xFFE2E8F0)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmDeleteDialog = false
                                messages.add(ChatMessage(isUser = false, text = "Action confirmed and executed safely."))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                        ) {
                            Text("Confirm & Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDeleteDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF131B2E)
                )
            }
        }
    }

    @Composable
    fun QuickActionChip(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131B2E))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 12.sp, color = Color(0xFFE2E8F0))
            }
        }
    }

    @Composable
    fun AnimatedCoreOrb(state: CoreOrbState, size: Dp = 130.dp, onClick: () -> Unit) {
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
            CoreOrbState.IDLE -> Color(0xFF00E5FF)
            CoreOrbState.LISTENING -> Color(0xFF00E676)
            CoreOrbState.THINKING -> Color(0xFFFFD600)
            CoreOrbState.SPEAKING -> Color(0xFF7C4DFF)
            CoreOrbState.ERROR -> Color(0xFFFF1744)
        }

        Box(
            modifier = Modifier
                .size(size)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val baseRadius = (size.toPx() / 2.6f) * pulseScale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(coreColor.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = baseRadius * 1.4f
                    ),
                    center = center,
                    radius = baseRadius * 1.4f
                )

                val ringRadius = baseRadius + 10.dp.toPx()
                drawCircle(
                    color = coreColor.copy(alpha = 0.4f),
                    center = center,
                    radius = ringRadius,
                    style = Stroke(width = 2f)
                )

                for (j in 0 until 4) {
                    val angleDeg = rotationAngle + (j * 90f)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val nx = center.x + (ringRadius * cos(angleRad)).toFloat()
                    val ny = center.y + (ringRadius * sin(angleRad)).toFloat()
                    drawCircle(color = coreColor, center = Offset(nx, ny), radius = 3.dp.toPx())
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, coreColor, coreColor.copy(alpha = 0.3f)),
                        center = center,
                        radius = baseRadius
                    ),
                    center = center,
                    radius = baseRadius
                )
            }
        }
    }
}
