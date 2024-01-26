@Grab('dev.langchain4j:langchain4j-vertex-ai:0.25.0')
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel

@Grab('dev.langchain4j:langchain4j:0.24.0')
import dev.langchain4j.data.document.FileSystemDocumentLoader
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.data.document.transformer.HtmlTextExtractor
import dev.langchain4j.model.output.Response

import java.nio.file.Paths
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
        .modelName("textembedding-gecko@latest")
        .build()
def embeddingStore = new InMemoryEmbeddingStore()

def savedStorePath = Paths.get(GENERATED_DIR, SAVED_FILENAME)

println "[${now().format('kk:mm:ss')}] Splitting document and creating in-memory embedding store"
def ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(1500, 500))
        .embeddingModel(vertexEmbeddingModel)
        .embeddingStore(embeddingStore)
        .build()
ingestor.ingest(document)

println "[${now().format('kk:mm:ss')}] Saving vector embeddings file"
embeddingStore.serializeToFile(savedStorePath)
