package fi.zalando.buzzer

data class TriviaResponse(val results: List<TriviaQuestion>)

data class TriviaQuestion(val question: String, val correct_answer: Boolean)
