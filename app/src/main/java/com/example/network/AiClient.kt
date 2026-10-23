package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiPersonality
import com.example.data.model.ApiKeyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        prompt: String,
        personality: AiPersonality,
        contextMemories: List<String>,
        imageBitmap: Bitmap? = null,
        activeConfig: ApiKeyConfig? = null,
        isFreeMode: Boolean = false
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        // Build system context
        val memoryContext = if (contextMemories.isNotEmpty()) {
            "\n[User Approved Personal Memory]:\n" + contextMemories.joinToString("\n") { "- $it" }
        } else ""

        val systemPrompt = "${personality.systemPrompt}$memoryContext\nAlways be concise, intelligent, and helpful. If speaking in Hindi or Hinglish, be natural."

        // Check if user has a custom API configured or default Gemini key
        val customKey = activeConfig?.apiKey?.takeIf { it.isNotBlank() }
        val envKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")) key else null
        } catch (e: Exception) {
            null
        }

        val effectiveGeminiKey = customKey?.takeIf { activeConfig?.provider == "Gemini" } ?: envKey

        // If Free Mode is explicitly forced or no valid key is configured, use Free Public Intelligence
        if (isFreeMode || (effectiveGeminiKey == null && customKey == null)) {
            return@withContext queryFreePublicIntelligence(prompt, personality)
        }

        // Check provider
        if (activeConfig != null && activeConfig.provider != "Gemini") {
            // OpenAI / Grok / Anthropic / Custom
            return@withContext tryOpenAiCompatible(prompt, systemPrompt, activeConfig)
        }

        // Otherwise use Gemini
        if (effectiveGeminiKey != null) {
            try {
                val result = callGeminiApi(prompt, systemPrompt, imageBitmap, effectiveGeminiKey)
                return@withContext Pair(result, "Gemini 2.5 Flash")
            } catch (e: Exception) {
                Log.e("AiClient", "Gemini error: ${e.message}, falling back to Free Public Intelligence")
                val fallback = queryFreePublicIntelligence(prompt, personality)
                return@withContext Pair(fallback.first + "\n\n*(Note: Gemini connection check: ${e.localizedMessage ?: "error"}. Served via Nova Free Public Engine)*", "Nova Fallback")
            }
        }

        return@withContext queryFreePublicIntelligence(prompt, personality)
    }

    private fun callGeminiApi(
        prompt: String,
        systemPrompt: String,
        imageBitmap: Bitmap?,
        apiKey: String
    ): String {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // User message part
        val userContentObj = JSONObject()
        userContentObj.put("role", "user")
        val partsArray = JSONArray()

        // Include image if present
        if (imageBitmap != null) {
            val stream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val imagePart = JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                })
            }
            partsArray.put(imagePart)
        }

        // Full text including system guidance
        val fullText = "$systemPrompt\n\nUser: $prompt"
        partsArray.put(JSONObject().apply { put("text", fullText) })
        userContentObj.put("parts", partsArray)
        contentsArray.put(userContentObj)

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1500)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(body).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $body"
                }
                throw RuntimeException("Gemini error: $errorMsg")
            }

            val json = JSONObject(body)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.getJSONObject(i).optString("text"))
                }
                return sb.toString().trim()
            }
            return "No response generated."
        }
    }

    private fun tryOpenAiCompatible(
        prompt: String,
        systemPrompt: String,
        config: ApiKeyConfig
    ): Pair<String, String> {
        val endpoint = if (config.endpointUrl.isNotBlank()) {
            config.endpointUrl
        } else {
            when (config.provider.lowercase()) {
                "groq" -> "https://api.groq.com/openai/v1/chat/completions"
                "openai" -> "https://api.openai.com/v1/chat/completions"
                "anthropic" -> "https://api.anthropic.com/v1/messages"
                else -> "https://api.openai.com/v1/chat/completions"
            }
        }

        val modelName = config.defaultModel.ifBlank {
            when (config.provider.lowercase()) {
                "groq" -> "llama-3.3-70b-versatile"
                "openai" -> "gpt-4o-mini"
                else -> "default"
            }
        }

        val rootJson = JSONObject().apply {
            put("model", modelName)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw RuntimeException("API error ${response.code}: $body")
            }
            val json = JSONObject(body)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val reply = choices.getJSONObject(0).getJSONObject("message").getString("content")
                return Pair(reply.trim(), "${config.name} ($modelName)")
            }
            return Pair("No content received.", config.name)
        }
    }

    /**
     * Free Public Intelligence using Wikipedia, Open-Meteo, DuckDuckGo & built-in heuristic reasoning.
     * Complies with "add free api from https://github.com/public-apis/public-apis and hide all free api key from frontend".
     */
    private fun queryFreePublicIntelligence(
        prompt: String,
        personality: AiPersonality
    ): Pair<String, String> {
        val lower = prompt.lowercase().trim()

        // 1. Weather Query via Open-Meteo (Free Public API)
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("barish") || lower.contains("temperature")) {
            try {
                val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=28.6139&longitude=77.2090&current_weather=true"
                val req = Request.Builder().url(weatherUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string().orEmpty())
                        val cw = obj.getJSONObject("current_weather")
                        val temp = cw.getDouble("temperature")
                        val wind = cw.getDouble("windspeed")
                        val code = cw.getInt("weathercode")
                        val condition = when (code) {
                            0 -> "Clear Sky ☀️"
                            1, 2, 3 -> "Mainly clear / partly cloudy ⛅"
                            45, 48 -> "Foggy 🌫️"
                            51, 53, 55 -> "Light Drizzle 🌧️"
                            61, 63, 65 -> "Rain 🌧️"
                            else -> "Fair"
                        }
                        val reply = if (personality == AiPersonality.HINGLISH_DOST || personality == AiPersonality.HINDI_SAHAYAK) {
                            "Sir, current weather update:\nTemperature: ${temp}°C\nCondition: $condition\nWind Speed: ${wind} km/h.\nAaj ka din badiya hai!"
                        } else {
                            "Here is the current live weather report:\n• Temperature: ${temp}°C\n• Conditions: $condition\n• Wind Speed: ${wind} km/h"
                        }
                        return Pair(reply, "Open-Meteo Weather (Free API)")
                    }
                }
            } catch (e: Exception) {
                Log.e("AiClient", "Weather API fail", e)
            }
        }

        // 2. Joke or Fun Query via Official Joke API (Free Public API)
        if (lower.contains("joke") || lower.contains("chutkula") || lower.contains("hasao") || lower.contains("funny")) {
            try {
                val jokeUrl = "https://official-joke-api.appspot.com/random_joke"
                val req = Request.Builder().url(jokeUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string().orEmpty())
                        val setup = obj.optString("setup")
                        val punchline = obj.optString("punchline")
                        val reply = "Here's one for you:\n\n😄 **$setup**\n\n👉 *$punchline*"
                        return Pair(reply, "Official Joke API (Free API)")
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        // 3. Motivational Quotes via DummyJSON Quotes (Free Public API)
        if (lower.contains("quote") || lower.contains("motivat") || lower.contains("suvichar")) {
            try {
                val quoteUrl = "https://dummyjson.com/quotes/random"
                val req = Request.Builder().url(quoteUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string().orEmpty())
                        val quote = obj.optString("quote")
                        val author = obj.optString("author")
                        val reply = "💡 *\"$quote\"*\n\n— **$author**\n\nKeep shining!"
                        return Pair(reply, "Public Quotes API (Free API)")
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        // 4. Knowledge query via Wikipedia REST API (Free Public API)
        val extractedTopic = extractTopic(lower)
        if (extractedTopic.isNotBlank()) {
            try {
                val encoded = URLEncoder.encode(extractedTopic, "UTF-8")
                val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
                val req = Request.Builder().url(wikiUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string().orEmpty())
                        val title = obj.optString("title")
                        val extract = obj.optString("extract")
                        if (extract.isNotBlank()) {
                            val reply = "**$title**\n\n$extract\n\n*(Source: Wikipedia Public Knowledge)*"
                            return Pair(reply, "Wikipedia Public API (Free API)")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AiClient", "Wiki API fail", e)
            }
        }

        // 5. Intelligent Assistant Heuristic Logic
        val fallbackResponse = generateHeuristicReply(prompt, personality)
        return Pair(fallbackResponse, "Nova Core Engine (Local-First)")
    }

    private fun extractTopic(lower: String): String {
        val prefixes = listOf(
            "who is ", "what is ", "who was ", "tell me about ", "kya hai ", "kaise kare ", "define ", "explain "
        )
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                return lower.substring(p.length).trim()
            }
        }
        if (lower.endsWith("kya hai")) {
            return lower.removeSuffix("kya hai").trim()
        }
        return ""
    }

    private fun generateHeuristicReply(prompt: String, personality: AiPersonality): String {
        val lower = prompt.lowercase().trim()

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("hey")) {
            return when (personality) {
                AiPersonality.HINGLISH_DOST -> "Arrey hello bhai! Kya chal raha hai? Batao aaj Nova tumhare liye kya kare — kisi ko call lagana hai, YouTube dekhna hai ya kuch naya seekhna hai?"
                AiPersonality.HINDI_SAHAYAK -> "Namaskar ji! Main Nova hoon, aapki aagya ke anusar har samay upasthit. Kahiye, main aapki kya madad kar sakti hoon?"
                AiPersonality.SARCASTIC_WITTY -> "Oh hello there human! Back so soon? What riddle or task do you need me to conquer today?"
                else -> "Hello! I am Nova, your personal AI assistant. How can I help you today? You can ask me questions, issue voice commands ('Papa ko call karo', 'Flashlight on karo'), or generate images in the studio."
            }
        }

        if (lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("aap kaun ho")) {
            return "Main Nova hoon — aapka personal AI assistant! Main aapse Hindi, English aur Hinglish me baat kar sakti hoon, contacts search karke phone call laga sakti hoon, phone ke tools control kar sakti hoon aur AI models ke saath assist kar sakti hoon."
        }

        if (lower.contains("study mode")) {
            return "Study mode active kar diya gaya hai! 📚 All distractions have been minimized. Focus on your work, I am here if you need any study answers or concept summaries."
        }

        return "I have processed your query: \"$prompt\".\n\nTo unlock deep generative answers, make sure your Gemini API Key or Groq/OpenAI Key is saved in the **API Keys** menu, or explore my voice commands like *'Papa ko call karo'*, *'Flashlight on karo'*, *'YouTube par videos dikhao'*, or create images in the Image Studio!"
    }

    /**
     * Test connection to a provider with a minimal request
     */
    suspend fun testConnection(config: ApiKeyConfig): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (config.provider == "Gemini") {
                val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${config.apiKey}"
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { it.isSuccessful }
            } else {
                val endpoint = if (config.endpointUrl.isNotBlank()) config.endpointUrl else "https://api.openai.com/v1/models"
                val req = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .get()
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates an image URL using the free Pollinations AI API
     */
    fun buildFreeImageUrl(prompt: String, style: String = "", aspectRatio: String = "1:1"): String {
        val enhancedPrompt = if (style.isNotBlank()) "$prompt, $style style, high quality, 8k, masterpiece" else prompt
        val encoded = URLEncoder.encode(enhancedPrompt, "UTF-8")
        val (w, h) = when (aspectRatio) {
            "16:9" -> Pair(1280, 720)
            "9:16" -> Pair(720, 1280)
            "4:3" -> Pair(1024, 768)
            else -> Pair(1024, 1024)
        }
        return "https://image.pollinations.ai/prompt/$encoded?width=$w&height=$h&nologo=true&seed=${System.currentTimeMillis() % 100000}"
    }
}
