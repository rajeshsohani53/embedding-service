package com.raj.embeddingservice.v2.service.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
@Data
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    private int maxBatchSize;
    private int maxTextLength;
    private String modelName;
    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

	public int getMaxTextLength() {
		return maxTextLength;
	}

	public void setMaxTextLength(int maxTextLength) {
		this.maxTextLength = maxTextLength;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
    

}