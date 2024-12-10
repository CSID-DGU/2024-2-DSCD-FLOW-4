package com.example.martfia.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient(
    val image_url: String, // 재료 이미지 URL
    val name: String        // 재료 이름
) : Parcelable
