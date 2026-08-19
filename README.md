# Java AI Launchpad — Trainer Evaluation Demo

A single Spring Boot + Spring AI app covering every point the coordinator listed:
simple chatbot → stateful memory (in-memory + JDBC, single & multi-user) →
web search tool → tool chaining → RAG with ChromaDB.

Domain: Employee Management System (EMS) — same domain as the rest of the
IBM courseware, so this doubles as a teaching example and not just a checkbox demo.

## 1. Prerequisites

- Java 17+
- Maven 3.9+
- An OpenAI API key
- Docker (to run ChromaDB locally)
- (Optional) A free Tavily API key for the web search tool — https://tavily.com

## 2. One-time setup

```bash
# Start ChromaDB locally
docker run -d --name chroma -p 8000:8000 chromadb/chroma

# Set your keys
export OPENAI_API_KEY=sk-...
export TAVILY_API_KEY=tvly-...   # optional — without it, the web search tool returns a stub message

# Build
mvn clean install
```

## 3. Run

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080` and, on boot, ingests
`src/main/resources/docs/leave-policy.md` into ChromaDB (watch the console
log for `[RAG] Ingested N chunks...`).

## 4. Demo script — run these in order during the evaluation call

**1. Simple chatbot**
```bash
curl "http://localhost:8080/api/chat/simple?message=What+can+you+help+me+with?"
```
Talk point: stateless, no memory, no tools — the baseline.

**2. Stateful conversation — in-memory**
```bash
curl "http://localhost:8080/api/chat/memory/inmemory?conversationId=demo-1&message=My+name+is+Vaman"
curl "http://localhost:8080/api/chat/memory/inmemory?conversationId=demo-1&message=What+is+my+name?"
```
Talk point: the model remembers within the process, but history is gone on restart.

**3. Stateful conversation — JDBC memory, single vs multi-user**
```bash
curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-1&message=My+name+is+Vaman"
curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-1&message=What+is+my+name?"

# Different conversationId = a different "user" with zero shared history
curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-2&message=What+is+my+name?"

# Proof point: read straight from HSQLDB, bypassing the model entirely
curl "http://localhost:8080/api/chat/memory/history?conversationId=user-1"
curl "http://localhost:8080/api/chat/memory/history?conversationId=user-2"
```
Talk point: `/history` hits the database directly via `JdbcChatMemoryRepository`, so
you can show `user-1`'s history containing "Vaman" while `user-2`'s is empty of
it — durable, per-conversation storage, not just the model "remembering."

**4. Web search tool**
```bash
curl "http://localhost:8080/api/chat/websearch?message=What+is+the+latest+stable+Spring+Boot+version?"
```
Talk point: this is *not* built into Spring AI — it's a plain `@Tool` method
calling an external search API (Tavily here; Bing/SerpAPI work identically).

**5. Tool chaining**
```bash
curl "http://localhost:8080/api/chat/toolchain?message=What+is+the+leave+balance+for+employee+E102?"
```
Talk point: the model has to resolve the employee via `getEmployee()` first,
then call `getLeaveBalance()` — a genuine two-hop chain in a single turn.
Show the DEBUG logs (`logging.level.org.springframework.ai=DEBUG`) so the
tool-call sequence is visible live, not just asserted.

**6. RAG over EMS HR policy (ChromaDB)**
```bash
curl "http://localhost:8080/api/rag/ask?question=How+many+days+of+sick+leave+do+I+get?"
curl "http://localhost:8080/api/rag/ask?question=Can+I+carry+forward+unused+leave?"
```
Talk point: answers come only from `leave-policy.md`'s embedded chunks in
ChromaDB — ask something *not* in the document to show it correctly says it
doesn't know, rather than hallucinating.

## 5. The "where it breaks" moment (recommended — see prior guidance)

Deliberately ask the tool-chain endpoint about a non-existent employee ID
(e.g. `E999`) and show how the model handles the tool returning "No employee
found" gracefully instead of inventing an answer. This is the senior-trainer
signal: showing you understand failure modes, not just the happy path.

## 6. Project layout

```
config/    ChatClient bean per demo scenario (simple / in-memory / jdbc / web search / tool chain)
tool/      @Tool-annotated classes: WebSearchTools, EmployeeTools
rag/       DocumentIngestionService — loads leave-policy.md into ChromaDB on boot
controller/ One thin REST controller per demo point
resources/docs/leave-policy.md  Sample EMS document for RAG
```

## 7. Known gaps closed while building this

- **H2 was originally used for JDBC chat memory storage and does not work** —
  Spring AI's JDBC chat memory module only ships schema scripts for
  PostgreSQL, MySQL/MariaDB, SQL Server, HSQLDB, and Oracle. H2 isn't one of
  them, so it fails at startup with "No schema scripts found". This project
  now uses **HSQLDB** instead, which is officially supported and just as easy
  to run locally as a file-based embedded DB — no separate install needed.
- Pin exact Spring AI artifact versions against the current Spring AI BOM
  (`1.0.0` used here) — Spring AI's package structure moved multiple times
  through its 1.0.0 milestones (e.g. `QuestionAnswerAdvisor` moved into
  `org.springframework.ai.chat.client.advisor.vectorstore` and its own
  `spring-ai-advisors-vector-store` dependency in the GA release).
- Swap HSQLDB for Postgres/MySQL if you want to demo a more
  production-realistic JDBC memory backend.
- Add a second sample document (e.g. IT policy) to make the RAG demo show
  retrieval *selecting the right document*, not just answering from the only
  one available.
