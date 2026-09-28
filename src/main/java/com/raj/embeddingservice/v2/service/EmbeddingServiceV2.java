
package com.raj.embeddingservice.v2.service;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.raj.embeddingservice.v2.cache.EmbeddingCache;
import com.raj.embeddingservice.v2.service.config.EmbeddingProperties;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;

@Service
public class EmbeddingServiceV2 {
  private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceV2.class);
  private final EmbeddingModel embeddingModel;
  private final EmbeddingProperties properties;
  private static final int MAX_TEXT_LENGTH = 8000;
  private static final int MAX_BATCH_SIZE = 100;
  private final EmbeddingCache cache;
  /// google gemni embedding model and embed text to vector in 100 batch at a time
  /// ["hello", "world", "foo"]  →  batch size = 3
  /// ["a", "b", "c", ..., "z"]  →  batch size = 26
  /// 100 strings                →  batch size = 100  ✅ allowed
  /// 101 strings                →  batch size = 101  ❌ rejected
    public EmbeddingServiceV2 (@Value("${gemini.api.key}") String apiKey,EmbeddingProperties properties,EmbeddingCache cache)//we will get the api key frist
    {
    	// so first here we build the model 
    	GoogleAiEmbeddingModel googleModel=GoogleAiEmbeddingModel
    	.builder()
    	.apiKey(apiKey)
    	.modelName(properties.getModelName())
    	.build();	
    	this.embeddingModel=googleModel;
    	this.properties=properties;
    	this.cache=cache;
    }
    // for here we will update our EmbeddingService okay 
    /// because our googel gemeni limit is 2048 tokens at a time 
    /// and if we conside 1 token=1 charaacter then 2048 token means 8000 character okay 
    /// 
    /// ---------------------------------------------------

    
    @Retryable(
    	    noRetryFor = { IllegalArgumentException.class },
    	    maxAttempts = 3,
    	    backoff = @Backoff(delay = 1000, multiplier = 2)
    	)
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
        
        
        // Cache setup: results holds vectors in original order;
        // textsToEmbed collects cache misses; textIndexes tracks their original positions.
        List<Float>[] results = new List[texts.size()];
        List<String> textsToEmbed = new ArrayList<>();
        List<Integer> textIndexes = new ArrayList<>();
        
        
        // Split texts into cache hits and misses
        for (int i = 0; i < texts.size(); i++) {
            String oneText = texts.get(i);
            List<Float> cachedVector = cache.get(oneText);

            if (cachedVector != null) {
                // cache hit → put the vector directly in its slot
                results[i] = cachedVector;
            } else {
                // cache miss → remember this text and its position for Gemini
                textsToEmbed.add(oneText);
                textIndexes.add(i);
            }
        }
        
        // if all texts were cache hit then no need to call Gemini
        List<List<Float>> newVectors = new ArrayList<>();
        
        if(!textsToEmbed.isEmpty())
        {
	        // embedAll() method input type is segment so we convert input data to segment okay 
	        List<TextSegment> segment =textsToEmbed        		
	        		.stream()
	        		.map((text)->TextSegment.from(text))//or i can write it as map(TextSegment::from) in short hand 
	        		.toList();
	        long startTime = System.nanoTime();// i'm capture start time before Gemini call
	        try {
	           
	            Response<List<Embedding>> response=embeddingModel.embedAll(segment);
	            //now we want to content from the embedding 
	            List<Embedding> embeddings=response.content();
	            // extract only the NEW vectors from Gemini (not the whole result)
	            newVectors=embeddings.stream().map((emb)->emb.vectorAsList()).toList();
	            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
	            log.info("embedBatch: Gemini returned {} new vectors in {} ms", newVectors.size(), durationMs);
	        }catch(RuntimeException e)
	        {
	        	long durationMs = (System.nanoTime() - startTime) / 1_000_000;
	            log.error("Gemini call failed after {} ms: {}", durationMs, e.getMessage(), e);
	            throw e;
	        }
        }
        
        // now put the new vectors from Gemini into the right slot of results
        // and also save them in cache for next time okay 
        for(int j=0; j<newVectors.size(); j++)
        {
        	String text = textsToEmbed.get(j);
        	List<Float> vector = newVectors.get(j);
        	
        	// save in cache so next time this text is a hit
        	cache.put(text, vector);
        	
        	// put vector in its original position in results
        	int originalIndex = textIndexes.get(j);
        	results[originalIndex] = vector;
        }
        
        // finally return the results — has cache hits AND new vectors, all in original order
        return List.of(results);
    }
    
    
    
}