# Java AI Launchpad — Trainer Evaluation Demo (minimal build)

No Docker, no external servers. Everything runs from one `mvn spring-boot:run`.

## Setup

```bash
export OPENAI_API_KEY=sk-...
export TAVILY_API_KEY=tvly-...   # optional — omit and web search just returns a stub message
mvn clean install
mvn spring-boot:run
```

On boot you should see `[RAG] Ingested N chunks from leave-policy.md (in-memory store)`.
That confirms RAG is ready with zero extra setup.

## Demo script

```bash
# 1. Simple chatbot
curl "http://localhost:8080/api/chat/simple?message=What+can+you+help+me+with?"

# 2. Stateful chat - in-memory (lost on restart)
curl "http://localhost:8080/api/chat/memory/inmemory?conversationId=demo-1&message=My+name+is+Vaman"
curl "http://localhost:8080/api/chat/memory/inmemory?conversationId=demo-1&message=What+is+my+name?"

# 3. Stateful chat - JDBC-backed, single vs multi-user
curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-1&message=My+name+is+Vaman"
curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-2&message=What+is+my+name?"   # no memory of Vaman
curl "http://localhost:8080/api/chat/memory/history?conversationId=user-1"                          # proof: read straight from DB

# 4. Web search tool
curl "http://localhost:8080/api/chat/websearch?message=What+is+the+latest+stable+Spring+Boot+version?"

# 5. Tool chaining (employee lookup -> leave balance, two-hop call in one turn)
curl "http://localhost:8080/api/chat/toolchain?message=What+is+the+leave+balance+for+employee+E102?"

# 6. RAG over the EMS leave policy (in-memory vector store)
curl "http://localhost:8080/api/rag/ask?question=How+many+days+of+sick+leave+do+I+get?"
curl "http://localhost:8080/api/rag/ask?question=Can+I+carry+forward+unused+leave?"
```

Recommended: also ask the tool-chain endpoint about a non-existent employee (`E999`)
to show it fails gracefully instead of hallucinating — a strong "I understand
AI's failure modes" moment for the evaluators.

## Why these choices

- **HSQLDB, not H2**, for JDBC chat memory — Spring AI's JDBC chat memory
  module only ships schema scripts for PostgreSQL, MySQL/MariaDB, SQL Server,
  HSQLDB, and Oracle. H2 isn't supported and fails at startup.
- **SimpleVectorStore, not ChromaDB**, for RAG — in-memory, ships in Spring
  AI core, needs no Docker or external process. The coordinator's brief
  explicitly lists "simple vector store" as one of the three acceptable RAG
  options, so this is a legitimate choice, not a shortcut.
- **`QuestionAnswerAdvisor`** lives in `org.springframework.ai.chat.client.advisor.vectorstore`
  and needs the separate `spring-ai-advisors-vector-store` dependency in
  Spring AI 1.0.0 GA — easy to miss since older milestone docs show a
  different package.

## Project layout

```
config/AppConfig.java       All ChatClient beans + the VectorStore bean, in one place
tool/EmsTools.java           @Tool methods: employee lookup, leave balance, web search
controller/DemoController.java   Endpoints 1-5
controller/RagController.java    Endpoint 6
rag/DocumentIngestionService.java  Loads leave-policy.md into the vector store on boot
resources/docs/leave-policy.md     Sample EMS document
```

## If you want ChromaDB back later

Once the core demo is solid, ChromaDB can be swapped back in for a closer-to-production
RAG story: add `spring-ai-starter-vector-store-chroma`, remove the `vectorStore` bean
from `AppConfig.java` (let auto-configuration create it instead), and run Chroma
locally without Docker via `pip install chromadb && chroma run --path ./chroma-data`.
Not required for the evaluation — only worth doing if you want the extra polish.
