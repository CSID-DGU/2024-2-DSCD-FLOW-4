package com.example.martfia.model.request

// 온라인 영수증 이미지 업로드 요청
data class ReceiptUploadRequest(
    val receipt_url: String // 영수증 이미지 URL
)
