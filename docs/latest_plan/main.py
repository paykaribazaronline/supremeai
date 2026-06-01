from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import numpy as np

app = FastAPI(title="SupremeAI Semantic Router")

# Load the lightweight all-MiniLM-L6-v2 model
print("Loading all-MiniLM-L6-v2 model...")
model = SentenceTransformer("all-MiniLM-L6-v2")
print("Model loaded successfully!")

# Define basic intents and their anchor phrases
INTENTS = {
    "GREETING": ["hello", "hi there", "good morning", "hey", "how are you"],
    "CODING": ["write a java function", "debug this code", "create a spring boot app", "fix the bug"],
    "ARCHITECTURE": ["design a database", "system architecture", "cloud deployment plan", "system design"],
    "WEB_SEARCH": ["what is the latest news", "search the web", "current events", "google it"]
}

# Precompute embeddings for intents for blazing fast matching
intent_embeddings = {
    intent: model.encode(phrases).mean(axis=0)
    for intent, phrases in INTENTS.items()
}

class QueryRequest(BaseModel):
    query: str

@app.post("/classify")
def classify_intent(req: QueryRequest):
    query_emb = model.encode(req.query)
    
    best_intent = "UNKNOWN"
    highest_score = -1.0
    
    for intent, ref_emb in intent_embeddings.items():
        # Cosine similarity matching
        score = np.dot(query_emb, ref_emb) / (np.linalg.norm(query_emb) * np.linalg.norm(ref_emb))
        if score > highest_score:
            highest_score = score
            best_intent = intent
            
    return {"query": req.query, "intent": best_intent, "confidence": float(highest_score)}