import os
import shutil
import json

def load_data():
    print("==================================================")
    print("RAG Showcase Data Loader Started")
    print("==================================================")
    
    # 1. Target source path
    src_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "../public/assets/constitution_cleaned_dataset.json"))
    
    # Check fallback path in case runner is in root
    if not os.path.exists(src_file):
        src_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../app/src/main/assets/constitution_cleaned_dataset.json"))
        
    print(f"Source file identified: {src_file}")
    
    if not os.path.exists(src_file):
        print("[Error] Source dataset not found. Please ensure 'constitution_cleaned_dataset.json' exists.")
        return
        
    # 2. Destination directory
    dest_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "documents"))
    os.makedirs(dest_dir, exist_ok=True)
    
    dest_file = os.path.join(dest_dir, "constitution_cleaned_dataset.json")
    
    # 3. Copy file
    shutil.copy2(src_file, dest_file)
    print(f"File copied successfully to: {dest_file}")
    
    # 4. Read to verify content
    with open(dest_file, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    print(f"Total legal records in copied dataset: {len(data)}")
    print("RAG Showcase Data Loader Complete.")
    print("==================================================")

if __name__ == "__main__":
    load_data()
