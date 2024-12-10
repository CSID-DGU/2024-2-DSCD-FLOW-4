package com.example.martfia.service

import com.example.martfia.model.request.ImageUploadRequest
import com.example.martfia.model.request.ReceiptUploadRequest
import com.example.martfia.model.response.ImageUploadOnlyResponse
import com.example.martfia.model.response.ImageUploadResponse
import com.example.martfia.model.response.IngredientsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface IngredientRecognitionService {

    // 이미지 파일 업로드 API
    @Multipart
    @POST("api/upload")
    fun uploadImageFile(
        @Part image: MultipartBody.Part
    ): Call<ImageUploadOnlyResponse>

    // 재료 이미지 업로드 API
    @POST("api/image/food")
    fun uploadImage(
        @Body request: ImageUploadRequest
    ): Call<ImageUploadResponse>

    // 온라인 영수증 이미지 업로드 API
    @POST("api/image/receipt")
    fun uploadReceipt(
        @Body request: ReceiptUploadRequest
    ): Call<ImageUploadResponse>


    // 인식된 재료 반환 API
    @GET("api/ingredients")
    fun getIngredients(): Call<IngredientsResponse>

}
