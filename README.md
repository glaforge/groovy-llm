## Apache Groovy documentation chatbot powered by Gemini

This demo application lets users ask questions about the [Apache Groovy](https://groovy-lang.org/) documentation.
It is a chatbot powered by the [Gemini](https://deepmind.google/technologies/gemini/) Large Language Model created by Google.
The application is developed using the Groovy programming language, and the [Micronaut](https://micronaut.io) framework.
The frontend is built using [Vue.js](https://vuejs.org/).

## Trying this chatbot

To interact with this chatbot, you can use the this link: 
[Apache Groovy LLM Doc Chatbot](https://docchat-lpj6s2duga-ew.a.run.app/)

## Implementation details

This chatbot application implements the Retrieval Augmented Generation (RAG) approach:
* a document **ingestion** phase is loading, parsing, chunking the documentation, and is creating vector embeddings
  to store them in an in-memory vector database
* at **query time**, the application calculates a vector embedding, and compares it to the other vector embeddings 
  in the vector database to find the most similar excerpts

This projects uses the [LangChain4J](https://github.com/langchain4j) LLM orchestration library 
to interact with the PaLM LLM and the in-memory vector database (also provided by LangChain4j).

