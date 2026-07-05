package org.example

import com.lanxin.prophet.util.SupportedChatPackages
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTest {
    @Test
    fun testEnvironmentLoads() {
        assertTrue(true)
    }

    @Test
    fun supportedTargetsIncludeWeChatAndQq() {
        assertTrue(SupportedChatPackages.contains("com.tencent.mm"))
        assertTrue(SupportedChatPackages.contains("com.tencent.mobileqq"))
    }
}
