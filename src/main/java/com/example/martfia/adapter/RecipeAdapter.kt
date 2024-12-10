package com.example.martfia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.martfia.R
import com.example.martfia.model.Recipe


class RecipeAdapter(private val recipeList: List<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.recipeImage)
        val recipeName: TextView = itemView.findViewById(R.id.recipeName)
        val recipeTime: TextView = itemView.findViewById(R.id.recipeTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val currentRecipe = recipeList[position]

        // Glide로 이미지 로드
        Glide.with(holder.itemView.context)
            .load(currentRecipe.imageUrl)
            .into(holder.recipeImage)

        holder.recipeName.text = currentRecipe.foodName
        holder.recipeTime.text = currentRecipe.cookingTime
    }

    override fun getItemCount() = recipeList.size
}
