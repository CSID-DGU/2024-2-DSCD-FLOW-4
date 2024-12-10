package com.example.martfia.model.response

data class CookingAssistantResponse(
    val message: String,
    val audio_url: String
) {
    val audioUrl: String
        get() = audio_url // 이름을 맞춤식으로 변환
}

