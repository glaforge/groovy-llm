@Grab('org.slf4j:slf4j-api:2.0.9')
@Grab('net.dankito.readability4j:readability4j:1.0.8')
import net.dankito.readability4j.extended.*

def url = 'https://glaforge.dev/posts/2023/07/06/custom-environment-variables-in-workflows/'
def html = url.toURL().text
def readability4J = new Readability4JExtended('.', html)
def article = readability4J.parse()

println article.title
println article.textContent
