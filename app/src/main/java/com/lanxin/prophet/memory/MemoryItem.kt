package com.lanxin.prophet.memory

import org.json.JSONArray
import org.json.JSONObject

data class MemoryItem(
    val id: String,
    val sourceChatId: String,
    val category: String,
    val summary: String,
    val evidence: String,
    val createdAtMillis: Long,
    val dueText: String,
    val path: String,
    val keywords: List<String>
) {
    val dedupKey: String
        get() = "${sourceChatId}:${evidence.trim()}"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("sourceChatId", sourceChatId)
        .put("category", category)
        .put("summary", summary)
        .put("evidence", evidence)
        .put("createdAtMillis", createdAtMillis)
        .put("dueText", dueText)
        .put("path", path)
        .put("keywords", JSONArray(keywords))

    companion object {
        fun fromJson(json: JSONObject): MemoryItem {
            val keywordArray = json.optJSONArray("keywords") ?: JSONArray()
            return MemoryItem(
                id = json.getString("id"),
                sourceChatId = json.getString("sourceChatId"),
                category = json.getString("category"),
                summary = json.getString("summary"),
                evidence = json.getString("evidence"),
                createdAtMillis = json.optLong("createdAtMillis", 0L),
                dueText = json.optString("dueText"),
                path = json.getString("path"),
                keywords = List(keywordArray.length()) { index -> keywordArray.getString(index) }
            )
        }
    }
}
