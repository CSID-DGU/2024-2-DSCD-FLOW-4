package com.example.martfia.service

import com.example.martfia.model.response.RecipeQueryResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface CookingAssistantService {
    @Multipart
    @POST("api/recipe/query")
    fun queryRecipeStep(
        @Part audio: MultipartBody.Part? = null,
        @Part("text") text: String? = null,
        @Part("current_step") currentStep: Int
    ): Call<RecipeQueryResponse>
}
