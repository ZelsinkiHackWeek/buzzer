package fi.zalando.buzzer

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

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
                sendMessage()
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
                    Toast.makeText(this, "message " + String(messageEvent.data), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendMessage() {
        nodes?.firstOrNull()?.id?.let { id ->
            Wearable.getMessageClient(this).sendMessage(
                id, "message", "Hello from watch".toByteArray()
            )
        }
    }
}
