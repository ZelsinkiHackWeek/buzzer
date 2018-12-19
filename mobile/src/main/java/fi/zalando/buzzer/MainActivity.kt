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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    private var nodes: Set<Node>? = null
    private lateinit var editText: EditText
    private var currentQuestion: Int = 0
    private var questions: List<TriviaQuestion>? = null



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
        getQuestions()
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
                    val answer = String(messageEvent.data).run {
                        contains("true") || contains("yes") || contains("correct")
                    }
                    if (questions!![currentQuestion].correct_answer == answer) {
                        Toast.makeText(this, "Correct!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Nope!", Toast.LENGTH_LONG).show()
                    }
                    currentQuestion++
                    sendMessage(questions!![currentQuestion].question)
                }
            }
        }
    }

    private fun getQuestions() {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://opentdb.com/api.php/")
            .build()

        val triviaService = retrofit.create(TriviaService::class.java)
        triviaService.getQuestions().enqueue(object: Callback<TriviaResponse> {
            override fun onResponse(call: Call<TriviaResponse>, response: Response<TriviaResponse>) {
                response.body()?.results?.run {
                    questions = this
                    currentQuestion = 0
                    sendMessage(questions!![currentQuestion].question)
                }
            }

            override fun onFailure(call: Call<TriviaResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun sendMessage(message: String) {
        nodes?.firstOrNull()?.id?.let { id ->
            Wearable.getMessageClient(this).sendMessage(
                id, "message", message.toByteArray()
            )
        }
    }
}
