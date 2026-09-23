package com.raj.embeddingservice.controller;

//import com.raj.embeddingservice.service.EmbeddingService;
import org.springframework.web.bind.annotation.*;

import com.raj.embeddingservice.service.EmbeddingService.EmbeddingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/embeddings")
@CrossOrigin(origins = "*")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/embed")
    public Map<String, Object> embedText(@RequestBody Map<String, String> request) {
        String text = request.get("text");

        List<Float> vector = embeddingService.embed(text);

        return Map.of(
            "dimensions", vector.size(),
            "vector", vector
        );
    }
    @PostMapping("/embed-batch")
    public Map<String, Object> embedBatch(@RequestBody Map<String, List<String>> request) {
        List<String> texts = request.get("texts");

        List<List<Float>> vectors = embeddingService.embedBatch(texts);

        return Map.of(
            "count", vectors.size(),
            "dimensions", vectors.isEmpty() ? 0 : vectors.get(0).size(),
            "vectors", vectors
        );
    }
}