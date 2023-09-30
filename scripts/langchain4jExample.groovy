//@Grab('dev.langchain4j:langchain4j:0.22.0')
@Grab('dev.langchain4j:langchain4j-vertex-ai:0.22.0')
import dev.langchain4j.model.vertexai.*

VertexAiChatModel vertexAiChatModel = VertexAiChatModel.builder()
        .endpoint("us-central1-aiplatform.googleapis.com:443")
        .project("genai-java-demos")
        .location("us-central1")
        .publisher("google")
        .modelName("chat-bison@001")
        .temperature(1.0)
//        .maxOutputTokens(50)
//        .topK(0)
//        .topP(0.0)
//        .maxRetries(3)
        .build();

def response = vertexAiChatModel.sendUserMessage("What is the best Large Language Model?");
println response.text()
