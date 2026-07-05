package com.lanxin.prophet.accessibility

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lanxin.prophet.model.BubbleSelection

class BubbleSelectionExtractor {
    fun extract(event: AccessibilityEvent): BubbleSelection? {
        val source = event.source ?: return null
        return try {
            val packageName = event.packageName?.toString()?.trim().orEmpty()
            if (packageName.isEmpty()) {
                return null
            }

            val bounds = Rect().also(source::getBoundsInScreen)
            if (bounds.isEmpty) {
                return null
            }

            val textHint = sequenceOf(
                collectText(source),
                event.text.joinToString(separator = "\n").trim(),
                event.contentDescription?.toString()?.trim().orEmpty()
            ).map(String::trim)
                .firstOrNull(String::isNotBlank)

            BubbleSelection(
                packageName = packageName,
                boundsInScreen = Rect(bounds),
                centerX = bounds.centerX(),
                centerY = bounds.centerY(),
                windowId = source.windowId,
                textHint = textHint,
                triggerEventType = event.eventType,
                capturedAtMillis = event.eventTime.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
        } finally {
            source.recycleCompat()
        }
    }

    private fun collectText(root: AccessibilityNodeInfo): String {
        val fragments = LinkedHashSet<String>()
        collectNodeText(
            node = root,
            fragments = fragments,
            depth = 0,
            visited = intArrayOf(0)
        )
        return fragments.joinToString(separator = "\n")
    }

    private fun collectNodeText(
        node: AccessibilityNodeInfo,
        fragments: MutableSet<String>,
        depth: Int,
        visited: IntArray
    ) {
        if (visited[0] >= MAX_VISITED_NODES || depth > MAX_TREE_DEPTH) {
            return
        }

        visited[0] += 1

        node.text?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(fragments::add)
        node.contentDescription?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(fragments::add)

        for (childIndex in 0 until node.childCount) {
            if (visited[0] >= MAX_VISITED_NODES) {
                return
            }

            val child = node.getChild(childIndex) ?: continue
            try {
                collectNodeText(
                    node = child,
                    fragments = fragments,
                    depth = depth + 1,
                    visited = visited
                )
            } finally {
                child.recycleCompat()
            }
        }
    }

    private companion object {
        const val MAX_VISITED_NODES = 32
        const val MAX_TREE_DEPTH = 6
    }
}

@Suppress("DEPRECATION")
private fun AccessibilityNodeInfo.recycleCompat() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        recycle()
    }
}
