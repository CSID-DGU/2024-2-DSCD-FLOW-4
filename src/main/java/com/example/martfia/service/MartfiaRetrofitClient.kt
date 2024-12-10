package com.example.martfia.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.security.SecureRandom
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.util.concurrent.TimeUnit

object MartfiaRetrofitClient {
    private const val BASE_URL = "BASE_URL"

    // OkHttpClient 설정 추가 (SSL 인증서 신뢰하도록 설정)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃 설정
        .readTimeout(30, TimeUnit.SECONDS) // 읽기 타임아웃 설정
        .writeTimeout(30, TimeUnit.SECONDS) // 쓰기 타임아웃 설정
        .sslSocketFactory(getSslSocketFactory(), getTrustManager()) // SSL 인증서 처리
        .hostnameVerifier { _, _ -> true } // 호스트명 검증 무시 (보안 위험 있음)
        .build()

    // SSL 인증서 무시를 위한 getSslSocketFactory
    private fun getSslSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }

    // TrustManager 객체를 반환
    private fun getTrustManager(): X509TrustManager {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        return trustAllCerts[0] as X509TrustManager
    }

    // Retrofit 인스턴스를 하나만 생성하여 모든 서비스에서 재사용하도록 설정
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // OkHttpClient 사용
            .build()
    }

    // 다양한 서비스 인터페이스 제공 함수
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
