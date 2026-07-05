package com.lanxin.prophet.util

object SupportedChatPackages {
    val ALL: Set<String> = setOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.tencent.mobileim"
    )

    fun contains(packageName: CharSequence?): Boolean {
        return packageName?.toString() in ALL
    }
}
