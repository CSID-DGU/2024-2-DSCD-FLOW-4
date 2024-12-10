package com.example.martfia

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.martfia.adapter.RecommendedRecipeAdapter
import com.example.martfia.model.Ingredient
import com.example.martfia.model.RecommendedRecipe
import com.example.martfia.model.request.RecommendedRecipeRequest
import com.example.martfia.model.response.RecommendedRecipeResponse
import com.example.martfia.service.MartfiaRetrofitClient
import com.example.martfia.service.RecommendedRecipeService
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecommendedRecipeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommended_recipe)

        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // RecyclerView 설정
        val recipeRecyclerView = findViewById<RecyclerView>(R.id.recommendedRecipeRecyclerView)
        recipeRecyclerView.layoutManager = LinearLayoutManager(this)

        // ChipGroup 설정
        val chipGroup = findViewById<ChipGroup>(R.id.ingredientChipGroup)

        // Intent로부터 재료 리스트 받기
        val ingredients = intent.getParcelableArrayListExtra<Ingredient>("saved_ingredients") ?: arrayListOf()

        // ChipGroup에 Chip 추가
        for (ingredient in ingredients) {
            val chip = Chip(this)
            chip.text = ingredient.name // 재료 이름 설정
            chip.isClickable = false   // 클릭 비활성화
            chip.isCheckable = false   // 선택 비활성화
            chipGroup.addView(chip)    // ChipGroup에 추가
        }

        // API 호출
        fetchRecommendedRecipes(ingredients, recipeRecyclerView)
    }

    private fun fetchRecommendedRecipes(ingredients: List<Ingredient>, recipeRecyclerView: RecyclerView) {
        val recommendedRecipeService = MartfiaRetrofitClient.createService(RecommendedRecipeService::class.java)

        // 요청 데이터 생성
        val request = RecommendedRecipeRequest(
            ingredients = ingredients.map { it.name } // 재료 이름 리스트로 변환
        )

        // Retrofit API 호출
        recommendedRecipeService.getRecommendedRecipes(request).enqueue(object : Callback<RecommendedRecipeResponse> {
            override fun onResponse(
                call: Call<RecommendedRecipeResponse>,
                response: Response<RecommendedRecipeResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val recommendedRecipes = response.body()!!.recipes

                    // 어댑터 설정
                    val adapter = RecommendedRecipeAdapter(recommendedRecipes) { selectedRecipe ->
                        // TODO: 레시피 클릭 시 상세 화면으로 이동 (추후 구현 예정)
                        Toast.makeText(this@RecommendedRecipeActivity, "${selectedRecipe.foodName} 클릭됨", Toast.LENGTH_SHORT).show()
                    }
                    recipeRecyclerView.adapter = adapter
                } else {
                    Toast.makeText(this@RecommendedRecipeActivity, "추천 레시피를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecommendedRecipeResponse>, t: Throwable) {
                Toast.makeText(this@RecommendedRecipeActivity, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
