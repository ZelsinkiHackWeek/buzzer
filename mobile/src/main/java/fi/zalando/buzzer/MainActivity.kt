package fi.zalando.buzzer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private var bluetoothAdapter: BluetoothAdapter? = null

    private var map: Map<String, BluetoothDevice> = mutableMapOf()

    private var nodes: Set<Node>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initBluetooth()
        initConnectivity()
        findViewById<Button>(R.id.button).apply {
            setOnClickListener {
                sendMessage(edit_text_wear.text.toString().trim())
                edit_text_wear.setText("")
            }
        }
        button_host.setOnClickListener {
            viewModel.connectAsServer(bluetoothAdapter!!)
        }
        button_join.setOnClickListener {
            joinGame()
        }
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.state.observe(this, Observer {
            if (it.type != null) {
                Toast.makeText(
                    this@MainActivity,
                    it.type.name,
                    Toast.LENGTH_LONG
                ).show()
            }
            it.message?.run {
                Toast.makeText(
                    this@MainActivity,
                    this,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        button_send.setOnClickListener {
            viewModel.sendMessage(edit_text.text.toString())
        }
    }

    private fun joinGame() {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        map = pairedDevices?.associateBy(
            {
                it.name
            }, {
                it
            }
        ) ?: emptyMap()
        if (map.isNotEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Choose device")
            val i = map.keys.toTypedArray()
            builder.setItems(i) { _, which ->
                map[i[which]]?.run {
                    viewModel.connectAsClient(this, bluetoothAdapter!!)
                }
            }
            builder.show()
        }
    }

    private fun initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // No support
            button_host.isEnabled = false
            button_join.isEnabled = false
        } else {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 111)
            } else {
                button_host.isEnabled = true
                button_join.isEnabled = true
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 111 && resultCode == Activity.RESULT_OK) {
            button_host.isEnabled = true
            button_join.isEnabled = false
        } else {
            button_host.isEnabled = true
            button_join.isEnabled = true
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

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
// ... (Add other message types here as needed.)


