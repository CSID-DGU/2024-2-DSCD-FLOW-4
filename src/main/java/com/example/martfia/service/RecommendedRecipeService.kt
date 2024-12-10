package com.example.martfia.service

import com.example.martfia.model.request.RecommendedRecipeRequest
import com.example.martfia.model.response.CookingAssistantResponse
import com.example.martfia.model.response.RecommendedRecipeDetailResponse
import com.example.martfia.model.response.RecommendedRecipeResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RecommendedRecipeService {
    @POST("api/recipe")
    fun getRecommendedRecipes(
        @Body request: RecommendedRecipeRequest
    ): Call<RecommendedRecipeResponse>

    @GET("api/recipe/details/{id}")
    fun getRecipeDetails(
        @Path("id") id: Int // 조회할 레시피 ID
    ): Call<RecommendedRecipeDetailResponse>

    //    @POST("api/recipe/{recipe_id}/start")
//    fun startCookingAssistant(
//        @Path("recipe_id") recipeId: Int
//    ): Call<CookingAssistantResponse>
//}
    @POST("api/recipe/start")
    fun startCookingAssistant(): Call<CookingAssistantResponse>
}