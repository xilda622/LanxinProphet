package com.lanxin.prophet.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemorySystemTest {
    private val now = 1_718_000_000_000L

    @Test
    fun `extracts future user commitments from wechat json`() {
        val json = """
            [
              {
                "chatId": "wx-trip",
                "sender": "朋友",
                "content": "明天上午10点在虹桥火车站见，去杭州开会",
                "timestamp": $now
              },
              {
                "chatId": "wx-noise",
                "sender": "朋友",
                "content": "昨天那个电影挺好看的",
                "timestamp": $now
              },
              {
                "chatId": "wx-dinner",
                "sender": "用户",
                "content": "下周五晚上和小李在国贸吃饭，帮我记一下",
                "timestamp": ${now + 1_000}
              }
            ]
        """.trimIndent()

        val memories = WeChatMemoryExtractor().extract(json)

        assertEquals(2, memories.size)
        assertEquals("travel", memories[0].category)
        assertTrue(memories[0].summary.contains("虹桥火车站"))
        assertTrue(memories[0].summary.contains("杭州开会"))
        assertEquals("meal", memories[1].category)
        assertTrue(memories[1].summary.contains("国贸吃饭"))
    }

    @Test
    fun `stores memories in layered filesystem context and deduplicates`() {
        val store = InMemoryLayeredMemoryStore()
        val memory = MemoryItem(
            id = "wx-trip-1718000000000",
            sourceChatId = "wx-trip",
            category = "travel",
            summary = "明天上午10点在虹桥火车站见，去杭州开会",
            evidence = "明天上午10点在虹桥火车站见，去杭州开会",
            createdAtMillis = now,
            dueText = "明天上午10点",
            path = "/memory/plans/travel/wx-trip-1718000000000.json",
            keywords = listOf("虹桥火车站", "杭州", "开会")
        )

        store.upsert(memory)
        store.upsert(memory.copy(summary = "重复：明天上午10点在虹桥火车站见，去杭州开会"))

        val context = store.buildContext(query = "明天行程")

        assertEquals(1, store.list().size)
        assertTrue(context.contains("L0 摘要"))
        assertTrue(context.contains("L1 待办记忆"))
        assertTrue(context.contains("L2 原始证据"))
        assertTrue(context.contains("/memory/plans/travel/wx-trip-1718000000000.json"))
        assertTrue(context.contains("虹桥火车站"))
    }
}
