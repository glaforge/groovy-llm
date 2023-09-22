@Grab('com.google.cloud:google-cloud-aiplatform:3.24.0')
import com.google.cloud.aiplatform.v1beta1.*
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat

String instance = '''{ "prompt": "Tell me more about Large Language Models"}'''
String parameters = '''{
  "temperature": 0.2,
  "maxOutputTokens": 256,
  "topP": 0.95,
  "topK": 40
}'''

String project = "genai-java-demos"
String location = "us-central1"
String publisher = "google"
String model = "text-bison"

def predictionServiceSettings = PredictionServiceSettings.newBuilder().setEndpoint("${location}-aiplatform.googleapis.com:443").build()

def predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)
def endpointName = EndpointName.ofProjectLocationPublisherModelName(project, location, publisher, model)

def instanceValue = Value.newBuilder()
JsonFormat.parser().merge(instance, instanceValue)
def instances = [instanceValue.build()]

def parameterValueBuilder = Value.newBuilder()
JsonFormat.parser().merge(parameters, parameterValueBuilder)
def parameterValue = parameterValueBuilder.build()

def predictResponse = predictionServiceClient.predict(endpointName, instances, parameterValue)
println predictResponse
println predictResponse.predictionsList.first().structValue.fieldsMap['content'].stringValue // resp[0].content
