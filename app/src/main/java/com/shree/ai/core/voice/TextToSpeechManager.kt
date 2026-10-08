package com.shree.ai.core.voice
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var isReady = false

    fun initialize() { tts = TextToSpeech(context, this) }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            tts?.setPitch(1.05f)
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shree_tts")
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.shutdown() }
}
