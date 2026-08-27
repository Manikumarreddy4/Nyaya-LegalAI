# Nyaya LegalAI - RAG Demonstration Module

This directory contains a completely separate, independent Retrieval-Augmented Generation (RAG) showcase module built for demonstration and presentation purposes.

> [!IMPORTANT]
> **This RAG module is implemented as an independent demonstration component and is not connected to the current production Web or Android chatbot implementation.** The production AI APIs and chatbot flow remain completely untouched.

---

## 1. What is RAG?
**RAG (Retrieval-Augmented Generation)** is an architectural pattern that extends the capability of a Large Language Model (LLM) by retrieving relevant facts from an external knowledge base (such as legal datasets or constitutional documents) and appending them to the user's query context before generating a response.

---

## 2. Why is RAG Used?
* **Reduces Hallucinations**: Since responses are grounded in verified legal datasets, the model is less prone to generating incorrect information.
* **Domain-Specific Knowledge**: Allows generic AI models to answer specialized legal questions by referencing actual acts, constitutions, or case studies.
* **Traceable Sources**: The user can see exactly which article, section, or document was used to produce the response.

---

## 3. RAG Showcase Architecture
```text
                 NYAYA LEGALAI
                  RAG SHOWCASE

        Legal Dataset / Documents
                    │
                    ▼
             Document Loader
                    │
                    ▼
               Text Chunking
                    │
                    ▼
                Embeddings
                    │
                    ▼
             Vector Database
                    │
                    ▲
                    │
User Question ──► Question Embedding
                    │
                    ▼
             Similarity Search
                    │
                    ▼
           Relevant Legal Context
                    │
                    ▼
          RAG Demonstration Response
```

---

## 4. Module Files Reference

* **[`data_loader.py`](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/webapp/rag_showcase/data_loader.py)**: Locates the repository's native dataset files (`constitution_cleaned_dataset.json`), reads them, and stages a reference copy inside `documents/`.
* **[`ingest.py`](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/webapp/rag_showcase/ingest.py)**: Handles text extraction, splits the articles into meaningful chunks with metadata preserved (source file, article number, title), generates embeddings, and builds the local vector database.
  * *Dual-Mode Architecture*: Implements `sentence-transformers` for dense semantic embeddings, but includes a built-in lightweight local vector fallback (`sklearn` Vectorizer) to ensure instant execution on machines with slow internet or no GPU acceleration.
* **[`retriever.py`](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/webapp/rag_showcase/retriever.py)**: Accepts a user query, translates it to the embedding space, runs a cosine-similarity matrix search against the vector database, and returns the highest scoring context blocks.
* **[`demo.py`](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/webapp/rag_showcase/demo.py)**: An interactive CLI presentation dashboard to show to faculty. Displays statistics, retrieves references, and generates a structured RAG demonstration response.

---

## 5. Execution Commands

To run the demonstration:

1. **Navigate to the Directory**:
   ```bash
   cd webapp/rag_showcase
   ```

2. **Install Dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Stage Dataset Documents**:
   ```bash
   python data_loader.py
   ```

4. **Run Dataset Ingestion**:
   ```bash
   python ingest.py
   ```

5. **Start RAG Showcase CLI Demo**:
   ```bash
   python demo.py
   ```
