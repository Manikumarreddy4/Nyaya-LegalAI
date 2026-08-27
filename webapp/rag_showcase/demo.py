import os
import sys
from retriever import RAGRetriever

def print_menu():
    print("\n" + "=" * 40)
    print("      NYAYA LEGALAI RAG SHOWCASE")
    print("=" * 40)
    print("1. Enter a legal question")
    print("2. View dataset information")
    print("3. View RAG architecture")
    print("4. Exit")
    print("=" * 40)

def view_dataset_info(retriever):
    print("\n" + "=" * 40)
    print("           DATASET INFORMATION")
    print("=" * 40)
    if not retriever.db:
        print("[Status] Database not ingested yet. Please run ingest.py first.")
        return
        
    chunks = retriever.db.get("chunks")
    mode = retriever.db.get("mode")
    
    print(f"Primary Source: constitution_cleaned_dataset.json")
    print(f"Document Type: CONSTITUTION OF INDIA")
    print(f"Total Chunks Ingested: {len(chunks)}")
    print(f"Embedding Engine Mode: {mode.upper()}")
    print(f"Vector Database Location: webapp/rag_showcase/vector_store/db.pkl")
    print("\nSample Ingested Document Metadata:")
    if chunks:
        sample = chunks[0]["metadata"]
        print(f"  - Title: {sample['title']}")
        print(f"  - Document Type: {sample['doc_type']}")
        print(f"  - Source file: {sample['source']}")
    print("=" * 40)

def view_architecture():
    arch_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "architecture.txt"))
    print("\n" + "=" * 40)
    if os.path.exists(arch_file):
        with open(arch_file, "r", encoding="utf-8") as f:
            print(f.read())
    else:
        print("Architecture diagram file not found.")
    print("=" * 40)

def synthesize_rag_response(retrieved_results):
    if not retrieved_results:
        return "I could not find any matching legal documents in the vector database to answer your question."
        
    top_result = retrieved_results[0]
    chunk_text = top_result["chunk"]
    meta = top_result["metadata"]
    
    # Simple semantic extraction from the formatted chunk:
    # "Article X: Title. Explanation: [explanation]. Example: [example]."
    explanation = "No detailed explanation found."
    example = ""
    
    if "Explanation:" in chunk_text:
        parts = chunk_text.split("Explanation:")
        explanation = parts[1].split("Example:")[0].strip()
        if "Example:" in parts[1]:
            example = parts[1].split("Example:")[1].strip()
            
    response_text = f"According to the Constitution of India (Article {meta['article_number']} - {meta['title']}):\n"
    response_text += f"{explanation}\n"
    if example and example != "No specific example":
        response_text += f"\nIllustration: {example}"
        
    return response_text

def query_rag(retriever):
    question = input("\nEnter your legal question: ").strip()
    if not question:
        return
        
    print("\n[RAG] Embedding question and searching vector database...")
    results = retriever.retrieve(question, top_k=2)
    
    if not results:
        print("\n[RAG] No relevant context retrieved above the threshold score.")
        return
        
    # Print the retrieved results (Similarity Search output)
    retriever.print_search_results(question, results)
    
    # Generate RAG response based ONLY on retrieved context
    response = synthesize_rag_response(results)
    
    print("\n" + "=" * 40)
    print("RAG DEMONSTRATION RESPONSE")
    print("=" * 40)
    print(response)
    print("=" * 40)
    print("\nRetrieved From:")
    for r in results:
        meta = r["metadata"]
        print(f" - File: {meta['source']} | Article: {meta['article_number']} (Score: {r['score']:.4f})")
    print("=" * 40)

def main():
    retriever = RAGRetriever()
    
    while True:
        print_menu()
        choice = input("Select an option (1-4): ").strip()
        
        if choice == "1":
            query_rag(retriever)
        elif choice == "2":
            view_dataset_info(retriever)
        elif choice == "3":
            view_architecture()
        elif choice == "4":
            print("\nExiting RAG Showcase. Goodbye!")
            sys.exit(0)
        else:
            print("\nInvalid choice. Please enter a number between 1 and 4.")

if __name__ == "__main__":
    main()
