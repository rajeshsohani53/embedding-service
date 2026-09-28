package com.raj.embeddingservice.v2.service;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.raj.embeddingservice.v2.service.config.EmbeddingProperties;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class EmbeddingServiceV2 {
  private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceV2.class);
  private final EmbeddingModel embeddingModel;
  private final EmbeddingProperties properties;
  private static final int MAX_TEXT_LENGTH = 8000;
  private static final int MAX_BATCH_SIZE = 100;
  /// google gemni embedding model and embed text to vector in 100 batch at a time
  /// ["hello", "world", "foo"]  →  batch size = 3
  /// ["a", "b", "c", ..., "z"]  →  batch size = 26
  /// 100 strings                →  batch size = 100  ✅ allowed
  /// 101 strings                →  batch size = 101  ❌ rejected
    public EmbeddingServiceV2 (@Value("${gemini.api.key}") String apiKey,EmbeddingProperties properties)//we will get the api key frist
    {
    	// so first here we build the model 
    	GoogleAiEmbeddingModel googleModel=GoogleAiEmbeddingModel
    	.builder()
    	.apiKey(apiKey)
    	.modelName(properties.getModelName())
    	.build();	
    	this.embeddingModel=googleModel;
    	this.properties=properties;
    }
    // for here we will update our EmbeddingService okay 
    /// because our googel gemeni limit is 2048 tokens at a time 
    /// and if we conside 1 token=1 charaacter then 2048 token means 8000 character okay 
    /// 
    /// ---------------------------------------------------

     
    public List<List<Float>> embedBatch(List<String> texts)
    {
    	log.info("embedBatch called with {} texts", texts == null ? 0 : texts.size());

        /// so next we check the input text is null or not
        if(texts==null)
        {
        	// if it null then we will throw an exception okay 
        	 log.warn("text list cannot be null");
        	throw new IllegalArgumentException("text list cannot be null");
        }
        
        if (texts.isEmpty()) {
        	log.warn("Texts list cannot be empty");
            throw new IllegalArgumentException("Texts list cannot be empty");
        }
         
        // list must must not exist more than batchh size 
        if(texts.size()>properties.getMaxBatchSize())
        {
        	log.warn("batch size is big ");
        	throw new IllegalArgumentException("batch size is big ");
        }
        
        //the validateion 
        if(texts.stream().anyMatch((oneText)->oneText==null))
        {
        	log.warn("text list cannot contain null value okay ");
        	throw new IllegalArgumentException("text list cannot contain null value okay  ");
        }
        if(texts.stream().anyMatch(text->text.isBlank()))
        {
        	log.warn("Text cannot be empty or blank");
        	throw new IllegalArgumentException(
                    "Text cannot be empty or blank");
        }
        
        if (texts.stream().anyMatch(text -> text.length() > properties.getMaxTextLength())) {
           log.warn("One or more texts exceed the maximum length");
        	throw new IllegalArgumentException(
                "One or more texts exceed the maximum length of " + properties.getMaxTextLength() + " characters"
            );
        }
        // embedAll() method input type is segment so we convert input data to segment okay 
        List<TextSegment> segment =texts
        		.stream()
        		.map((text)->TextSegment.from(text))//or i can write it as map(TextSegment::from) in short hand 
        		.toList();
        long startTime = System.nanoTime();// i'm capture start time before Gemini call
        try {
           
            Response<List<Embedding>> response=embeddingModel.embedAll(segment);
            //now we want to content from the embedding 
            List<Embedding> embeddings=response.content();
            // after getting embedding we want the embedding in the form of Vectorlist becuase we want in datastudeture
            List<List<Float>> allVectors=embeddings.stream().map((emb)->emb.vectorAsList()).toList();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("embedBatch completed: {} vectors in {} ms", allVectors.size(), durationMs);
    	return allVectors;
        }catch(RuntimeException e)
        {
        	long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Gemini call failed after {} ms: {}", durationMs, e.getMessage(), e);
            throw e;
        }
    }
    
    
    
}
