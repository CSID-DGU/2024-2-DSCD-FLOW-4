package com.example.martfia.model.response

// 서버 응답 매핑
data class ImageUploadResponse(
    val saved_ingredients: List<SavedIngredient> // 저장된 재료 리스트
)

data class SavedIngredient(
    val image_url: String, // 재료 이미지 URL
    val name: String        // 재료 이름
)
