# Embedding Service — Learning Notes (Standalone)

> Service #2 in the RAG pipeline. Port **8082**. One job: **text → vector**.
> Stateless — text comes in, a vector goes out. It does NOT store or search vectors
> (that's the Retrieval Service's job).

---

## 1. What an Embedding Is

An **embedding is a list of numbers (a vector) that represents a piece of text's *meaning*.**
The model is trained so text with similar meaning gets vectors pointing in similar directions.
Meaning becomes geometry.

- You don't design the numbers — the model learned them from huge amounts of text.
- Same model must embed BOTH the chunks and the question, or the vectors live in different
  spaces and comparison is meaningless.
- This service only *produces* vectors. Storing + searching them = Retrieval Service (8083).

### Cosine similarity (interview favourite)
- Measures the **angle** between two vectors, not the distance between their tips.
- Same direction → ~0° → cosine ≈ **1** (same meaning). Unrelated → ~90° → **0**.
- **Why angle, not straight-line distance?** We care about **direction, not magnitude.**
  A vector's length can be affected by text length / word frequency — stuff that isn't meaning.
  Cosine ignores magnitude and focuses on "are these pointing the same way?"
- Formula: `cos(A,B) = (A · B) / (‖A‖ × ‖B‖)` — dividing by the lengths cancels magnitude.

---

## 2. The Provider Journey (real engineering story)

This is a STRONGER interview story than "it worked first try":

1. **OpenAI `text-embedding-3-small`** (1536 dims) — coded it, but my credit balance was empty → every call failed (500 / insufficient quota).
2. **Tried Groq** — but **Groq has no embeddings.** Groq is an LLM *inference* service (chat/generation only). Wrong tool. (Groq is for the LLM/Answer Service later, not here.)
3. **Google `text-embedding-004`** — hit a **404: model not found**. Google **deprecated it on Jan 14, 2026.**
4. **Google `gemini-embedding-001`** — works. **3072 dimensions.** ✅

**Lesson:** provider migration, dead balances, and model deprecations are normal. Handle them, don't panic.

---

## 3. The Interface Payoff (the design win)

Field typed as the **interface**, not the concrete class:

```java
private final EmbeddingModel embeddingModel;   // interface, NOT OpenAiEmbeddingModel
```

Only the **constructor** knows which provider it is. Because of this, swapping OpenAI → Google
was a change to **one dependency + one constructor**. Controller, endpoint, `embed()` method —
untouched. Did it TWICE (OpenAI→Google, then model swap) and nothing downstream moved.

**Interview line:**
> *"I coded against the `EmbeddingModel` interface, not the concrete class. Only the constructor
> knows it's Google. So switching providers is a one-line change — nothing downstream is touched.
> That's loose coupling — the Dependency Inversion Principle (the D in SOLID)."*

Same idea reused for the vector store later (`EmbeddingStore` interface → swap in-memory for a real
vector DB in one line). Interface-based design, used consistently.

---

## 4. API Key Security (never commit secrets)

**Incident:** I once pasted the raw key straight into `application.properties`. Wrong — revoked it
immediately and made a new one. Reflex now: **secrets live in environment variables, never in files.**

**The key's path — 4 hops (interview answer):**
1. Lives in the `GEMINI_API_KEY` **environment variable** (set in the IDE Run Config → Environment tab).
2. `application.properties` reads it: `gemini.api.key=${GEMINI_API_KEY}` → a Spring property.
3. `@Value("${gemini.api.key}")` injects that property into the constructor parameter.
4. Passed to `.apiKey(apiKey)` in the builder.

The actual secret only ever exists in the environment — never in a committed file. That's the whole win.

---

## 5. Maven Lessons (pom.xml)

- **`<dependencyManagement>` vs `<dependencies>`:**
  - `dependencyManagement` = "*if* anyone uses this, here's the version." Manages, doesn't add.
  - `dependencies` = "add this library now." Actually adds.
  - The **BOM** goes in `dependencyManagement`; the real libraries go in `dependencies`.
  - Bug I hit: put `langchain4j-open-ai` inside `dependencyManagement` → it was version-managed but
    never added to the classpath → imports wouldn't resolve. Fix: move it into `dependencies`.
- **Transitive dependencies:** `langchain4j-open-ai` depends on `langchain4j-core`, so adding the
  OpenAI module automatically pulled in core (where `EmbeddingModel`, `Embedding` live). One line,
  both libraries. → *"It came in transitively."*
- Keep ALL services on the **same Spring Boot version (3.3.5)** — parent `4.1.1` gave wrong starter
  names (`spring-boot-starter-webmvc`); correct is `spring-boot-starter-web`.

---

## 6. Other Gotchas

- **`package` line bug:** wrote `package com.raj.embeddingservice.service.EmbeddingService;` —
  put the CLASS NAME in the package path. `package` = the folder only; the class name is the filename.
  Correct: `package com.raj.embeddingservice.service;`
- **`@RequestBody` vs `@RequestParam`:**
  - File upload (Document Service) = `@RequestParam` + `MultipartFile` (multipart/form-data).
  - JSON body (this service, `{"text": "..."}`) = `@RequestBody`.
  - Need the `Content-Type: application/json` header for `@RequestBody` to kick in.
- For a single JSON field I used `Map<String, String>`; the "proper" version is a DTO
  (`record EmbedRequest(String text)`) for type safety.

---

## 7. The Dimension — 3072 (LOAD-BEARING)

- `gemini-embedding-001` default output = **3072 dimensions.**
- Uses Matryoshka Representation Learning → can scale down to 768 / 1536 / 3072 via output
  dimensionality if needed (smaller = less storage).
- **This number is critical for the Retrieval Service:** the vector store is sized to exactly 3072,
  and question vectors MUST match. Mismatched dimensions = store rejects the vectors.

---

## 8. The Endpoint

`POST /api/embeddings/embed` — JSON in, vector out.

```java
@PostMapping("/embed")
public Map<String, Object> embedText(@RequestBody Map<String, String> request) {
    String text = request.get("text");
    List<Float> vector = embeddingService.embed(text);
    return Map.of("dimensions", vector.size(), "vector", vector);
}
```

Tested with curl → `{"dimensions": 3072, "vector": [0.024, -0.007, ...]}` ✅

---

## Status / Next

- [x] Concept: embeddings + cosine similarity
- [x] Service scaffolded on 8082, Google Gemini via `langchain4j-google-ai-gemini`
- [x] API key secured in env var (`GEMINI_API_KEY`)
- [x] `EmbeddingService` (interface-typed field, Google constructor)
- [x] `EmbeddingController` — single-text embed endpoint, tested (3072 dims)
- [ ] **Batch endpoint** — embed a `List<String>` of chunks in one call (used at upload time)
- [ ] Then: Retrieval Service (8083) — vector store sized to **3072**

**Model:** `gemini-embedding-001` · **Dimensions:** 3072
**Commit:** `feat: embedding service — text to vector via Gemini gemini-embedding-001 (3072-dim)`
