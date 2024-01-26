package com.google.cloud.devrel.docchat;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.io.Readable;
import io.micronaut.core.io.ResourceResolver;
import jakarta.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

@Singleton
public class LLMQueryService {

    private final PromptTemplate from;

    private final VertexAiEmbeddingModel embeddingModel;

    private final VertexAiGeminiChatModel geminiChatModel;

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    private final InMemoryChatMemoryStore chatMemoryStore;

    private final EmbeddingStoreRetriever retriever;

    public LLMQueryService() {
        this.embeddingModel = VertexAiEmbeddingModel.builder()
            .endpoint("us-central1-aiplatform.googleapis.com:443")
            .project("genai-java-demos")
            .location("us-central1")
            .publisher("google")
            .modelName("textembedding-gecko@001")
            .maxRetries(3)
            .build();

        this.geminiChatModel = VertexAiGeminiChatModel.builder()
            .project("genai-java-demos")
            .location("us-central1")
            .modelName("gemini-pro")
            .temperature(0.3f)
            .maxRetries(3)
            .build();

        Optional<URL> resource = new ResourceResolver().getResource("classpath:saved-embedding-store.json");
        Readable savedEmbeddingStore = Readable.of(resource.get());

        String storeJson = "{}";
        try {
            storeJson = IOUtils.readText(new BufferedReader(savedEmbeddingStore.asReader()));
            System.out.println("Read " + storeJson.length() + " bytes of saved embeddings");
        } catch (IOException ioe) {
            System.err.println("Impossible to read saved embeddings");
            ioe.printStackTrace();
        }

        this.embeddingStore = InMemoryEmbeddingStore.fromJson(storeJson);
        System.out.println("In-memory embedding store loaded");

        this.retriever = EmbeddingStoreRetriever.from(embeddingStore, embeddingModel);

        this.from = PromptTemplate.from("""
            You are an expert in the Apache Groovy programming language.
            You are also knowledgeable in the Java language, but be sure to write idiomatic Groovy code in your answers.
            You excel at teaching and explaining concepts of the language.
            If you don't know the answer to the question, say that you don't know the answer, and that the user should refer to the Groovy documentation.
            Answer the following question to the best of your ability:
                        
            Using the Groovy language, {{question}}
                        
            Base your answer exclusively on the following information from the Groovy documentation:
                        
            {{information}}))
            """);

        this.chatMemoryStore = new InMemoryChatMemoryStore();
    }

    public String executeWithMemory(String query, String chatId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryStore(chatMemoryStore)
            .id(chatId)
            .maxMessages(10)
            .build();

        ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
            .chatLanguageModel(geminiChatModel)
            .chatMemory(chatMemory)
            .promptTemplate(from)
            .retriever(retriever)
            .build();

        System.out.println("query = " + query);
        String response = chain.execute(query);

        System.out.println("response = " + response);
        return renderMarkdownToHtml(response);
    }

    private static String renderMarkdownToHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(markdown);

        return renderer.render(document);
    }
}
