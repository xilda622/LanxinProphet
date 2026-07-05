# Agent Memory System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local memory system that turns WeChat JSON into remembered future plans and visible agent context.

**Architecture:** Add pure Kotlin memory extraction and storage classes under `com.lanxin.prophet.memory`, then wire them into `MainActivity` with sample data. Use a local JSON file store on Android and an in-memory test store for deterministic unit tests.

**Tech Stack:** Android Kotlin, `org.json`, JUnit 4, Kotlin test.

---

### Task 1: Memory Model And Extractor

**Files:**
- Create: `app/src/main/java/com/lanxin/prophet/memory/MemoryItem.kt`
- Create: `app/src/main/java/com/lanxin/prophet/memory/WeChatMemoryExtractor.kt`
- Test: `app/src/test/kotlin/com/lanxin/prophet/memory/MemorySystemTest.kt`

- [ ] Write failing tests for extracting future commitments from WeChat JSON.
- [ ] Run `./gradlew testDebugUnitTest --tests com.lanxin.prophet.memory.MemorySystemTest`.
- [ ] Implement `MemoryItem` and `WeChatMemoryExtractor`.
- [ ] Run the same test and confirm it passes.

### Task 2: Layered Store

**Files:**
- Create: `app/src/main/java/com/lanxin/prophet/memory/LayeredMemoryStore.kt`
- Test: `app/src/test/kotlin/com/lanxin/prophet/memory/MemorySystemTest.kt`

- [ ] Write failing tests for path-based storage, deduplication, and L0/L1/L2 context output.
- [ ] Run the targeted unit test and confirm the expected failure.
- [ ] Implement in-memory and JSON-file-backed stores.
- [ ] Run the targeted unit test and confirm it passes.

### Task 3: App Wiring

**Files:**
- Create: `app/src/main/java/com/lanxin/prophet/memory/AgentMemorySystem.kt`
- Modify: `app/src/main/java/com/lanxin/prophet/MainActivity.kt`

- [ ] Wire extraction and storage behind `AgentMemorySystem`.
- [ ] Display sample memory context in `MainActivity`.
- [ ] Run unit tests and assemble the debug APK.
