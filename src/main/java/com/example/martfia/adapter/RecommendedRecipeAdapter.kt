package com.example.martfia.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.martfia.R
import com.example.martfia.RecipeDetailActivity
import com.example.martfia.model.RecommendedRecipe

class RecommendedRecipeAdapter(
    private val recipeList: List<RecommendedRecipe>,
    private val onRecipeClick: (RecommendedRecipe) -> Unit // 레시피 클릭 시 처리할 콜백 함수
) : RecyclerView.Adapter<RecommendedRecipeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeImage: ImageView = view.findViewById(R.id.recipeImage)
        val recipeName: TextView = view.findViewById(R.id.recipeName)
        val recipeTime: TextView = view.findViewById(R.id.recipeTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommended_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipeList[position]

        // 이미지 설정
        Glide.with(holder.itemView.context)
            .load(recipe.image)
            .centerCrop()
            .placeholder(R.drawable.img_cooking) // 기본 이미지 설정
            .into(holder.recipeImage)

        // 레시피 이름 및 소요 시간 설정
        holder.recipeName.text = recipe.foodName
        holder.recipeTime.text = recipe.cookingTime

        // 레시피 클릭 시 레시피 상세 화면으로 이동
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RecipeDetailActivity::class.java)
            intent.putExtra("recipe_id", recipe.id) // 레시피 ID 전달
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = recipeList.size
}
