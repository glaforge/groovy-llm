@Grab('org.jsoup:jsoup:1.16.1')
import org.jsoup.*
import org.jsoup.parser.*
import org.jsoup.nodes.*
import org.jsoup.select.*
import groovy.json.*
import groovy.transform.*

def htmlDoc = new File('groovy-documentation.html').text

def contentStartIdx = htmlDoc.indexOf('<div id="content">')
htmlDoc = htmlDoc.substring(contentStartIdx)

List<String> splitsBy(String input, String prefix) {
    def parts = []
    def currentIndex = 0
    while (currentIndex < input.size() - prefix.size()) {
		def foundIndex = input.indexOf(prefix, currentIndex + prefix.size())
        if (foundIndex == -1 || foundIndex == currentIndex) {
            parts << input.substring(currentIndex)
            break
        } else {
            parts << input.substring(currentIndex, foundIndex)
            currentIndex = foundIndex
        }
    }
    return parts
}

def iterativeSplits(String input, List<String> allPrefixes, int maxToken = 1000) {
    def splits = [input]
    for (String aPrefix: allPrefixes) {
        splits = splits.collect { splitsBy(it, aPrefix) }.flatten()
    }
    return splits
}

@Canonical
class HeaderTag {
    int level
    String text
    String link
    String toString() { "${level} — ${text}" }
}
@Canonical
class Snippet {
    String text
    HeaderTag header
}

def headings = []
def allExtracts = iterativeSplits(htmlDoc, ['<h1', '<h2', '<h3', '<h4', '<h5', '<h6', '<h7', '<h8', '<table', '</table', '<pre', '</pre', '<tr']).collect {
    def subDocument = Jsoup.parse(it)

    def accum = new StringBuilder()
    NodeTraversor.traverse(new NodeVisitor() {
        @Memoized
        private static boolean hasPreParent(Node node) {
            if (!node.hasParent()) return false
            def parent = node.parent()
            if (parent.isNode('pre')) return true
            else return hasPreParent(parent)
        }

        void head(Node node, int depth) {
            def headerMatcher = node.nodeName() =~ /h(\d)/
            if (headerMatcher.matches()) {
                def (_, headerDepth) = headerMatcher[0]
                def newHeader = new HeaderTag(
                    headerDepth as int,
                    node.text(),
                    node.select('[href]')?[0]?.attr('href')
                )

                if (headings.size() > 0) {
                    def last = headings[-1]
                    if (newHeader.level > last.level) {
                        headings << newHeader
                    } else if (newHeader.level == last.level) {
                        headings.removeLast()
                        headings << newHeader
                    } else {
                        while (headings.size() > 0 && newHeader.level <= last.level) {
                            headings.removeLast()
                            last = headings[-1]
                        }
                        headings << newHeader
                    }
                } else {
                    headings << newHeader
                }
            }

            if ((node instanceof TextNode || node.isNode('pre')) && !hasPreParent(node)) {
                accum.append(node.text())
            } else if (node instanceof Element && (node.isBlock() || node.isNode("br"))) {
                accum.append('\n')
            }
        }

        void tail(Node node, int depth) {
            if (node instanceof Element) {
                def next = node.nextSibling()
                if (node.isBlock() && !accum.endsWithAny('\n')) {
                    accum.append('\n\n')
                }
            }

        }
    }, subDocument)

    def text = accum.toString()
        .replaceAll(/( |\n)+\n/, '\n\n')
        .trim()

    return new Snippet(text, headings ? headings[-1] : null)
}


def jsonOutput = JsonOutput.toJson(allExtracts)
println jsonOutput