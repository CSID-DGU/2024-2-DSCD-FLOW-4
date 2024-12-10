package com.example.martfia

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.martfia.model.response.RecipeQueryResponse
import com.example.martfia.service.CookingAssistantService
import com.example.martfia.service.MartfiaRetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*


//import android.media.MediaPlayer
//import android.os.Handler
//import android.os.Looper
//
class CookingAssistantActivity : AppCompatActivity() {

    private var currentStep = 1 // 현재 단계
    private var recipeId: Int = -1 // 레시피 ID (전역 변수로 관리)
    private lateinit var cookingStepMessage: TextView
    private lateinit var nextStepButton: Button
    private lateinit var backButton: ImageView
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooking_assistant)



        // 뷰 초기화
        cookingStepMessage = findViewById(R.id.cookingStepMessage)
        nextStepButton = findViewById(R.id.nextStepButton)
        backButton = findViewById(R.id.backButton)

        // 음성 인식기 초기화
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        // 초기 단계 불러오기
        fetchCookingStep(null, null, currentStep)

        // "다음 단계" 버튼 클릭 이벤트
        nextStepButton.setOnClickListener {
            currentStep++
            fetchCookingStep(null, null, currentStep)
        }

        // "뒤로 가기" 버튼 클릭 이벤트
        backButton.setOnClickListener {
            finish() // 액티비티 종료
        }
    }

    private fun fetchCookingStep(audioFile: File?, text: String?, step: Int) {
        val service = MartfiaRetrofitClient.createService(CookingAssistantService::class.java)

        val audioPart: MultipartBody.Part? = audioFile?.let {
            val requestBody = it.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("audio", it.name, requestBody)
        }

        service.queryRecipeStep(audioPart, text, step).enqueue(object : Callback<RecipeQueryResponse> {
            override fun onResponse(
                call: Call<RecipeQueryResponse>,
                response: Response<RecipeQueryResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()!!
                    cookingStepMessage.text = responseData.text // 메시지 업데이트
                    playAudio(responseData.audio_url) // 음성 응답 재생
                } else {
                    Toast.makeText(this@CookingAssistantActivity, "조리 단계를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecipeQueryResponse>, t: Throwable) {
                Toast.makeText(this@CookingAssistantActivity, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun playAudio(audioUrl: String) {
        Log.d("CookingAssistantActivity", "Audio URL: $audioUrl")
        stopAudio()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioUrl)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    Log.d("CookingAssistantActivity", "Audio playback completed.")
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("CookingAssistantActivity", "Error playing audio: $e")
                Toast.makeText(this@CookingAssistantActivity, "오디오를 재생할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                reset()
                release()
            }
        }
        mediaPlayer = null
    }

    private fun setupSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@CookingAssistantActivity, "음성 인식을 시작합니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 문제 발생"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류 발생"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류 발생"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 시간 초과"
                    SpeechRecognizer.ERROR_NO_MATCH -> "일치하는 결과 없음"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기가 바쁨"
                    SpeechRecognizer.ERROR_SERVER -> "서버 오류 발생"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "입력이 너무 짧음"
                    else -> "알 수 없는 오류 발생"
                }
                Toast.makeText(this@CookingAssistantActivity, "음성 인식 실패: $errorMessage", Toast.LENGTH_SHORT).show()
                Log.e("SpeechRecognizer", "Error occurred: $errorMessage ($error)")
            }


            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0] // 가장 첫 번째 결과를 가져옴
                    Toast.makeText(this@CookingAssistantActivity, "음성 인식 결과: $recognizedText", Toast.LENGTH_SHORT).show()
                    Log.d("SpeechRecognizer", "Recognition result: $recognizedText")
                } else {
                    Toast.makeText(this@CookingAssistantActivity, "음성 인식 결과를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("SpeechRecognizer", "No recognition results")
                }
                val voiceResults = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val userSpeech = voiceResults?.get(0) ?: ""


                if (userSpeech.contains("다음", true)) {
                    currentStep++
                    fetchCookingStep(null, userSpeech, currentStep)
                } else {
                    Toast.makeText(this@CookingAssistantActivity, "다음 단계 요청이 인식되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // 음성 인식 시작
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
        speechRecognizer.destroy()
    }
}
