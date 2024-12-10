package com.example.martfia.model.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecommendedRecipeDetailResponse(
    val recipe: RecipeDetail // 응답 데이터의 `recipe` 키
) : Parcelable

@Parcelize
data class RecipeDetail(
    val cookingTime: String,               // 조리 시간
    val foodName: String,                  // 요리 이름
    val image: String,                     // 이미지 URL
    val instructions: Map<String, String>  // 단계별 조리 방법 (단계 번호 -> 설명)
) : Parcelable
