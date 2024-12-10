package com.example.martfia.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecommendedRecipe(
    val id: Int,              // 레시피 ID
    val image: String,        // 이미지 URL
    val foodName: String,     // 레시피 이름
    val cookingTime: String   // 요리 시간
) : Parcelable
