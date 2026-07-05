package com.lanxin.prophet.memory

import android.content.Context
import org.json.JSONObject
import java.io.File

interface LayeredMemoryStore {
    fun upsert(memory: MemoryItem)
    fun list(): List<MemoryItem>
    fun buildContext(query: String = ""): String
}

class InMemoryLayeredMemoryStore : LayeredMemoryStore {
    private val memories = linkedMapOf<String, MemoryItem>()

    override fun upsert(memory: MemoryItem) {
        val existingKey = memories.entries.firstOrNull { it.value.dedupKey == memory.dedupKey }?.key
        memories[existingKey ?: memory.id] = memory
    }

    override fun list(): List<MemoryItem> = memories.values.sortedBy { it.createdAtMillis }

    override fun buildContext(query: String): String = renderLayeredContext(selectRelevant(list(), query))
}

class JsonFileLayeredMemoryStore(context: Context) : LayeredMemoryStore {
    private val rootDir = File(context.filesDir, "agent-memory")

    override fun upsert(memory: MemoryItem) {
        rootDir.mkdirs()
        val duplicate = list().firstOrNull { it.dedupKey == memory.dedupKey }
        val target = fileFor(duplicate ?: memory)
        target.parentFile?.mkdirs()
        target.writeText(memory.toJson().toString(2))
    }

    override fun list(): List<MemoryItem> {
        if (!rootDir.exists()) return emptyList()
        return rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { file -> runCatching { MemoryItem.fromJson(JSONObject(file.readText())) }.getOrNull() }
            .sortedBy { it.createdAtMillis }
            .toList()
    }

    override fun buildContext(query: String): String = renderLayeredContext(selectRelevant(list(), query))

    private fun fileFor(memory: MemoryItem): File =
        File(rootDir, "${memory.category}/${memory.id}.json")
}

internal fun selectRelevant(memories: List<MemoryItem>, query: String): List<MemoryItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return memories
    return memories.filter { memory ->
        memory.summary.contains(normalizedQuery) ||
            memory.dueText.contains(normalizedQuery) ||
            normalizedQuery.any { char -> char.toString() in memory.summary } ||
            memory.keywords.any { keyword -> normalizedQuery.contains(keyword) || memory.summary.contains(keyword) }
    }.ifEmpty { memories }
}

internal fun renderLayeredContext(memories: List<MemoryItem>): String {
    if (memories.isEmpty()) {
        return "L0 摘要\n暂无用户未来事项记忆。"
    }

    val l0 = "L0 摘要\n用户有 ${memories.size} 条未来事项记忆：" +
        memories.joinToString("；") { it.summary }
    val l1 = buildString {
        appendLine("L1 待办记忆")
        memories.forEachIndexed { index, memory ->
            appendLine("${index + 1}. [${memory.category}] ${memory.dueText.ifBlank { "时间待确认" }} - ${memory.summary}")
            appendLine("   path: ${memory.path}")
        }
    }.trimEnd()
    val l2 = buildString {
        appendLine("L2 原始证据")
        memories.forEachIndexed { index, memory ->
            appendLine("${index + 1}. ${memory.evidence} (source=${memory.sourceChatId})")
        }
    }.trimEnd()

    return listOf(l0, l1, l2).joinToString("\n\n")
}
