package com.google.cloud.devrel.docchat;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.retriever.Retriever;

import java.util.List;

public class EmbeddingStoreRetrieverWrapper implements Retriever<TextSegment> {

    private final EmbeddingStoreRetriever retriever;

    public EmbeddingStoreRetrieverWrapper(EmbeddingStoreRetriever retriever) {
        this.retriever = retriever;
    }

    @Override
    public List<TextSegment> findRelevant(String text) {
        List<TextSegment> relevant = retriever.findRelevant(text);
        for (TextSegment textSegment : relevant) {
            System.out.println("\n=======================================================\n");
            System.out.println("textSegment = " + textSegment);
        }
        return relevant;
    }
}
