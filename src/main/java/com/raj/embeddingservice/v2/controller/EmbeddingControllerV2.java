package com.raj.embeddingservice.v2.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.raj.embeddingservice.v2.service.EmbeddingServiceV2;

@RestController
@RequestMapping("/api/v2/embeddings")
public class EmbeddingControllerV2 {
  private EmbeddingServiceV2 embeddingServiceV2;
  String text;
   public EmbeddingControllerV2 (EmbeddingServiceV2 embeddingServiceV2)
   {
	   this.embeddingServiceV2=embeddingServiceV2;
   }
   
   @PostMapping("/embed-batch")
   public Map<String, Object> embed(@RequestBody Map<String, List<String>> request)
   {   
	   List<String> texts = request.get("texts");
	   List<List<Float>> vectors = embeddingServiceV2.embedBatch(texts);
	   
	   return Map.of(
	            "count", vectors.size(),
	            "dimensions", vectors.isEmpty() ? 0 : vectors.get(0).size(),
	            "vectors", vectors
	        );
   }
}
