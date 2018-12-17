package fi.zalando.buzzer

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable


class MainActivity : AppCompatActivity() {

    private var nodes: Set<Node>? = null
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.edit_text)
        initConnectivity()
        findViewById<Button>(R.id.button).apply {
            setOnClickListener {
                sendMessage(editText.text.toString().trim())
                editText.setText("")
            }
        }
    }

    private fun initConnectivity() {
        val capabilityInfoTask = Wearable.getCapabilityClient(this)
            .getCapability("watch_client", CapabilityClient.FILTER_ALL)

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
                    Toast.makeText(this, "message " + String(messageEvent.data), Toast.LENGTH_LONG).show()
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
}
