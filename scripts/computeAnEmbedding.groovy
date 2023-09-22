@Grab('com.google.cloud:google-cloud-aiplatform:3.24.0')
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings
import com.google.cloud.aiplatform.v1beta1.EndpointName
import com.google.cloud.aiplatform.util.ValueConverter
import com.google.protobuf.Value
import com.google.protobuf.Struct

String project = "genai-java-demos"
String location = "us-central1"
String publisher = "google"
String model = "textembedding-gecko@001"

def helloValue = Value.newBuilder()
        .setStructValue(
                Struct.newBuilder()
                        .putFields('content', Value.newBuilder()
                                .setStringValue("Normal classes refer to classes which are top level and concrete. This means they can be instantiated without restrictions from any other classes or scripts. This way, they can only be public (even though the public keyword may be suppressed). Classes are instantiated by calling their constructors, using the new keyword, as in the following snippet.")
                                .build())
                        .build()
        ).build()


def predictionServiceSettings = PredictionServiceSettings.newBuilder().setEndpoint("${location}-aiplatform.googleapis.com:443").build()
def predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)
def endpointName = EndpointName.ofProjectLocationPublisherModelName(project, location, publisher, model)

def instances = [helloValue]

def predictResponse = predictionServiceClient.predict(endpointName, instances, ValueConverter.EMPTY_VALUE)
println predictResponse

println predictResponse.predictionsList.first()
        .structValue.fieldsMap['embeddings']
        .structValue.fieldsMap['values']
        .listValue.valuesList.collect { it.numberValue } as double[]

