package com.example.luna

import android.util.Log
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

sealed class GeminiResponse {
    data class TextResponse(val text: String) : GeminiResponse()
    data class CalendarEventResponse(
        val title: String, val startTime: Calendar, val endTime: Calendar,
        val description: String = "", val spokenResponse: String
    ) : GeminiResponse()
    data class AlarmResponse(
        val hour: Int, val minute: Int, val message: String = "", val spokenResponse: String
    ) : GeminiResponse()
    data class ErrorResponse(val error: String) : GeminiResponse()
}

class GeminiAssistant(private val apiKey: String) {

    private val systemPrompt = """
Sei Luna, un'assistente vocale intelligente e amichevole. Rispondi sempre in italiano in modo conciso e naturale, come se stessi parlando.

REGOLE IMPORTANTI:
1. Rispondi in modo breve e conversazionale (massimo 2-3 frasi).
2. Non usare mai formattazione markdown, emoji, asterischi o caratteri speciali. Le tue risposte verranno lette ad alta voce.
3. Se l'utente chiede di aggiungere un evento al calendario, rispondi SOLO con un JSON nel formato:
   {"type":"calendar","title":"titolo evento","date":"YYYY-MM-DD","startHour":HH,"startMinute":MM,"endHour":HH,"endMinute":MM,"description":"descrizione","spoken":"Ho aggiunto l'evento al calendario"}
4. Se l'utente chiede di impostare una sveglia, rispondi SOLO con un JSON nel formato:
   {"type":"alarm","hour":HH,"minute":MM,"message":"messaggio sveglia","spoken":"Ho impostato la sveglia alle HH:MM"}
5. Per gli eventi di calendario, usa la data corrente come riferimento. Oggi è ${SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALIAN).format(Date())}.
6. I giorni della settimana: lunedì, martedì, mercoledì, giovedì, venerdì, sabato, domenica.
7. Se non viene specificata un'ora di fine per un evento, aggiungi 1 ora all'ora di inizio.
8. Per le sveglie, se l'utente dice "domani mattina" usa le 7:00, "tra un'ora" calcola l'ora corrente + 1.
    """.trimIndent()

    suspend fun sendMessage(text: String): GeminiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val client = Client.builder().apiKey(apiKey).build()
                val fullPrompt = "$systemPrompt\n\nUtente: $text"
                val response: GenerateContentResponse = client.models.generateContent(
                    "gemini-2.5-flash", fullPrompt, null
                )
                val responseText = response.text()?.trim() ?: ""
                Log.d("GeminiAssistant", "Response: $responseText")
                parseResponse(responseText)
            } catch (e: Exception) {
                Log.e("GeminiAssistant", "Error: ${e.message}", e)
                GeminiResponse.ErrorResponse("Mi dispiace, si è verificato un errore: ${e.message}")
            }
        }
    }

    private fun parseResponse(responseText: String): GeminiResponse {
        if (responseText.contains("{") && responseText.contains("}")) {
            try {
                val jsonStr = responseText.substring(responseText.indexOf("{"), responseText.lastIndexOf("}") + 1)
                val json = JSONObject(jsonStr)
                when (json.optString("type")) {
                    "calendar" -> {
                        val dateParts = json.getString("date").split("-")
                        val startCal = Calendar.getInstance().apply {
                            set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt(),
                                json.getInt("startHour"), json.getInt("startMinute"), 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt(),
                                json.getInt("endHour"), json.getInt("endMinute"), 0)
                        }
                        return GeminiResponse.CalendarEventResponse(
                            json.getString("title"), startCal, endCal,
                            json.optString("description", ""),
                            json.optString("spoken", "Evento aggiunto al calendario")
                        )
                    }
                    "alarm" -> return GeminiResponse.AlarmResponse(
                        json.getInt("hour"), json.getInt("minute"),
                        json.optString("message", "Sveglia"),
                        json.optString("spoken", "Sveglia impostata")
                    )
                }
            } catch (e: Exception) {
                Log.w("GeminiAssistant", "JSON parse failed: ${e.message}")
            }
        }
        return GeminiResponse.TextResponse(responseText)
    }
}
