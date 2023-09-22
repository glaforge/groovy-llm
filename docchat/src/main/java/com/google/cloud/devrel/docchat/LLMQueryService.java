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
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
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

    private final VertexAiChatModel chatModel;

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    private final ConversationalRetrievalChain chain;

    private final InMemoryChatMemoryStore chatMemoryStore;

    private final MessageWindowChatMemory chatMemory;

    public LLMQueryService() {
        this.embeddingModel = VertexAiEmbeddingModel.builder().endpoint("us-central1-aiplatform.googleapis.com:443").project("genai-java-demos").location("us-central1").publisher("google").modelName("textembedding-gecko@001").maxRetries(3).build();

        this.chatModel = VertexAiChatModel.builder()
            .endpoint("us-central1-aiplatform.googleapis.com:443")
            .project("genai-java-demos")
            .location("us-central1")
            .publisher("google")
            .modelName("chat-bison@001")
            .temperature(0.1)
            .maxRetries(1)
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

        this.from = PromptTemplate.from("You are an expert in the Apache Groovy programming language.\n" +
            "You are also knowledgeable in the Java language, but be sure to write idiomatic Groovy code in your answers.\n" +
            "You excel at teaching and explaining concepts of the language.\n" +
            "If you don't know the answer to the question, say that you don't know the answer, and that the user should refer to the Groovy documentation.\n" +
            "Answer the following question to the best of your ability:\n\n" +
            "Using the Groovy language, {{question}}\n\n" +
            "Base your answer exclusively on the following information from the Groovy documentation:\n\n" + "{{information}}))\n\n" /* +
                    "In your answers, make sure to quote the above information that lead to your answer by appending them as a reference." */);

        this.chatMemoryStore = new InMemoryChatMemoryStore();

        this.chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(11)
            .build();

        this.chain = ConversationalRetrievalChain.builder()
            .chatLanguageModel(chatModel)
            .chatMemory(chatMemory)
            .promptTemplate(from)
            .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel)).build();

        System.out.println("Groovy knowledge ready");
    }

    public String execute(String query) {
        String response = chain.execute(query);
        System.out.println("response = " + response);

        return renderMarkdownToHtml(response);
    }

    public String executeWithMemory(String query, String chatId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .id(chatId)
            .chatMemoryStore(chatMemoryStore)
            .maxMessages(11)
            .build();

        System.out.println("chatMemory (" + chatId + ") size = " + chatMemory.messages().size());

        ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
            .chatLanguageModel(chatModel)
            .chatMemory(chatMemory)
            .promptTemplate(from)
            .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel)).build();

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
