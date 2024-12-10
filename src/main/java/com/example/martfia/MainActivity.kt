package com.example.martfia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.martfia.adapter.RecipeAdapter
import com.example.martfia.model.Recipe

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 레시피 더미 데이터
        val recipeList = listOf(
            Recipe("샐러드 파스타", "https://via.placeholder.com/150", "10 minutes"),
            Recipe("참치 볶음밥", "https://via.placeholder.com/150", "15 minutes"),
            Recipe("연어 포케", "https://via.placeholder.com/150", "20 minutes"),
            Recipe("김치찌개", "https://via.placeholder.com/150", "30 minutes"),
            Recipe("김밥", "https://via.placeholder.com/150", "25 minutes")
        )


        // RecyclerView
        val recyclerView_Recipe = findViewById<RecyclerView>(R.id.todayRecipeList)

        // LayoutManager - 한 줄 수평 스크롤
        val recipeLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerView_Recipe.layoutManager = recipeLayoutManager

        // Adapter
        recyclerView_Recipe.adapter = RecipeAdapter(recipeList)



        // 추천 버튼 클릭 시 ChooseImageActivity로 이동
        val recommendButton: Button = findViewById(R.id.recommendButton)
        recommendButton.setOnClickListener {
            val intent = Intent(this, ChooseImageActivity::class.java)
            startActivity(intent)
        }

        // 제철 재료 버튼 클릭 시 브라우저 열기
        val seasonalIngredientsButton: Button = findViewById(R.id.seasonalIngredientsButton)
        seasonalIngredientsButton.setOnClickListener {
            val url = "https://search.naver.com/search.naver?sm=tab_hty.top&where=nexearch&ssc=tab.nx.all&query=제철+재료&oquery=계절+재료&tqi=i0dTdlqo15VsssqvX4GssssstzC-022201"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        //  레이아웃이 시스템 영역과 겹치지 않도록 패딩 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}