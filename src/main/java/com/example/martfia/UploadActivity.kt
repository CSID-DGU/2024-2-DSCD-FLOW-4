package com.example.martfia

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.martfia.service.MartfiaRetrofitClient
import com.example.martfia.service.IngredientRecognitionService
import com.example.martfia.model.request.ImageUploadRequest
import com.example.martfia.model.request.ReceiptUploadRequest
import com.example.martfia.model.response.ImageUploadResponse
import com.example.martfia.model.Ingredient
import com.example.martfia.model.response.ImageUploadOnlyResponse
import com.example.martfia.model.response.IngredientsResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream

class UploadActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
        const val REQUEST_PERMISSIONS = 3
    }

    private var selectedImageUri: Uri? = null
    private var uploadType: String? = null // "food" 또는 "receipt"
    private val ingredientService: IngredientRecognitionService by lazy {
        MartfiaRetrofitClient.createService(IngredientRecognitionService::class.java)
    }

    private var tempImageFile: File? = null
    private var uploadedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload)

        uploadType = intent.getStringExtra("uploadType")
        Log.d("UploadActivity", "Upload type: $uploadType")

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val uploadContainer = findViewById<ImageView>(R.id.uploadContainer)
        uploadContainer.setOnClickListener {
            checkPermissions()
        }

        val recognizeButton = findViewById<Button>(R.id.recognizeButton)
        recognizeButton.setOnClickListener {
            if (selectedImageUri != null) {
                recognizeIngredients() // API 호출
            } else {
                Toast.makeText(this, "먼저 이미지를 업로드하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA // 카메라 권한만 요청
        )

        val ungrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isEmpty()) {
            Log.d("UploadActivity", "All permissions granted.")
            showImagePickerDialog()
        } else {
            Log.d("UploadActivity", "Requesting permissions: $ungrantedPermissions")
            ActivityCompat.requestPermissions(this, ungrantedPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }


    private fun showImagePickerDialog() {
        val options = arrayOf("사진 찍기", "앨범에서 선택")
        android.app.AlertDialog.Builder(this)
            .setTitle("사진 업로드")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        tempImageFile = File(getExternalFilesDir(null), "temp_image.jpg")
        val imageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", tempImageFile!!)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            selectedImageUri = imageUri
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "카메라를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    Log.d("UploadActivity", "Captured image URI: $selectedImageUri")
                    findViewById<ImageView>(R.id.uploadContainer).setImageURI(selectedImageUri)
                    selectedImageUri?.let { uri ->
                        val file = getFileFromUri(uri)
                        if (file != null) {
                            uploadImage(file) { imageUrl ->
                                Log.d("UploadActivity", "Image uploaded successfully: $imageUrl")
                                Toast.makeText(this, "이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "이미지 파일 변환 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    selectedImageUri = data?.data
                    Log.d("UploadActivity", "Picked image URI: $selectedImageUri")
                    findViewById<ImageView>(R.id.uploadContainer).setImageURI(selectedImageUri)
                    selectedImageUri?.let { uri ->
                        val file = getFileFromUri(uri)
                        if (file != null) {
                            uploadImage(file) { imageUrl ->
                                Log.d("UploadActivity", "Image uploaded successfully: $imageUrl")
                                Toast.makeText(this, "이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "이미지 파일 변환 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun uploadImage(file: File, onSuccess: (String) -> Unit) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("이미지 업로드 중...")
            setCancelable(false)
            show()
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestFile)

        ingredientService.uploadImageFile(multipartBody).enqueue(object : Callback<ImageUploadOnlyResponse> {
            override fun onResponse(call: Call<ImageUploadOnlyResponse>, response: Response<ImageUploadOnlyResponse>) {
                progressDialog.dismiss() // 로딩 상태 종료
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.image_url != null) {
                        uploadedImageUrl = body.image_url
                        Log.d("UploadActivity", "Uploaded Image URL: $uploadedImageUrl")
                        onSuccess(uploadedImageUrl!!)
                    } else {
                        Log.e("UploadActivity", "Response body or image_url is null")
                        Toast.makeText(this@UploadActivity, "이미지 업로드 실패: 응답 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UploadActivity", "Image upload failed: $errorBody")
                    Toast.makeText(this@UploadActivity, "이미지 업로드 실패: $errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ImageUploadOnlyResponse>, t: Throwable) {
                progressDialog.dismiss() // 로딩 상태 종료
                Log.e("UploadActivity", "Image upload failed", t)
                Toast.makeText(this@UploadActivity, "이미지 업로드 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun getFileFromUri(uri: Uri): File? {
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val file = File(cacheDir, "temp_image.jpg") // 캐시 디렉터리에 임시 파일 생성
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return file
    }


    private fun recognizeIngredients() {
        if (uploadedImageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "이미지를 업로드하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        when (uploadType) {
            "food" -> processFoodImage(uploadedImageUrl!!)
            "receipt" -> processReceiptImage(uploadedImageUrl!!)
            else -> Toast.makeText(this, "잘못된 업로드 유형입니다.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun processFoodImage(imageUrl: String) {
        val request = ImageUploadRequest(image_url = imageUrl)
        ingredientService.uploadImage(request)
            .enqueue(object : Callback<ImageUploadResponse> {
                override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                    if (response.isSuccessful) {
                        fetchIngredients() // 재료 데이터 요청
                    } else {
                        Toast.makeText(this@UploadActivity, "재료 이미지 처리 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                    Toast.makeText(this@UploadActivity, "서버 통신 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun processReceiptImage(imageUrl: String) {
        val request = ReceiptUploadRequest(receipt_url = imageUrl)
        ingredientService.uploadReceipt(request)
            .enqueue(object : Callback<ImageUploadResponse> {
                override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                    if (response.isSuccessful) {
                        fetchIngredients() // 재료 데이터 요청
                    } else {
                        Toast.makeText(this@UploadActivity, "영수증 이미지 처리 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                    Toast.makeText(this@UploadActivity, "서버 통신 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchIngredients() {
        ingredientService.getIngredients().enqueue(object : Callback<IngredientsResponse> {
            override fun onResponse(call: Call<IngredientsResponse>, response: Response<IngredientsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Map 데이터를 Ingredient 리스트로 변환
                    val ingredients = response.body()?.images?.map { (name, url) ->
                        Ingredient(image_url = url, name = name)
                    } ?: emptyList()

                    // Intent로 데이터 전달
                    val intent = Intent(this@UploadActivity, CheckIngredientActivity::class.java).apply {
                        putParcelableArrayListExtra("saved_ingredients", ArrayList(ingredients)) // ArrayList로 변환
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@UploadActivity, "재료 데이터를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<IngredientsResponse>, t: Throwable) {
                Toast.makeText(this@UploadActivity, "재료 데이터 요청 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun handleApiResponse(response: Response<ImageUploadResponse>) {
        Log.d("UploadActivity", "API Response Code: ${response.code()}")
        Log.d("UploadActivity", "API Response Headers: ${response.headers()}")
        Log.d("UploadActivity", "API Response Body: ${response.body()}")

        if (response.isSuccessful && response.body() != null) {
            val savedIngredients = response.body()!!.saved_ingredients.map {
                Ingredient(
                    image_url = it.image_url,
                    name = it.name
                )
            }
            Log.d("UploadActivity", "Recognized Ingredients: $savedIngredients")
            val intent = Intent(this, CheckIngredientActivity::class.java).apply {
                putParcelableArrayListExtra("saved_ingredients", ArrayList(savedIngredients))
            }
            startActivity(intent)
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e("UploadActivity", "Error Response Body: $errorBody")
            Toast.makeText(this, "재료 인식에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun handleApiFailure(t: Throwable) {
        Log.e("UploadActivity", "Server communication failed", t)
        if (t.message != null) {
            Log.e("UploadActivity", "Failure Message: ${t.message}")
        }
        Toast.makeText(this, "서버와 통신에 실패했습니다.", Toast.LENGTH_SHORT).show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val allPermissionsGranted =
                grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                Log.d("UploadActivity", "All permissions granted.")
                showImagePickerDialog()
            } else {
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                        Log.e("UploadActivity", "Denied permission: $permission")
                    }
                }
                Toast.makeText(this, "카메라 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
