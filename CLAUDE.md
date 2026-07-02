# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build / Run / Test

```bash
# Compile all modules (run from repo root)
mvn clean compile -DskipTests

# Compile + run tests
mvn clean test

# Run a single test class
mvn test -pl bootstrap -Dtest=UserServiceTest

# Package the bootable JAR (output in bootstrap/target/)
mvn clean package -DskipTests -pl bootstrap

# Start the application (bootstrap module is the main entry point)
mvn spring-boot:run -pl bootstrap

# Frontend (React app in frontend/, NOT the old Vue scaffold in console/)
cd frontend && npm install && npm run dev
```

- **Main class**: `com.caobolun.bootstrap.BootstrapApplication` (scans `com.caobolun` base package, enables scheduling, maps MyBatis mappers across 4 packages)
- **Server**: port `9000`, context-path `/api/ragent`
- **Database**: PostgreSQL with pgvector extension, schema `ragent`
- **Java**: 17, **Spring Boot**: 3.5.7, **MyBatis-Plus**: 3.5.14

## Project overview

Ragent is an enterprise Agentic RAG platform — document ingestion → knowledge retrieval → intelligent Q&A with streaming SSE output.

| Module | Role |
|--------|------|
| `framework/` | Cross-cutting infrastructure (exceptions, conventions, web utils, idempotency, trace context) |
| `infra-ai/` | AI model abstraction (Chat / Embedding / Rerank clients, OpenAI-style SSE parsing, model routing) |
| `bootstrap/` | Business logic + Spring Boot entry point — all controllers, services, entities, the 8-stage RAG pipeline |
| `mcp-server/` | Standalone MCP tool server process (Weather/Ticket/Sales executors) |
| `frontend/` | React 18 + TypeScript + Vite 5 + Tailwind + shadcn/ui (replacing the old `console/` Vue scaffold) |

**Source project reference (the master copy)**: `E:\JavaProject\ragent` — when implementing a class, check whether it exists there first and replicate its logic.

## Critical MyBatis-Plus pattern: `@TableLogic`

**Every entity with a `deleted` field annotated `@TableLogic` gets automatic logical-delete behavior:**
- All `SELECT` queries auto-append `WHERE deleted = 0` (not-deleted value)
- All `DELETE` operations become `UPDATE SET deleted = 1` (deleted value)

**Never manually add `.eq(Entity::getDeleted, 0)` to query wrappers** — it produces duplicate `deleted = 0` conditions in the SQL. The annotation handles this transparently.

## Key architecture: the 8-stage RAG pipeline

Entry point: `GET /rag/v3/chat` → `RAGChatController` → `RAGChatServiceImpl` → `StreamChatPipeline`

```
Pipeline stages in StreamChatPipeline.execute():
  ① loadMemory       — Load conversation history + summary (DB read, parallel fetch)
  ② rewriteQuery     — Rewrite question + split into sub-questions (LLM call)
  ③ resolveIntents   — Classify each sub-question against intent tree (LLM call, parallel)
  ④ handleGuidance   — [SHORT-CIRCUIT] If ambiguous intent, push clarification prompt to user
  ⑤ handleSystemOnly — [SHORT-CIRCUIT] If pure system chat (e.g. "hello"), reply directly
  ⑥ retrieve         — Multi-channel KB search (pgvector/Milvus) + MCP tool calls (parallel)
  ⑦ handleEmptyRetrieval — [SHORT-CIRCUIT] If no results, push "no documents found"
  ⑧ streamRagResponse — Assemble prompt + stream LLM response via SSE
```

**Concurrency**: Stages that fan out (intent classify, retrieval, MCP) use dedicated `Executor` beans from the Spring context — see `bootstrap` thread pool configuration.

**Task cancellation**: `StreamTaskManager` uses Redis Pub/Sub (`ragent:stream:cancel`) to cancel running streams across cluster nodes.

**Rate limiting**: `ChatQueueLimiter` → `FairDistributedRateLimiter` (Redis ZSET + Lua atomic operations).

## Package layout conventions

- `bootstrap.core.*` — Domain service interfaces + implementations for the RAG pipeline stages (memory, rewrite, intent, guidance, retrieve, prompt, mcp)
- `bootstrap.rag.*` — Controllers, DTOs, entities, mappers, handlers, config for the RAG/chat subsystem
- `bootstrap.user.*` — User management (separate from RAG)
- `bootstrap.konwledge.*` — Knowledge base & document management (note: the package name has the typo `konwledge` from the source project — keep it)

## Current implementation status

The codebase is being replicated from `E:\JavaProject\ragent`. Many service implementations already exist. The critical gaps:

- **`RoutingLLMService`** (`infra-ai`): `chat()` returns `""`, `streamChat()` returns `null` — the LLM routing/multi-provider dispatch is not wired up. `SiliconFlowChatClient` (extends `AbstractOpenAIStyleChatClient`) exists and is functional.
- **`MultiQuestionRewriteService.rewrite()`** returns `""` — query rewriting not implemented yet.
- **Password hashing**: `UserServiceImpl` stores passwords in plaintext — needs BCrypt.
- **SQL schema**: No DDL scripts exist yet — tables must be created manually or from the source project.

## Bug reference

- **BUG-4 (open)**: `AbstractOpenAIStyleEmbeddingClient.doEmbed()` missing return statement, `embedBatch()` incomplete.
- **BUG-5 (open)**: Missing classes — `SourceType` enum, `ConversationMessageBO`, `ConversationSummaryBO`, `ConversationMessageOrder`, `ConversationCreateBO`.
- **BUG-6 (open)**: Password stored as plaintext in `AuthServiceImpl` and `UserServiceImpl`.

Previously resolved (for context): BUG-1 (AbstractException field swap), BUG-2 (SpEL old package name), BUG-3 (missing semicolon in StreamTaskManager).

## How to handle progress queries

When the user asks "进度", "下一步", "规划", "复刻到哪了", or similar — consult the full replication roadmap in memory (`[[project-replication-roadmap]]`). When the user says "继续" or "开始下一步", pick up from the next incomplete step in that roadmap. The source project at `E:\JavaProject\ragent` is the reference implementation — replicate its logic, adapting package names from `com.nageoffer.ai.ragent` → `com.caobolun`.
