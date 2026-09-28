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
        //and embedAll() method take the list the of input text anf first we convert the text into text sqgment becaue 
        //embedALl() method input type is TextSegment but 
        //why text segment....?
        //the data segment is not only contain the our text data but also contain the related information about 
        // that text menas metadata okay 
        // metadata means where the data is comming from a document name , page no etc ...okay 
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // pull the number list out of each embedding
       // return embeddings.stream()
         //       .map(Embedding::vectorAsList)//.map(Embedding.)
          //     .toList();
        return embeddings.stream()
        		.map((emb)->emb.vectorAsList())
        		.toList();
    }
}

/// drawbacks of current this microservice is that even the same text comes it again do the same
/// thing means embedding so we want to build some logic if same text comes agin then don't do embedding so it sawes out 
/// token and api hit call okay 
/// 2.> suppose by any reason my llm goes down or my free plan finished then what i need backup plan or 
/// auto model switch way okay rajesh means retry logic okay 
/// so we add text--> check is google embedding model working good or not if yes then go if not change to another 
/// .3. is no input check what if someone send the empty string then so we need to handle that thing also 
///4 and is most valuable and important concept what if book/pdf size is so big and we have 11000 cheunks then 
/// that time i thing my api means google api goes down and it will throw may exception okay  
/// so i need to divide the data into batches okay 
/// 5. and we need to add one thing that take care when when this service is run what is input 
/// token how many its time ,date how many requests api latency and errors okay and its response and request speed 
/// okay 
/// 6 is the most import part is it is synchronous means suppose it call gimini and wait its response means one work at 
/// a time okay and this is the waste of time and resource okay 
/// and we want to do something okay 	 
/// 
/// 
