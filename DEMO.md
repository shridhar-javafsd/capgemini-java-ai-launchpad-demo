# DEMO DAY — Live Call Cheat Sheet

Keep this open on a second monitor/tab. Copy-paste, don't retype. Total: 30 min.

---

## Before the call (do this now, not live)

- [ ] App already running (`mvn spring-boot:run`), tested once, left up
- [ ] Confirm env vars are the OpenAI ones, not Ollama's — check with:
  ```powershell
  echo $env:OPENAI_BASE_URL   # should be https://api.openai.com
  ```
- [ ] Swagger UI tab open: **http://localhost:8080/swagger-ui.html**
- [ ] IDE open, Copilot Chat panel visible, `AppConfig.java` + `EmsTools.java` in tabs
- [ ] A terminal tab showing the app's live logs
- [ ] This file open, README open in a background tab as backup

---

## Switching providers mid-demo (Ollama first, then OpenAI)

One paste each — set all five vars in one line, then restart the app (env vars are
only read at startup, so a running app won't pick up a mid-session change).

**Ollama (paste this first):**
```powershell
$env:OPENAI_BASE_URL="http://localhost:11434"; $env:OPENAI_API_KEY="ollama"; $env:OPENAI_CHAT_MODEL="llama3.2"; $env:OPENAI_EMBEDDING_MODEL="nomic-embed-text"; $env:SERPER_API_KEY="your-serper-key-here"
```

**OpenAI (paste this when you switch):**
```powershell
$env:OPENAI_BASE_URL="https://api.openai.com"; $env:OPENAI_API_KEY="sk-your-real-key"; $env:OPENAI_CHAT_MODEL="gpt-4o-mini"; $env:OPENAI_EMBEDDING_MODEL="text-embedding-3-small"; $env:SERPER_API_KEY="your-serper-key-here"
```

After either paste: `Ctrl+C` to stop the running app, `y` to confirm, then
`mvn spring-boot:run` again. Watch for `[RAG] Ingested...` in the logs — that's your
signal it's back up and using the new provider.

**Note the Ollama base URL has no `/v1`** — that was a bug we fixed earlier; double-check
you're not pasting an older version from elsewhere.

**Suggested split, to stay inside 30 minutes:** run only #1 (simple chat) and #2
(in-memory recall) on Ollama — enough to say "this runs free and local too" — then
switch to OpenAI *before* #3 onward. Save tool chaining, RAG, and the failure case for
OpenAI, where they're proven reliable. Don't repeat the full six-endpoint sequence on
both providers; that's the fastest way to burn your time budget and risk a live
stumble on the parts that matter most.

---

## 0. Frame it (2 min) — say this, don't read it

> "I've built a working Spring AI app covering the full list — chatbot, both memory
> strategies for single and multi-user, web search, tool chaining, and RAG. It's
> already running, and I'll walk through it live via Swagger."

Then, in one breath:

> "It's a Spring Boot app, EMS-domain, with a ChatClient bean per scenario, Serper
> for web search, an in-memory vector store for RAG, and HSQLDB for persistent chat
> memory."

---

## 1. Simple chatbot
```powershell
curl.exe -G localhost:8080/api/chat/simple --data-urlencode "message=What is dependency injection?"
```
*(Or click "Try it out" on `/api/chat/simple` in Swagger — either works, whichever feels faster live.)*

---

## 2. In-memory recall
```powershell
curl.exe -G localhost:8080/api/chat/memory/inmemory --data-urlencode "conversationId=demo" --data-urlencode "message=My name is Vaman."
curl.exe -G localhost:8080/api/chat/memory/inmemory --data-urlencode "conversationId=demo" --data-urlencode "message=What's my name?"
```
Should correctly say "Vaman" on the second call.

---

## 3. JDBC memory + persistence proof (your strongest single moment #1)
```powershell
curl.exe -G localhost:8080/api/chat/memory/jdbc --data-urlencode "conversationId=trainer1" --data-urlencode "message=I work in the Sales department."
curl.exe -G localhost:8080/api/chat/memory/history --data-urlencode "conversationId=trainer1"
```
Say while running the second command:
> "This reads straight from the database, not the model — proof this is real
> persistence, not just the model appearing to remember."

---

## 4. Web search
```powershell
curl.exe -G localhost:8080/api/chat/websearch --data-urlencode "message=What is the latest stable Spring Boot version?"
```

---

## 5. Tool chaining — combined query (your strongest single moment #2)
```powershell
curl.exe -G localhost:8080/api/chat/toolchain --data-urlencode "message=Look up employee E101 and tell me their leave balance, then search the web for how many annual leave days are typical in India."
```
Say while it runs:
> "One question, two different tools called in sequence before it answers —
> employee lookup, leave balance, and a live web search, all in one turn."

---

## 6. RAG
```powershell
curl.exe -G localhost:8080/api/rag/ask --data-urlencode "question=How many days of sick leave do I get?"
```

---

## 7. Live Copilot Chat prompt (3 min) — the "vibe coding" proof point

In the IDE, open `EmsTools.java`, open Copilot Chat, and type (narrate it out loud as you type):

> "Add a new @Tool method called getEmployeesByDepartment that returns all employees
> in a given department from the existing EMPLOYEES map."

Let it generate, glance at the output, comment on whether you'd accept it as-is or fix
something — that reaction *is* the point, not the generated code itself.

---

## 8. Deliberate failure case (3 min) — the senior-trainer moment
```powershell
curl.exe -G localhost:8080/api/chat/toolchain --data-urlencode "message=What is the leave balance for employee E999?"
```
Should gracefully say "No employee found" instead of inventing an answer. Say:

> "This is what I'd teach trainees to test for — AI-generated code that looks
> confident but needs validation."

If it comes up naturally, you can also mention hitting real tool-calling flakiness
testing locally with a small free model earlier — a genuine example of exactly this
failure pattern, not a hypothetical.

---

## 9. Q&A buffer (5-8 min) — bridge, don't stumble

If asked about something you haven't demoed live (Maven, JDBC internals, Jenkins,
Sonar, JPA specifics), bridge to your own curriculum instead of freestyling:

> "That's covered in Days [X] of the curriculum I built for this program — happy to
> walk through how I sequence it."

**Quick answers if asked directly:**

| Question | 30-second answer |
|---|---|
| Agentic AI vs. this tool-calling demo? | Tool-calling here is single-turn — model picks a tool, gets a result, answers. Agentic AI adds planning and looping across multiple turns toward a goal, often self-correcting without a human directing each step. |
| Why JDBC memory over a plain List? | Persists across restarts, and scopes history per conversationId so multiple users don't share memory — what you'd actually run in production. |
| Why did you use a local model to test first? | Cost and iteration speed during development — caught real bugs (schema init, package changes, base-URL quirks) for free before spending on the real API. |
| Common prompt engineering anti-patterns? | Vague prompts with no role/constraints/output format, and single-shot prompting for complex tasks instead of iterative refinement. |
| RAG gave a wrong answer — why, if the doc was ingested? | Usually chunking (splitting mid-fact), weak embedding match to the query wording, or retrieving too few/many chunks — the document being present doesn't guarantee retrieval quality. |

---

## If something breaks live

Don't panic-debug on camera. Say:
> "Let me note that and follow up" — then move to the next endpoint.

Full troubleshooting table (HSQLDB lock files, base-URL quirks, schema errors) is in
`README.md`, Troubleshooting section — check it after the call, not during.

---

## Closing line

> "Everything you just saw ran clean against real OpenAI this morning — happy to dig
> into any part of it further, or talk through how this maps to the training
> curriculum."
