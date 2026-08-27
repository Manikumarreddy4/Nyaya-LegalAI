import os
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
    from sklearn.metrics.pairwise import cosine_similarity
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False

class RAGRetriever:
    def __init__(self):
        self.db_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "vector_store/db.pkl"))
        self.db = None
        self.model = None
        self.load_database()

    def load_database(self):
        if not os.path.exists(self.db_file):
            print(f"[Retriever Error] Database file not found at {self.db_file}. Please run ingest.py first.")
            return False
            
        with open(self.db_file, "rb") as f:
            self.db = pickle.load(f)
            
        # If the database was created using dense mode, load the sentence transformer model
        if self.db.get("mode") == "dense":
            if SENTENCE_TRANSFORMERS_AVAILABLE:
                try:
                    self.model = SentenceTransformer('all-MiniLM-L6-v2')
                except Exception as e:
                    print(f"[Retriever Warning] Failed to load dense embedding model: {e}")
            else:
                print("[Retriever Error] Database is dense but sentence-transformers is missing!")
                
        return True

    def retrieve(self, question: str, top_k: int = 3):
        if not self.db:
            if not self.load_database():
                return []
                
        mode = self.db.get("mode")
        chunks = self.db.get("chunks")
        embeddings = self.db.get("embeddings")
        
        results = []
        
        if mode == "dense" and self.model:
            # 1. Encode query
            query_vector = self.model.encode([question])[0]
            
            # 2. Compute similarity
            scores = []
            for emb in embeddings:
                # Cosine similarity for normalized vectors is dot product
                dot_product = np.dot(query_vector, emb)
                norm_q = np.linalg.norm(query_vector)
                norm_e = np.linalg.norm(emb)
                sim = dot_product / (norm_q * norm_e + 1e-9)
                scores.append(float(sim))
                
        elif mode == "sparse" and SKLEARN_AVAILABLE:
            vectorizer = self.db.get("vectorizer")
            # 1. Transform query
            query_vector = vectorizer.transform([question])
            
            # 2. Compute similarity
            sim_matrix = cosine_similarity(query_vector, embeddings)
            scores = sim_matrix[0].tolist()
            
        else:
            print("[Retriever Error] Incompatible database mode or dependency missing.")
            return []
            
        # 3. Sort indices by score desc
        ranked_indices = np.argsort(scores)[::-1][:top_k]
        
        for idx in ranked_indices:
            score = scores[idx]
            # Ignore zero similarity in fallback mode if they have no overlapping words
            if score < 0.01 and mode == "sparse":
                continue
            results.append({
                "chunk": chunks[idx]["text"],
                "metadata": chunks[idx]["metadata"],
                "score": score
            })
            
        return results

    def print_search_results(self, question: str, results):
        print("\n========================================")
        print("QUESTION:")
        print(question)
        print("========================================")
        print(f"RETRIEVED RESULTS: (Found {len(results)} matches)\n")
        
        for idx, res in enumerate(results):
            meta = res["metadata"]
            print(f"Result {idx + 1}")
            print(f"Source: {meta['source']}")
            print(f"Chunk: {meta['chunk_number']}")
            print(f"Article Number: {meta['article_number']}")
            print(f"Relevance Score: {res['score']:.4f}")
            print("\nRetrieved Context:")
            print(res["chunk"])
            print("-" * 40)
            
if __name__ == "__main__":
    retriever = RAGRetriever()
    # Test query
    q = "What should a person do in case of territory of India?"
    res = retriever.retrieve(q, top_k=2)
    retriever.print_search_results(q, res)
