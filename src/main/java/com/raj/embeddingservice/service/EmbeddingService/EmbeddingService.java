//package com.raj.embeddingservice.service.EmbeddingService;
package com.raj.embeddingservice.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    // constructor injection — read the key from config, build the model once
    public EmbeddingService(@Value("${gemini.api.key}") String apiKey) {
        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-embedding-001")
                .build();
    }

    public List<Float> embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        return embedding.vectorAsList();
    }
    
    public List<List<Float>> embedBatch(List<String> texts) {
        // wrap each raw string as a TextSegment (what the batch API expects)
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();

        // one call embeds all segments at once
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // pull the number list out of each embedding
        return embeddings.stream()
                .map(Embedding::vectorAsList)
                .toList();
    }
}