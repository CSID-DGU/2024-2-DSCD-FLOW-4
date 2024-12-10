package com.example.martfia.model.response

data class RecipeQueryResponse(
    val text: String,
    val audio_url: String,
    val current_step: Int
)
