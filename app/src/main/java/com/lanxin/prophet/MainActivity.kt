package com.lanxin.prophet

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.prophet.memory.AgentMemorySystem

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val memorySystem = AgentMemorySystem.fileBacked(this)
        memorySystem.ingestWechatJson(sampleWechatJson)

        val output = memorySystem.buildContext("明天行程")
        setContentView(TextView(this).apply {
            text = output
            textSize = 16f
            setPadding(32, 48, 32, 32)
        })
    }

    private val sampleWechatJson = """
        [
          {
            "chatId": "wx-trip",
            "sender": "朋友",
            "content": "明天上午10点在虹桥火车站见，去杭州开会",
            "timestamp": 1718000000000
          },
          {
            "chatId": "wx-dinner",
            "sender": "用户",
            "content": "下周五晚上和小李在国贸吃饭，帮我记一下",
            "timestamp": 1718000001000
          }
        ]
    """.trimIndent()
}
