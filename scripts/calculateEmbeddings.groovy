@Grab('dev.langchain4j:langchain4j-vertex-ai:0.22.0')
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel

@Grab('dev.langchain4j:langchain4j:0.22.0')
import dev.langchain4j.data.document.FileSystemDocumentLoader
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.data.document.transformer.HtmlTextExtractor

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.concurrent.ForkJoinPool
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import static java.time.LocalTime.now

final GENERATED_DIR = './generated/'
final DOC_FILENAME = 'groovy-documentation.html'
final SAVED_FILENAME = 'saved-embedding-store.json'

Paths.get(GENERATED_DIR).toFile().mkdirs()

println "[${now().format('kk:mm:ss')}] Fetching latest documentation"
def fetchedHtmlDocument = Paths.get(GENERATED_DIR, DOC_FILENAME)
fetchedHtmlDocument << new URL('https://docs.groovy-lang.org/latest/html/documentation/').text

println "[${now().format('kk:mm:ss')}] Loading documentation"
def document = new HtmlTextExtractor('#content', [:], true)
        .transform(FileSystemDocumentLoader.loadDocument(fetchedHtmlDocument))

def vertexEmbeddingModel = VertexAiEmbeddingModel.builder()
        .endpoint("us-central1-aiplatform.googleapis.com:443")
        .project("genai-java-demos")
        .location("us-central1")
        .publisher("google")
        .modelName("textembedding-gecko@001")
        .build()
def embeddingModel = new BatchedEmbeddingModel(vertexEmbeddingModel, 5, 4, 100)
def embeddingStore = new InMemoryEmbeddingStore()

def savedStorePath = Paths.get(GENERATED_DIR, SAVED_FILENAME)

println "[${now().format('kk:mm:ss')}] Splitting document and creating in-memory embedding store"
def ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(500))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build()
ingestor.ingest(document)

println "[${now().format('kk:mm:ss')}] Saving vector embeddings file"
embeddingStore.serializeToFile(savedStorePath)
/*
println "[${now().format('kk:mm:ss')}] Zipping vector embeddings file"
try(def fos = new FileOutputStream(GENERATED_DIR + SAVED_FILENAME + '.zip');
    def zos = new ZipOutputStream(fos)) {
    def zipEntry = new ZipEntry(SAVED_FILENAME)
    zos.putNextEntry(zipEntry)
    zos << Files.newInputStream(savedStorePath)
}
*/

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