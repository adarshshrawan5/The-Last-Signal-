package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class MoshiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class MoshiContent(
    @Json(name = "parts") val parts: List<MoshiPart>
)

@JsonClass(generateAdapter = true)
data class MoshiGenerateContentRequest(
    @Json(name = "contents") val contents: List<MoshiContent>,
    @Json(name = "systemInstruction") val systemInstruction: MoshiContent? = null
)

@JsonClass(generateAdapter = true)
data class MoshiCandidate(
    @Json(name = "content") val content: MoshiContent
)

@JsonClass(generateAdapter = true)
data class MoshiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<MoshiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: MoshiGenerateContentRequest
    ): MoshiGenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun analyzeSignal(clueText: String, userCustomKey: String = ""): String {
        // Use user-provided key, or fallback to BuildConfig key
        val apiKey = userCustomKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "[STATIC CRACKLE...] WARNING: NO SIGNAL KEY SECURED. Connect a signal key in the Settings terminal to establish connection."
        }

        val systemPrompt = """
            You are the supernatural radio frequency or the mysterious, terrified previous lookout ranger from the psychological horror game 'The Last Signal'.
            The user is Ethan Carter, look-out at Watchtower 4 in Blackwood Forest.
            They are seeking to analyze a clue or audio log.
            Respond in a cryptic, creepy, atmospheric, and highly immersive psychological horror manner.
            Include radio interference indicators like [static...] or [distant scratching...].
            Keep your response short (under 3 sentences) and do not break character under any circumstances.
            Do not mention that you are an AI or language model.
        """.trimIndent()

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = "Ethan Carter's entry/clue: '$clueText'. Decrypt this frequency.")))
            ),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "[STATIC...] No readable signal returned."
        } catch (e: Exception) {
            "[INTERFERENCE...] Connection failed: ${e.localizedMessage ?: "Unknown signal error"}"
        }
    }
}
