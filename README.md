# Embedding Service

A standalone Spring Boot **microservice** with one job: turning text into **vector embeddings**.

It is part of a **Retrieval-Augmented Generation (RAG)** system built from scratch in Java with Spring Boot and LangChain4j, split into five independent microservices.

---

## Role in the System

This is **service #2** in a five-service RAG pipeline. Each service does exactly one job and runs as its own process, communicating over REST.

| Port | Service | Responsibility |
|------|---------|----------------|
| 8080 | API Gateway / Orchestrator | Single entry point; coordinates the flow |
| 8081 | Document Service | Raw document → clean chunks |
| **8082** | **Embedding Service** | **Text → vector** ← *this service* |
| 8083 | Retrieval Service | Stores vectors; query vector → most relevant chunks |
| 8084 | LLM / Answer Service | Chunks + question → final answer |

The gateway calls this service twice:
- **Ingest:** embeds every chunk of an uploaded document in one batch call.
- **Ask:** embeds the user's question so it can be compared against the stored chunks.

---

## Tech Stack

- **Java 17**
- **Spring Boot 3.3.5** (Spring Web)
- **LangChain4j 1.19.0** (versions managed via the LangChain4j BOM)
- **Google Gemini** `gemini-embedding-001` embedding model (3072 dimensions)
- **Maven**

---

## API

### `POST /api/embeddings/embed`

Embeds a single piece of text.

```bash
curl -X POST http://localhost:8082/api/embeddings/embed \
     -H "Content-Type: application/json" \
     -d '{"text": "What is a microservice?"}'
```

```json
{
  "dimensions": 3072,
  "vector": [0.0123, -0.0456, ...]
}
```

### `POST /api/embeddings/embed-batch`

Embeds many texts in **one call** to the model, which is much faster than one request per chunk.

```bash
curl -X POST http://localhost:8082/api/embeddings/embed-batch \
     -H "Content-Type: application/json" \
     -d '{"texts": ["first chunk", "second chunk"]}'
```

```json
{
  "count": 2,
  "dimensions": 3072,
  "vectors": [[0.0123, ...], [0.0789, ...]]
}
```

---

## Running Locally

**Prerequisites:** JDK 17+, a Google Gemini API key.

The API key is read from an **environment variable**. It is never stored in the code or committed to git.

```bash
# Linux / macOS
export GEMINI_API_KEY=your-key-here

# Windows PowerShell
$env:GEMINI_API_KEY="your-key-here"

./mvnw spring-boot:run
```

The service starts on **http://localhost:8082**.

### Configuration

`src/main/resources/application.properties`:

```properties
server.port=8082
gemini.api.key=${GEMINI_API_KEY}
```

---

## Project Structure

```
embedding-service/
├── src/main/java/com/raj/embeddingservice/
│   ├── EmbeddingServiceApplication.java                 # entry point
│   ├── controller/EmbeddingController.java              # REST endpoints
│   └── service/EmbeddingService/EmbeddingService.java   # Gemini embedding model
├── src/main/resources/
│   ├── application.properties
│   └── Notes/embedding-service-notes.md                 # learning notes
└── pom.xml
```
