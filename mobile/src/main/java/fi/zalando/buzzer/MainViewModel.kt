package fi.zalando.buzzer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext


private const val TAG = "MainViewModel"
private const val NAME = "DEVICE"
private const val UUID_DEFAULT = "9d577744-0375-11e9-8eb2-f2801f1b9fd1"

class MainViewModel : ViewModel(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    private var bluetoothSocket: BluetoothSocket? = null

    val state: MutableLiveData<ViewState> = MutableLiveData()

    fun connectAsServer(bluetoothAdapter: BluetoothAdapter) {
        val serverSocket: BluetoothServerSocket? = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            NAME,
            UUID.fromString(UUID_DEFAULT)
        )
        if (serverSocket != null) {
            launch {
                // Keep listening until exception occurs or a socket is returned.
                var shouldLoop = true
                while (shouldLoop) {
                    val socket: BluetoothSocket? = withContext(Dispatchers.Default) {
                        try {
                            serverSocket.accept()
                        } catch (e: IOException) {
                            Log.e(TAG, "Socket's accept() method failed", e)
                            shouldLoop = false
                            null
                        }
                    }
                    socket?.also {
                        bluetoothSocket = it
                        this@MainViewModel.state.postValue(ViewState(TYPE.HOST, null))
                        listenForMessages()
                        serverSocket.close()
                        shouldLoop = false
                    }
                }
            }
        }
    }

    fun connectAsClient(bluetoothDevice: BluetoothDevice, bluetoothAdapter: BluetoothAdapter) {
        val socket: BluetoothSocket? =
            bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUID_DEFAULT))
        bluetoothAdapter.cancelDiscovery()
        if (socket != null) {
            bluetoothSocket = socket
            launch {
                withContext(Dispatchers.Default) {
                    socket.connect()
                }
                this@MainViewModel.state.postValue(ViewState(TYPE.CLIENT, null))
                listenForMessages()
            }
        }
    }

    private fun listenForMessages() {
        val inputStream: InputStream = bluetoothSocket?.inputStream ?: return
        val buffer = ByteArray(1024) // mmBuffer store for the stream
        launch {
            withContext(Dispatchers.Default) {
                var message: String // bytes returned from read()
                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    message = try {
                        String(
                            buffer, 0, inputStream.read(buffer)
                        )
                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream was disconnected", e)
                        break
                    }
                    this@MainViewModel.state.postValue(ViewState(null, message))
                }
            }
        }
    }

    override fun onCleared() {
        job.cancel()
    }

    fun sendMessage(message: String) {
        val outputStream: OutputStream = bluetoothSocket?.outputStream ?: return
        launch {
            withContext(Dispatchers.Default) {
                try {
                    outputStream.write(message.toByteArray())
                } catch (e: IOException) {
                    Log.e(TAG, "Error occurred when sending data", e)
                    return@withContext
                }
            }
        }
    }
}

data class ViewState(
    val type: TYPE?,
    val message: String?
)

enum class TYPE {
    HOST, CLIENT
}
