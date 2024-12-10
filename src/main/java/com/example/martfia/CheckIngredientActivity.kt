package com.example.martfia

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.martfia.adapter.IngredientAdapter
import com.example.martfia.model.Ingredient
import com.example.martfia.model.request.RecommendedRecipeRequest
import com.example.martfia.model.response.RecommendedRecipeResponse
import com.example.martfia.service.MartfiaRetrofitClient
import com.example.martfia.service.RecommendedRecipeService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckIngredientActivity : AppCompatActivity() {

    private lateinit var ingredientAdapter: IngredientAdapter
    private val ingredientList = mutableListOf<Ingredient>() // 재료 리스트
    private lateinit var recommendedRecipeService: RecommendedRecipeService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_ingredient)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val ingredientRecyclerView = findViewById<RecyclerView>(R.id.ingredientRecyclerView)
        val ingredientEditText = findViewById<EditText>(R.id.ingredientEditText)
        val addIngredientButton = findViewById<Button>(R.id.addIngredientButton)
        val recommendRecipeButton = findViewById<Button>(R.id.recommendRecipeButton)

        backButton.setOnClickListener { finish() }

        // RecyclerView 설정
        ingredientAdapter = IngredientAdapter(
            ingredientList,
            onDeleteClick = { ingredient -> deleteIngredient(ingredient) }
        )
        ingredientRecyclerView.layoutManager = LinearLayoutManager(this)
        ingredientRecyclerView.adapter = ingredientAdapter

        // 초기 재료 리스트 가져오기
        val savedIngredients = intent.getParcelableArrayListExtra<Ingredient>("saved_ingredients") ?: arrayListOf()
        ingredientList.addAll(savedIngredients)
        ingredientAdapter.notifyDataSetChanged()

        // 재료 추가 버튼 클릭 이벤트
        addIngredientButton.setOnClickListener {
            val ingredientName = ingredientEditText.text.toString().trim()
            if (ingredientName.isNotEmpty()) {
                val newIngredient = Ingredient(image_url = "", name = ingredientName) // 이미지 URL은 비어 있을 수 있음
                ingredientList.add(newIngredient)
                ingredientAdapter.notifyItemInserted(ingredientList.size - 1)
                ingredientEditText.text.clear()
            } else {
                Toast.makeText(this, "재료 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 레시피 추천 버튼 클릭 이벤트
        recommendRecipeButton.setOnClickListener {
            if (ingredientList.isNotEmpty()) {
                sendIngredientsToBackend()
            } else {
                Toast.makeText(this, "재료를 추가해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteIngredient(ingredient: Ingredient) {
        val index = ingredientList.indexOf(ingredient)
        if (index != -1) {
            ingredientList.removeAt(index)
            ingredientAdapter.notifyItemRemoved(index)
        }
    }

    private fun sendIngredientsToBackend() {
        val ingredientNames = ingredientList.map { it.name }

        val request = RecommendedRecipeRequest(
            ingredients = ingredientNames
        )

        recommendedRecipeService = MartfiaRetrofitClient.createService(RecommendedRecipeService::class.java)
        recommendedRecipeService.getRecommendedRecipes(request).enqueue(object : Callback<RecommendedRecipeResponse> {
            override fun onResponse(call: Call<RecommendedRecipeResponse>, response: Response<RecommendedRecipeResponse>) {
                if (response.isSuccessful) {
                    val recipeList = response.body()?.recipes
                    if (!recipeList.isNullOrEmpty()) {
                        val intent = Intent(this@CheckIngredientActivity, RecommendedRecipeActivity::class.java)
                        intent.putParcelableArrayListExtra("recipe_list", ArrayList(recipeList)) // Parcelable로 전달
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@CheckIngredientActivity, "추천 레시피가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@CheckIngredientActivity, "레시피 추천을 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecommendedRecipeResponse>, t: Throwable) {
                Toast.makeText(this@CheckIngredientActivity, "API 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
