package com.example.martfia.model.response

data class IngredientsResponse(
    val images: Map<String, String>, // 재료 이름과 이미지 URL 매핑
    val ingredients: List<String>    // 재료 이름 리스트
)
