package com.example.martfia

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.LinearLayout

class ChooseImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_image)

        // 뒤로 가기 버튼 설정
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // 현재 액티비티 종료
        }

        // 재료 사진 버튼 클릭 이벤트
        val ingredientPhotoButton = findViewById<LinearLayout>(R.id.ingredientPhotoButton)
        ingredientPhotoButton.setOnClickListener {
            navigateToUploadScreen("food") // "food" 값 전달
        }

        // 온라인 영수증 버튼 클릭 이벤트
        val onlineReceiptButton = findViewById<LinearLayout>(R.id.onlineReceiptButton)
        onlineReceiptButton.setOnClickListener {
            navigateToUploadScreen("receipt") // "receipt" 값 전달
        }
    }

    private fun navigateToUploadScreen(uploadType: String) {
        val intent = Intent(this, UploadActivity::class.java)
        intent.putExtra("uploadType", uploadType) // 업로드 타입 전달
        startActivity(intent)
    }
}
