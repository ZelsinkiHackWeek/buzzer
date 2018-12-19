package fi.zalando.buzzer

import retrofit2.Call
import retrofit2.http.GET

interface TriviaService {
    @GET("?amount=10&type=boolean")
    fun getQuestions(): Call<TriviaResponse>
}
