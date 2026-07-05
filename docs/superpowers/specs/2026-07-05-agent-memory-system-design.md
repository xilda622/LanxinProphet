# Agent Memory System Design

## Goal

Add a runnable memory system for LanxinProphet that reads assumed WeChat chat JSON, remembers future user commitments, and outputs concise context for the agent.

## Architecture

The system mirrors OpenViking's context database ideas in a mobile-friendly form. It uses filesystem-style paths, layered context output, and recursive retrieval semantics, but stores everything locally as JSON-compatible Kotlin data instead of requiring a server, vector database, or model calls.

## Components

- `WeChatMemoryExtractor`: parses WeChat JSON messages and extracts future-looking commitments with simple deterministic rules.
- `LayeredMemoryStore`: stores memory items under paths like `/memory/plans/travel/<id>.json`, deduplicates by source and evidence, and returns matching memories.
- `AgentMemorySystem`: orchestrates extraction, storage, and context generation.
- `MainActivity`: runs a sample JSON input and displays the generated memory context so the app has visible output.

## Data Flow

1. WeChat reader writes chat records to JSON.
2. `AgentMemorySystem.ingestWechatJson()` parses messages and upserts extracted `MemoryItem` entries.
3. The store keeps L1 task entries with L2 evidence and produces L0 summary text on demand.
4. The agent calls `buildContext(query)` before planning.

## Scope

This implementation intentionally avoids silent WeChat access code, permissions, background services, and cloud sync. The user stated the chat content can be assumed to exist in JSON, so this change focuses only on memory extraction, persistence shape, and output.

## Testing

Unit tests cover extraction, layered context rendering, and deduplication. Build verification uses Gradle unit tests and debug APK assembly.
