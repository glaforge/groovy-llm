@Grab('dev.langchain4j:langchain4j-vertex-ai:0.22.0')
import dev.langchain4j.model.vertexai.*

@Grab('dev.langchain4j:langchain4j:0.22.0')
import dev.langchain4j.data.document.FileSystemDocumentLoader
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.chain.ConversationalRetrievalChain
import dev.langchain4j.retriever.EmbeddingStoreRetriever
import dev.langchain4j.model.input.PromptTemplate

//@Grab('dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.22.0')
//import dev.langchain4j.model.inprocess.InProcessEmbeddingModel
//import static dev.langchain4j.model.inprocess.InProcessEmbeddingModelType.ALL_MINILM_L6_V2

import java.nio.file.Paths
import java.nio.file.Files
import java.util.stream.Collectors
import java.util.concurrent.ForkJoinPool

def fetchedHtmlDocument = Paths.get('./generated', 'groovy-documentation.html')
new URL('https://groovy-lang.org/single-page-documentation.html').text >> fetchedHtmlDocument

def document = FileSystemDocumentLoader.loadDocument(fetchedHtmlDocument);

def vertexEmbeddingModel = VertexAiEmbeddingModel.builder()
        .endpoint("us-central1-aiplatform.googleapis.com:443")
        .project("genai-java-demos")
        .location("us-central1")
        .publisher("google")
        .modelName("textembedding-gecko@001")
        .build()
def embeddingModel = new BatchedEmbeddingModel(vertexEmbeddingModel, 5, 4, 100)
//def embeddingModel = new InProcessEmbeddingModel(ALL_MINILM_L6_V2)

def embeddingStore = new InMemoryEmbeddingStore()

def savedStorePath = Paths.get('./generated', 'saved-embedding-store.json')
if (Files.exists(savedStorePath)) {
    println 'Loading from saved storage'
    embeddingStore = InMemoryEmbeddingStore.fromFile(savedStorePath)
} else {
    println 'Creating in-memory embedding store'
    def ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(DocumentSplitters.recursive(500))
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();
    ingestor.ingest(document)
    embeddingStore.serializeToFile(savedStorePath)
}

def vertexAiChatModel = VertexAiChatModel.builder()
        .endpoint("us-central1-aiplatform.googleapis.com:443")
        .project("genai-java-demos")
        .location("us-central1")
        .publisher("google")
        .modelName("chat-bison@001")
        .temperature(0.3)
//        .maxOutputTokens(50)
//        .topK(0)
//        .topP(0.0)
//        .maxRetries(3)
        .build();

ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
        .chatLanguageModel(vertexAiChatModel)
        .promptTemplate(PromptTemplate.from('''\
        You are an expert in the Apache Groovy programming language. 
        You are also knowledgeable in the Java language, but be sure to write idiomatic Groovy code in your answers.
        You excel at teaching and explaining concepts of the language.
        If you don't know the answer to the question, say that you don't know the answer, and that the user should refer to the Groovy documentation.
        
        Answer the following question to the best of your ability: 
        Using the Groovy language, {{question}}
        
        Base your answer exclusively on the following information from the Groovy documentation:
        
        {{information}}'''.stripIndent()))
        .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
// .chatMemory() // you can override default chat memory
// .promptTemplate() // you can override default prompt template
        .build();

//println chain.execute("Show me an example of a record implemented in Groovy.")
//println chain.execute("How can I collect items of a list in parallel in Groovy?")

10.times {
    print   '==========================================================================================================\n>>> '
    String query = System.console().readLine()
    println '----------------------------------------------------------------------------------------------------------'
    println chain.execute(query)
}


class BatchedEmbeddingModel implements EmbeddingModel {
    EmbeddingModel embeddingModel
    int batchSize = 1
    int numberOfThreads = 1
    int paddingTimeMillis = 0

    BatchedEmbeddingModel(EmbeddingModel embeddingModel, int batchSize, int numberOfThreads, int paddingTimeMillis) {
        this.embeddingModel = embeddingModel
        this.batchSize = batchSize
        this.numberOfThreads = numberOfThreads
        this.paddingTimeMillis = paddingTimeMillis
    }

    List<Embedding> embedAll(List<TextSegment> textSegments) {
        def response
        new ForkJoinPool(numberOfThreads).submit { ->
            response = textSegments.collate(batchSize)
                    .parallelStream().map { batchSegments ->
                        def result = embeddingModel.embedAll(batchSegments)
                        sleep paddingTimeMillis
                        return result
                    }.collect(Collectors.toList())
                    .flatten()
        }.get()
        return response
    }
}