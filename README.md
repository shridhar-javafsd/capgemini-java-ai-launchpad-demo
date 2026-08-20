# Java AI Launchpad — EMS Spring AI Demo

A single Spring Boot app that shows off six core Spring AI capabilities, all wired to a tiny
fictional Employee Management System (EMS):

1. **Simple chatbot** — stateless, no memory
2. **In-memory conversation memory** — remembers you, forgets on restart
3. **JDBC-backed conversation memory** — remembers you *forever* (well, until you delete the DB file)
4. **Web search tool calling** — the model decides on its own when to search the web
5. **Tool chaining** — the model calls more than one tool in a single turn
6. **RAG** — answers grounded in an actual policy doc, not vibes

You can run this two ways: **free and local with Ollama**, or **with a real OpenAI key**. Same
code either way — genuinely zero code changes, just environment variables. We'll do Ollama first
because it costs nothing and you can be chatting with it in under 10 minutes.

---

## 0. Before you clone anything

You'll need:

- **Java 17+** — check with `java -version`. Don't have it?
  - macOS: `brew install openjdk@17`
  - Linux: `sudo apt install openjdk-17-jdk` (Debian/Ubuntu) or your distro's equivalent
  - Windows: download from [adoptium.net](https://adoptium.net) and run the installer
  - Any OS, one-liner: [SDKMAN](https://sdkman.io) → `sdk install java 17.0.13-tem`
- **Maven 3.9+** — check with `mvn -version`. Don't have it?
  - macOS: `brew install maven`
  - Linux: `sudo apt install maven`
  - Windows: download from [maven.apache.org](https://maven.apache.org/download.cgi), unzip, add `bin` to your `PATH`
  - Any OS: [SDKMAN](https://sdkman.io) → `sdk install maven`
- A [Serper.dev](https://serper.dev) API key (free, 2,500 queries, no card required) — this
  powers the web search tool (#4 and #5 above). Sign up, grab the key from your dashboard. The
  app *runs* without it, the search tool just won't return real results.

That's it for prerequisites shared by both paths. Model-specific setup is below.

### Clone the repo

**macOS / Linux:**
```bash
git clone <this-repo-url>
cd <repo-folder-name>
```

**Windows (PowerShell):**
```powershell
git clone <this-repo-url>
cd <repo-folder-name>
```
Same command either way — `git clone` doesn't change between shells. (You do need
[Git for Windows](https://git-scm.com/download/win) installed first if you don't have it; that
also gives you the `git` command inside PowerShell, so there's no need to go looking for a
separate Git Bash window for this.)

(Swap in the actual URL/folder — replace this line once you've got the repo up on GitHub.)

### On Windows? Read this before you run anything

Every command in this README is written in **two flavors side by side: macOS/Linux (bash)** and
**Windows (PowerShell)**. Stick to PowerShell throughout if you're on Windows — don't mix it with
Command Prompt, the syntax differs between the two and hopping between them mid-README is how
people get stuck. Open it via the Start menu ("PowerShell" or "Terminal" — Windows 11's default
terminal is PowerShell already).

Two things differ from the bash commands you'll see elsewhere in this README:

- `export VAR=value` → `$env:VAR="value"`
- **Important — the curl gotcha:** in PowerShell, `curl` is secretly an *alias for
  `Invoke-WebRequest`*, not real curl. It doesn't understand `-G` or `--data-urlencode`, so every
  curl example in [section 4](#4-the-6-features-with-examples) needs one small change: type
  `curl.exe` instead of `curl`. Windows 11 ships a real curl.exe alongside the alias — typing
  `.exe` forces PowerShell to use that instead of the `Invoke-WebRequest` alias. Every curl
  example from here on shows both forms side by side so you don't have to remember this.

---

## 1. Quick start with Ollama (free, local, no API key)

### 1.1 Install Ollama and pull two models

**Install Ollama first** — pick your OS:

```bash
# macOS (Homebrew)
brew install ollama

# macOS / Linux (official install script — works on both)
curl -fsSL https://ollama.com/install.sh | sh

# Windows
# Download and run the installer: https://ollama.com/download/windows
```

Verify it installed correctly:

```bash
ollama --version
```

> **Heads up:** the macOS and Windows installers both set Ollama up to run automatically in the
> background (you'll see a llama icon in your menu bar / system tray). If that's the case, you
> don't need to run `ollama serve` manually — it's already listening on port 11434. If you try
> anyway and see `Error: listen tcp 127.0.0.1:11434: bind: address already in use`, that error
> is actually good news — it means Ollama's already running, just carry on to the next step.

You need **two** local models — one for chat, one for embeddings (RAG needs both, and Ollama
doesn't ship a default embedding model the way OpenAI does).

```bash
ollama pull llama3.2          # chat model
ollama pull nomic-embed-text  # embedding model, needed for RAG (#6)
ollama serve                  # only if it's not already running in the background — see note above
```

Sanity-check it's alive:

macOS / Linux:
```bash
curl http://localhost:11434/v1/models
```
Windows (PowerShell):
```powershell
curl.exe http://localhost:11434/v1/models
```

### 1.2 Set your environment variables

**macOS / Linux:**
```bash
export OPENAI_BASE_URL=http://localhost:11434/v1   # note the /v1 — see gotcha #5 below
export OPENAI_API_KEY=ollama                        # any non-empty string works, Ollama ignores it
export OPENAI_CHAT_MODEL=llama3.2
export OPENAI_EMBEDDING_MODEL=nomic-embed-text
export SERPER_API_KEY=your-serper-key-here
```

**Windows (PowerShell):**
```powershell
$env:OPENAI_BASE_URL="http://localhost:11434/v1"
$env:OPENAI_API_KEY="ollama"
$env:OPENAI_CHAT_MODEL="llama3.2"
$env:OPENAI_EMBEDDING_MODEL="nomic-embed-text"
$env:SERPER_API_KEY="your-serper-key-here"
```
*(Note: these `$env:` variables only last for the current PowerShell window/session. If you close
the terminal, you'll need to set them again before your next `mvn spring-boot:run`.)*

### 1.3 Run it

```bash
mvn spring-boot:run
```

> First run will take a few minutes — Maven's downloading every dependency (Spring Boot, Spring
> AI, etc.) for the first time. Every run after that is fast, since they're cached locally in
> `~/.m2`.

App comes up on `http://localhost:8080`. Jump to [section 4](#4-the-6-features-with-examples) to
start hitting endpoints, or [section 5](#5-swagger-ui-try-it-in-the-browser) to try it in the browser.

> **Heads up on quality:** small local models like `llama3.2` are genuinely less reliable at tool
> calling than GPT-4o-mini — see [Troubleshooting](#6-troubleshooting--things-we-actually-hit).
> It's not a bug in this repo, it's just what running a 3B-class model locally looks like.

---

## 2. Switching to OpenAI (real key, zero code changes)

This whole app reads its model config from environment variables — there is genuinely nothing
to edit in the code or `application.yml`. Just point the same four variables at OpenAI instead:

**macOS / Linux:**
```bash
export OPENAI_BASE_URL=https://api.openai.com   # no /v1 here — see gotcha #5
export OPENAI_API_KEY=sk-...your real key...
export OPENAI_CHAT_MODEL=gpt-4o-mini
export OPENAI_EMBEDDING_MODEL=text-embedding-3-small
export SERPER_API_KEY=your-serper-key-here

mvn spring-boot:run
```

**Windows (PowerShell):**
```powershell
$env:OPENAI_BASE_URL="https://api.openai.com"
$env:OPENAI_API_KEY="sk-...your real key..."
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"
$env:OPENAI_EMBEDDING_MODEL="text-embedding-3-small"
$env:SERPER_API_KEY="your-serper-key-here"

mvn spring-boot:run
```

Same jar, same endpoints, same everything — just a smarter, paid model behind it. This is the
config you want before anything that actually matters (a demo, an evaluation, showing a friend).

---

## 3. What's actually happening under the hood

`application.yml` has this at its core:

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

Ollama ships an OpenAI-compatible API, so Spring AI's regular OpenAI client can talk to it —
you're just handing it a different `base-url`. That's the entire trick. No Ollama-specific
starter, no separate config class, no `if (provider == ...)` branching anywhere in the codebase.

---

## 4. The 6 features, with examples

All examples below use curl — macOS/Linux uses real `curl`; Windows PowerShell uses `curl.exe`
(see the curl gotcha explained earlier) with backtick (`` ` ``) line continuations instead of
backslash. See [section 5](#5-swagger-ui-try-it-in-the-browser) for a point-and-click way to do
the same thing without typing any of this.

### 1️⃣ Simple chatbot — no memory

macOS / Linux:
```bash
curl -G localhost:8080/api/chat/simple --data-urlencode "message=What is dependency injection?"
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/chat/simple --data-urlencode "message=What is dependency injection?"
```

### 2️⃣ In-memory conversation — remembers you until restart

macOS / Linux:
```bash
curl -G localhost:8080/api/chat/memory/inmemory \
  --data-urlencode "conversationId=alice" \
  --data-urlencode "message=My name is Alice."

curl -G localhost:8080/api/chat/memory/inmemory \
  --data-urlencode "conversationId=alice" \
  --data-urlencode "message=What's my name?"
# -> should say Alice
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/chat/memory/inmemory `
  --data-urlencode "conversationId=alice" `
  --data-urlencode "message=My name is Alice."

curl.exe -G localhost:8080/api/chat/memory/inmemory `
  --data-urlencode "conversationId=alice" `
  --data-urlencode "message=What's my name?"
# -> should say Alice
```
Try a different `conversationId` and it won't know anything about Alice — each id is its own
isolated conversation. That's single-user and multi-user support, from the same endpoint.

### 3️⃣ JDBC-backed conversation — remembers you across restarts

Same idea, different endpoint:

macOS / Linux:
```bash
curl -G localhost:8080/api/chat/memory/jdbc \
  --data-urlencode "conversationId=bob" \
  --data-urlencode "message=I work in the Sales department."
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/chat/memory/jdbc `
  --data-urlencode "conversationId=bob" `
  --data-urlencode "message=I work in the Sales department."
```
Restart the app, then ask it again with the same `conversationId` — it still remembers, because
it's reading/writing an actual HSQLDB table on disk instead of a JVM-memory map.

Proof-of-persistence endpoint — reads the raw stored history straight from the DB:

macOS / Linux:
```bash
curl -G localhost:8080/api/chat/memory/history --data-urlencode "conversationId=bob"
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/chat/memory/history --data-urlencode "conversationId=bob"
```

### 4️⃣ Web search tool

macOS / Linux:
```bash
curl -G localhost:8080/api/chat/websearch \
  --data-urlencode "message=What is the latest stable Spring Boot version?"
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/chat/websearch `
  --data-urlencode "message=What is the latest stable Spring Boot version?"
```
The model decides on its own whether the question needs a live web search — you're not routing
this manually anywhere in the code.

### 5️⃣ Tool chaining

macOS / Linux:
```bash
curl -G localhost:8080/api/chat/toolchain \
  --data-urlencode "message=Look up employee E101 and tell me their leave balance, then search the web for how many annual leave days are typical in India."
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/chat/toolchain `
  --data-urlencode "message=Look up employee E101 and tell me their leave balance, then search the web for how many annual leave days are typical in India."
```
This forces the model to call **two different tools** (`getEmployee`/`getLeaveBalance` and
`searchWeb`) in one turn before it can answer — that's tool chaining.

### 6️⃣ RAG — grounded in `leave-policy.md`

macOS / Linux:
```bash
curl -G localhost:8080/api/rag/ask \
  --data-urlencode "question=How many days of sick leave do I get?"
```
Windows (PowerShell):
```powershell
curl.exe -G localhost:8080/api/rag/ask `
  --data-urlencode "question=How many days of sick leave do I get?"
```
The answer traces back to `src/main/resources/docs/leave-policy.md`, not to whatever the model
happened to memorize during training — that's the whole point of RAG.

---

## 5. Swagger UI — try it in the browser

No curl, no Postman, nothing to install. Once the app is running:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Raw OpenAPI spec (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Every endpoint above shows up there automatically, grouped and numbered in the order you'd
naturally walk through them, with a "Try it out" button that fires a real request and shows you
the real response — no separate tool needed. This is genuinely how most real Spring Boot teams
hand off an API for others to explore, not just a demo trick.

---

## 6. Troubleshooting / things we actually hit

These aren't hypothetical — every one of these was a real error message at some point building
this repo.

### HSQLDB instead of H2
Spring AI's JDBC chat-memory module only ships ready-made schema scripts for a specific set of
databases (PostgreSQL, MySQL, SQL Server, HSQLDB, Oracle) — **H2 isn't in that list** in the
current release, even though H2 is usually everyone's first instinct for "just give me an
embedded DB." Using H2 here throws a `No schema scripts found` error at startup. HSQLDB is,
so that's what this project uses for the JDBC-memory demo.

### `SimpleVectorStore` instead of ChromaDB
RAG needs a vector store. `SimpleVectorStore` is in-memory and ships with Spring AI core — no
extra process to install or run before you can try the RAG endpoint. The trade-off: it's rebuilt
from scratch every time the app restarts and won't scale past a small demo dataset. Swapping in
`ChromaVectorStore` or `PgVectorStore` later is a one-bean change in `AppConfig`, not a rewrite.

### `QuestionAnswerAdvisor`'s package
If you're following an older tutorial and get a "cannot find symbol" on this import, it's because
the class lives at:
```java
org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
```
and ships in its own artifact:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-advisors-vector-store</artifactId>
</dependency>
```
Older Spring AI milestone versions had this advisor in a different package — if a blog post's
import doesn't compile, this is usually why.

### `spring.ai.chat.memory.repository.jdbc.initialize-schema`
This property (set to `always` in `application.yml`) is what auto-creates the
`SPRING_AI_CHAT_MEMORY` table on startup. Without it, endpoint #3 fails the first time you ever
run the app against a fresh database.

### Port 8080 already in use
If `mvn spring-boot:run` fails with something like `Web server failed to start. Port 8080 was
already in use`, something else on your machine is already using that port. Either stop that
other process, or run this app on a different port for one session without touching any config
file:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### `base-url` isn't the same shape for every provider — read this before you get a 404
This is the one that actually costs people time, so pay attention to the exact value:

| Provider | `OPENAI_BASE_URL` value | Why |
|---|---|---|
| OpenAI | `https://api.openai.com` (**no** `/v1`) | Spring AI's OpenAI client appends `/v1/...` itself for the real OpenAI API |
| Ollama | `http://localhost:11434/v1` (**with** `/v1`) | Ollama's OpenAI-compatibility layer expects the `/v1` prefix already in the base URL you give it |

Mixing these up gets you a 404 or a connection error that doesn't obviously point at the real
cause.

**Also, about that tool-call-JSON-leaking-into-the-answer-text bug:** if you've seen a response
that looks like `{"name": "searchWeb", "parameters": {...}}` printed out as plain chat text
instead of the tool actually running — that's a known rough edge with Ollama's `/v1`
OpenAI-compatibility layer specifically, more common with smaller models. It isn't a bug in this
code; the exact same tool call can work perfectly a few requests later against the exact same
setup. If you need rock-solid tool calling every time, that's the strongest reason to run this
against a real OpenAI key (or a larger model) rather than something to debug further here.

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
OPENAI_BASE_URL        OPENAI_API_KEY   OPENAI_CHAT_MODEL   OPENAI_EMBEDDING_MODEL
─────────────────────  ───────────────  ───────────────────  ───────────────────────
Ollama (local, free):
http://localhost:11434/v1   ollama       llama3.2             nomic-embed-text

OpenAI (real, paid):
https://api.openai.com      sk-...       gpt-4o-mini           text-embedding-3-small
```

Everything else — every controller, every tool, every advisor — is identical. That's the actual
point of Spring AI's abstraction layer, demonstrated rather than just claimed.
