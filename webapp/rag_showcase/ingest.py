import os
import json
import pickle
import numpy as np

# Try importing sentence_transformers
try:
    from sentence_transformers import SentenceTransformer
    SENTENCE_TRANSFORMERS_AVAILABLE = True
except ImportError:
    SENTENCE_TRANSFORMERS_AVAILABLE = False

# Try importing sklearn
try:
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.metrics.pairwise import cosine_similarity
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False

def ingest_documents():
    print("==================================================")
    print("RAG Showcase Dataset Processing Started")
    print("==================================================")
    
    docs_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "documents"))
    vector_store_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "vector_store"))
    os.makedirs(vector_store_dir, exist_ok=True)
    
    # 1. Identify files
    files = [f for f in os.listdir(docs_dir) if f.endswith(".json")]
    print(f"Files found: {len(files)}")
    
    chunks = []
    documents_processed = 0
    
    for filename in files:
        filepath = os.path.join(docs_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            try:
                data = json.load(f)
            except Exception as e:
                print(f"[Error] Failed to read {filename}: {e}")
                continue
                
            documents_processed += 1
            
            for idx, record in enumerate(data):
                # Formulate semantic chunk
                number = record.get("number", str(idx + 1))
                title = record.get("title", "Legal Section")
                explanation = record.get("explanation", "")
                example = record.get("example", "")
                
                chunk_text = f"Article {number}: {title}. Explanation: {explanation}. Example: {example}."
                
                chunks.append({
                    "text": chunk_text,
                    "metadata": {
                        "source": filename,
                        "article_number": number,
                        "title": title,
                        "chunk_number": idx + 1,
                        "doc_type": record.get("type", "CONSTITUTION")
                    }
                })
                
    print(f"Documents processed: {documents_processed}")
    print(f"Text chunks created: {len(chunks)}")
    
    if len(chunks) == 0:
        print("[Error] No text chunks created. Ingestion stopped.")
        return
        
    # 2. Embedding generation & Vector store creation
    db = {
        "chunks": chunks,
        "mode": "fallback_sparse",
        "embeddings": None,
        "vectorizer": None
    }
    
    # Mode selection
    use_dense = SENTENCE_TRANSFORMERS_AVAILABLE
    
    if use_dense:
        print("[EmbeddingEngine] sentence-transformers detected. Initializing dense embeddings...")
        try:
            model = SentenceTransformer('all-MiniLM-L6-v2')
            texts = [c["text"] for c in chunks]
            embeddings = model.encode(texts, show_progress_bar=True)
            db["embeddings"] = embeddings
            db["mode"] = "dense"
            print(f"Embeddings generated: {len(embeddings)}")
        except Exception as e:
            print(f"[Warning] Failed to generate dense embeddings: {e}. Falling back to TF-IDF vectorizer...")
            use_dense = False
            
    if not use_dense:
        if SKLEARN_AVAILABLE:
            print("[EmbeddingEngine] Using scikit-learn TF-IDF Vectorizer for local sparse embeddings...")
            vectorizer = TfidfVectorizer(stop_words='english')
            texts = [c["text"] for c in chunks]
            embeddings = vectorizer.fit_transform(texts)
            db["embeddings"] = embeddings
            db["vectorizer"] = vectorizer
            db["mode"] = "sparse"
            print(f"Embeddings generated: {embeddings.shape[0]}")
        else:
            print("[Error] Neither sentence-transformers nor scikit-learn is available. Please run: pip install -r requirements.txt")
            return
            
    # 3. Save database
    db_file = os.path.join(vector_store_dir, "db.pkl")
    with open(db_file, "wb") as f:
        pickle.dump(db, f)
        
    print("Vector database created successfully")
    print(f"Saved database to: {db_file}")
    print("==================================================")

if __name__ == "__main__":
    ingest_documents()
