package com.lanxin.prophet.memory

import android.content.Context

class AgentMemorySystem(
    private val extractor: WeChatMemoryExtractor,
    private val store: LayeredMemoryStore
) {
    fun ingestWechatJson(wechatJson: String): List<MemoryItem> {
        val memories = extractor.extract(wechatJson)
        memories.forEach(store::upsert)
        return memories
    }

    fun buildContext(query: String = ""): String = store.buildContext(query)

    companion object {
        fun fileBacked(context: Context): AgentMemorySystem =
            AgentMemorySystem(WeChatMemoryExtractor(), JsonFileLayeredMemoryStore(context))

        fun inMemory(): AgentMemorySystem =
            AgentMemorySystem(WeChatMemoryExtractor(), InMemoryLayeredMemoryStore())
    }
}
