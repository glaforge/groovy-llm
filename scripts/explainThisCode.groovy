@Grab('dev.langchain4j:langchain4j-vertex-ai:0.22.0')
import dev.langchain4j.model.vertexai.*

def langModel = VextexAiLanguageModel.builder()
        .endpoint("us-central1-aiplatform.googleapis.com:443")
        .project("genai-java-demos")
        .location("us-central1")
        .publisher("google")
        .modelName("text-bison")
        .temperature(0.6)
        .maxOutputTokens(300)
        .topK(20)
        .topP(0.8)
        .maxRetries(3)
        .build();

def thisScriptFile = new File(this.class.protectionDomain.codeSource.location.path).text

println langModel.process("Please explain the following Groovy code: \n\n${thisScriptFile}")