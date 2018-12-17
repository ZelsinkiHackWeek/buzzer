package fi.zalando.buzzer

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.util.*

class MainActivity : WearableActivity() {

    private var nodes: Set<Node>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Enables Always-on
        setAmbientEnabled()
        initConnectivity()
        findViewById<Button>(R.id.button).apply {
            setOnClickListener {
                recordSpeech()
            }
        }
    }

    private fun initConnectivity() {
        val capabilityInfoTask = Wearable.getCapabilityClient(this)
            .getCapability("watch_server", CapabilityClient.FILTER_ALL)

        capabilityInfoTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                nodes = task.result?.nodes
            } else {
                Log.d("FAIL", "Capability request failed to return any results.")
            }
        }

        Wearable.getMessageClient(this).addListener { messageEvent ->
            when (messageEvent.path) {
                "message" -> {
                    val message = String(messageEvent.data)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    playMessage(message)
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        nodes?.firstOrNull()?.id?.let { id ->
            Wearable.getMessageClient(this).sendMessage(
                id, "message", message.toByteArray()
            )
        }
    }

    private fun recordSpeech() {
        try {
            startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    "en-US"
                )
            }, 123)
        } catch (e: Throwable) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            123 -> {
                if (resultCode == RESULT_OK && null != data) {
                    val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    sendMessage(text.joinToString(" "))
                }
            }
        }
    }

    var tts: TextToSpeech? = null
    private fun playMessage(message: String) {
        tts = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.run {
                    language = Locale.US
                    speak(message, TextToSpeech.QUEUE_FLUSH, null)
                }
            }
        })
    }
}
