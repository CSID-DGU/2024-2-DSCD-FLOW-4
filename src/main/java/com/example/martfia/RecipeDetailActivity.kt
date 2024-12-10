package com.example.martfia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.martfia.adapter.CookingStepAdapter
import com.example.martfia.model.response.CookingAssistantResponse
import com.example.martfia.model.response.RecommendedRecipeDetailResponse
import com.example.martfia.service.MartfiaRetrofitClient
import com.example.martfia.service.RecommendedRecipeService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecipeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val recipeImageView = findViewById<ImageView>(R.id.recipeImageView)
        val recipeNameTextView = findViewById<TextView>(R.id.recipeNameTextView)
        val recipeTimeTextView = findViewById<TextView>(R.id.recipeTimeTextView)
        val recipeStepsRecyclerView = findViewById<RecyclerView>(R.id.recipeStepsRecyclerView)
        val assistantButton = findViewById<LinearLayout>(R.id.assistantButton)

        backButton.setOnClickListener {
            val intent = Intent(this, RecommendedRecipeActivity::class.java)
            startActivity(intent)
            finish()
        }

        val recipeId = intent.getIntExtra("recipe_id", -1)
        if (recipeId == -1) {
            Toast.makeText(this, "잘못된 레시피 ID입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 레시피 상세 정보 가져오기
        val service = MartfiaRetrofitClient.createService(RecommendedRecipeService::class.java)
        service.getRecipeDetails(recipeId).enqueue(object : Callback<RecommendedRecipeDetailResponse> {
            override fun onResponse(
                call: Call<RecommendedRecipeDetailResponse>,
                response: Response<RecommendedRecipeDetailResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val recipeDetail = response.body()!!.recipe
                    recipeNameTextView.text = recipeDetail.foodName
                    recipeTimeTextView.text = recipeDetail.cookingTime

                    Glide.with(this@RecipeDetailActivity)
                        .load(recipeDetail.image)
                        .centerCrop()
                        .placeholder(R.drawable.img_cooking)
                        .into(recipeImageView)

                    // RecyclerView 설정
                    recipeStepsRecyclerView.layoutManager = LinearLayoutManager(this@RecipeDetailActivity)
                    recipeStepsRecyclerView.adapter = CookingStepAdapter(recipeDetail.instructions)
                } else {
                    Toast.makeText(this@RecipeDetailActivity, "레시피 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecommendedRecipeDetailResponse>, t: Throwable) {
                Toast.makeText(this@RecipeDetailActivity, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })

        // 조리 Assistant 버튼 동작 연결
        assistantButton.setOnClickListener {
            startCookingAssistantSession()
        }
    }

    private fun startCookingAssistantSession() {
        val service = MartfiaRetrofitClient.createService(RecommendedRecipeService::class.java)

        service.startCookingAssistant().enqueue(object : Callback<CookingAssistantResponse> {
            override fun onResponse(
                call: Call<CookingAssistantResponse>,
                response: Response<CookingAssistantResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()!!
                    val intent = Intent(this@RecipeDetailActivity, CookingAssistantActivity::class.java)
                    intent.putExtra("welcome_message", responseData.message)
                    intent.putExtra("audio_url", responseData.audio_url)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@RecipeDetailActivity, "조리 어시스턴트를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CookingAssistantResponse>, t: Throwable) {
                Toast.makeText(this@RecipeDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
