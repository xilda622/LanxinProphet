package com.lanxin.prophet.memory

import org.json.JSONArray

class WeChatMemoryExtractor {
    fun extract(wechatJson: String): List<MemoryItem> {
        if (wechatJson.isBlank()) return emptyList()

        val messages = JSONArray(wechatJson)
        val memories = mutableListOf<MemoryItem>()
        for (index in 0 until messages.length()) {
            val message = messages.getJSONObject(index)
            val content = message.optString("content").trim()
            if (!looksLikeFutureCommitment(content)) continue

            val chatId = message.optString("chatId", "wechat")
            val timestamp = message.optLong("timestamp", System.currentTimeMillis())
            val category = categorize(content)
            val id = "${sanitize(chatId)}-$timestamp"
            memories += MemoryItem(
                id = id,
                sourceChatId = chatId,
                category = category,
                summary = summarize(content),
                evidence = content,
                createdAtMillis = timestamp,
                dueText = extractDueText(content),
                path = "/memory/plans/$category/$id.json",
                keywords = extractKeywords(content, category)
            )
        }
        return memories.distinctBy { it.dedupKey }
    }

    private fun looksLikeFutureCommitment(content: String): Boolean {
        if (content.isBlank()) return false
        val pastSignals = listOf("昨天", "前天", "上周", "已经", "刚才")
        if (pastSignals.any { content.contains(it) }) return false

        val futureSignals = listOf("明天", "后天", "今晚", "明晚", "下周", "周一", "周二", "周三", "周四", "周五", "周六", "周日", "星期", "下个月")
        val actionSignals = listOf("去", "见", "开会", "吃饭", "聚餐", "出差", "面试", "预约", "航班", "高铁", "火车站", "机场", "酒店", "记一下", "提醒")
        return futureSignals.any { content.contains(it) } && actionSignals.any { content.contains(it) }
    }

    private fun categorize(content: String): String = when {
        listOf("吃饭", "聚餐", "晚饭", "午饭", "餐厅").any { content.contains(it) } -> "meal"
        listOf("火车站", "机场", "航班", "高铁", "酒店", "出差", "去").any { content.contains(it) } -> "travel"
        listOf("开会", "会议", "面试").any { content.contains(it) } -> "work"
        else -> "todo"
    }

    private fun summarize(content: String): String =
        content.replace(Regex("\\s+"), " ").trim().take(120)

    private fun extractDueText(content: String): String {
        val duePattern = Regex("(明天|后天|今晚|明晚|下周[一二三四五六日天]?|周[一二三四五六日天]|星期[一二三四五六日天]|下个月)[^，。,.、；;]*")
        return duePattern.find(content)?.value?.trim() ?: ""
    }

    private fun extractKeywords(content: String, category: String): List<String> {
        val dictionary = listOf(
            "虹桥火车站", "火车站", "机场", "杭州", "国贸", "开会", "吃饭", "酒店",
            "高铁", "航班", "面试", "出差"
        )
        val words = dictionary.filter { content.contains(it) }.toMutableList()
        words += category
        return words.distinct()
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "-").trim('-').ifBlank { "wechat" }
}
