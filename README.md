# Java AI Launchpad — EMS Spring AI Demo

One Spring Boot app, six Spring AI moves, all wired to a tiny fictional Employee Management
System (EMS):

1. **Simple chatbot** — stateless, no memory
2. **In-memory conversation memory** — remembers you, forgets on restart
3. **JDBC-backed conversation memory** — remembers you *forever* (until you delete the DB file)
4. **Web search tool calling** — model decides on its own when to search
5. **Tool chaining** — model calls more than one tool in a single turn
6. **RAG** — answers grounded in an actual policy doc, not vibes

Runs two ways: **free + local with Ollama**, or **with a real OpenAI key**. Same code, zero code
changes between them — just env vars. Ollama first, since it's free and you'll be chatting with
it in under 10 minutes.

All instructions below are **Windows + PowerShell**.

---

## 0. Prerequisites

- **Java 17+** — `java -version` to check. Missing it? [adoptium.net](https://adoptium.net)
- **Maven 3.9+** — `mvn -version` to check. Missing it? [maven.apache.org](https://maven.apache.org/download.cgi), unzip, add `bin` to PATH
- **Git for Windows** — [git-scm.com](https://git-scm.com/download/win), gives you `git` in PowerShell
- A free [Serper.dev](https://serper.dev) key (2,500 free queries, no card) — powers the web
  search tool (#4, #5). App runs fine without it, search just returns a placeholder.

### Clone it

```powershell
git clone <this-repo-url>
cd <repo-folder-name>
```

### The one PowerShell gotcha you need to know

`curl` in PowerShell is secretly aliased to `Invoke-WebRequest` — it doesn't understand `-G` or
`--data-urlencode`. Every curl command below uses `curl.exe` instead, which forces PowerShell to
use the real curl.exe that ships with Windows 11. Just copy-paste as written and you're fine.

---

## 1. Quick start with Ollama (free, local, zero API keys)

### 1.1 Install Ollama + pull two models

```powershell
# Download & run the installer: https://ollama.com/download/windows
ollama --version   # sanity check
```

You need **two** models — chat + embeddings (RAG needs both; Ollama has no default embedding
model the way OpenAI does):

```powershell
ollama pull llama3.2
ollama pull nomic-embed-text
```

Ollama's installer runs it as a background service automatically (llama icon in your system
tray). If you ever need to start it manually: `ollama serve`. If that throws
`address already in use` — good news, it's already running.

Sanity check:
```powershell
curl.exe http://localhost:11434/v1/models
```

### 1.2 Set your env vars

```powershell
$env:OPENAI_BASE_URL="http://localhost:11434"
$env:OPENAI_API_KEY="ollama"
$env:OPENAI_CHAT_MODEL="llama3.2"
$env:OPENAI_EMBEDDING_MODEL="nomic-embed-text"
$env:SERPER_API_KEY="your-serper-key-here"
```
These only last for the current terminal session — set them again if you close and reopen.

### 1.3 Run it

```powershell
mvn spring-boot:run
```

First run downloads the internet (Maven deps). After that it's fast. App's up on
`http://localhost:8080` — jump to [section 4](#4-the-6-features-with-examples) or
[section 5](#5-swagger-ui-try-it-in-the-browser).

> Small local models like `llama3.2` are noticeably flakier at tool calling than GPT-4o-mini —
> see [Troubleshooting](#6-troubleshooting). Not a bug here, just what a 3B model locally looks like.

---

## 2. Switching to OpenAI (real key, zero code changes)

Same four vars, different values:

```powershell
$env:OPENAI_BASE_URL="https://api.openai.com"
$env:OPENAI_API_KEY="sk-...your real key..."
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"
$env:OPENAI_EMBEDDING_MODEL="text-embedding-3-small"
$env:SERPER_API_KEY="your-serper-key-here"

mvn spring-boot:run
```

Same jar, same endpoints. Just a smarter, paid model behind it — this is what you want before
anything that actually matters.

---

## 3. What's actually happening under the hood

```yaml
spring:
  ai:
    openai:
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL:gpt-4o-mini}
      embedding:
        options:
          model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
```

Ollama ships an OpenAI-compatible API, so Spring AI's regular OpenAI client talks to it fine —
you're just handing it a different `base-url`. That's the entire trick. No Ollama-specific
starter, no provider branching anywhere in the code.

---

## 4. The 6 features, with examples

curl examples below, or skip to [Swagger UI](#5-swagger-ui-try-it-in-the-browser) to click
instead of type.

### 1️⃣ Simple chatbot — no memory
```powershell
curl.exe -G localhost:8080/api/chat/simple --data-urlencode "message=What is dependency injection?"
```

### 2️⃣ In-memory conversation — remembers you until restart
```powershell
curl.exe -G localhost:8080/api/chat/memory/inmemory `
  --data-urlencode "conversationId=sonu" `
  --data-urlencode "message=My name is Sonu."
```
```powershell
curl.exe -G localhost:8080/api/chat/memory/inmemory `
  --data-urlencode "conversationId=sonu" `
  --data-urlencode "message=What's my name?"
# -> should say Sonu
```
Different `conversationId` = totally isolated conversation. That's single- and multi-user support
from the same endpoint, for free.

### 3️⃣ JDBC-backed conversation — survives restarts
```powershell
curl.exe -G localhost:8080/api/chat/memory/jdbc `
  --data-urlencode "conversationId=monu" `
  --data-urlencode "message=I work in the Sales department."
```
Restart the app, hit it again with the same `conversationId` — it still remembers, because it's
reading/writing an actual HSQLDB table instead of a JVM-memory map.

Proof-of-persistence — reads raw stored history straight from the DB:
```powershell
curl.exe -G localhost:8080/api/chat/memory/history --data-urlencode "conversationId=monu"
```

### 4️⃣ Web search tool
```powershell
curl.exe -G localhost:8080/api/chat/websearch `
  --data-urlencode "message=What is the latest stable Spring Boot version?"
```
Model decides on its own whether it needs to search — nothing routed manually.

### 5️⃣ Tool chaining
```powershell
curl.exe -G localhost:8080/api/chat/toolchain `
  --data-urlencode "message=Look up employee E101 and tell me their leave balance, then search the web for how many annual leave days are typical in India."
```
Forces the model to call **two different tools** (`getEmployee`/`getLeaveBalance` and
`searchWeb`) in one turn before it can answer.

### 6️⃣ RAG — grounded in `leave-policy.md`
```powershell
curl.exe -G localhost:8080/api/rag/ask `
  --data-urlencode "question=How many days of sick leave do I get?"
```
Answer traces back to `src/main/resources/docs/leave-policy.md`, not whatever the model happened
to memorize.

---

## 5. Swagger UI — try it in the browser

No curl needed. App running? Go to:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Raw OpenAPI spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Every endpoint above shows up automatically, numbered in order, with a "Try it out" button that
fires a real request. This is genuinely how most Spring Boot teams hand off an API — not a demo trick.

---

## 6. Troubleshooting

Real errors we actually hit building this, not hypotheticals.

**HSQLDB instead of H2** — Spring AI's JDBC chat-memory module only ships schema scripts for
Postgres/MySQL/SQL Server/HSQLDB/Oracle. H2 isn't on that list, and throws
`No schema scripts found` at startup. HSQLDB is, so that's what this project uses.

**`SimpleVectorStore` instead of ChromaDB** — in-memory, ships with Spring AI core, zero extra
process to run. Trade-off: rebuilt from scratch on every restart, won't scale past a small
dataset. Swapping to `ChromaVectorStore`/`PgVectorStore` later is a one-bean change in
`AppConfig`, not a rewrite.

**`QuestionAnswerAdvisor` import not resolving** — it lives at
`org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor`, shipped by the
`spring-ai-advisors-vector-store` artifact. Older Spring AI milestones had it in a different
package — that's usually why a blog post's import won't compile.

**`spring.ai.chat.memory.repository.jdbc.initialize-schema`** — set to `always` in
`application.yml`, auto-creates the `SPRING_AI_CHAT_MEMORY` table on startup. Skip it and
endpoint #3 fails on a fresh DB.

**Port 8080 already in use:**
```powershell
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

**`base-url` shape differs per provider — this one wastes real time:**

| Provider | `OPENAI_BASE_URL` | Why |
|---|---|---|
| OpenAI | `https://api.openai.com` (**no** `/v1`) | client appends `/v1/...` itself |
| Ollama | `http://localhost:11434` (**no** `/v1`) | same client, same rule — it appends `/v1/...` regardless of host |
| Groq | `https://api.groq.com/openai` (**no** `/v1`) | same rule again |

The pattern is simple once you see it: **never add `/v1` yourself, for any provider** — the
app's OpenAI client always appends `/v1/chat/completions` (and `/v1/embeddings`) on its own. Add
it yourself and you get a doubled path and a 404 that doesn't obviously explain itself.

**Tool-call JSON leaking into the answer text** (e.g. seeing
`{"name": "searchWeb", "parameters": {...}}` printed as plain chat instead of executed) — known
rough edge in Ollama's `/v1` compat layer, more common on smaller models. Not a bug here; the
exact same call can work fine seconds later. If you need rock-solid tool calling every time,
that's the real argument for OpenAI over local Ollama.

---

## 7. Project structure

```
src/main/java/com/launchpad/demo/
├── JavaAiLaunchpadDemoApplication.java   # entry point
├── config/
│   ├── AppConfig.java                    # 5 ChatClient beans - one per memory/tool strategy
│   └── OpenApiConfig.java                # Swagger UI page title/description
├── controller/
│   ├── DemoController.java               # endpoints #1-#5
│   └── RagController.java                # endpoint #6
├── rag/
│   └── DocumentIngestionService.java     # loads leave-policy.md into the vector store at startup
└── tool/
    └── EmsTools.java                     # getEmployee / getLeaveBalance / searchWeb (Serper)

src/main/resources/
├── application.yml
└── docs/leave-policy.md                  # the RAG source document
```

---

## 8. Recap: the only 4 things that change between Ollama and OpenAI

```
                          OPENAI_BASE_URL              OPENAI_API_KEY  CHAT_MODEL     EMBEDDING_MODEL
Ollama (local, free):     http://localhost:11434       ollama          llama3.2       nomic-embed-text
OpenAI (real, paid):      https://api.openai.com       sk-...          gpt-4o-mini    text-embedding-3-small
```

Everything else — every controller, tool, advisor — is identical. That's the actual point of
Spring AI's abstraction layer, demonstrated rather than just claimed.
