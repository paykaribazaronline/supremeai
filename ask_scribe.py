#!/usr/bin/env python3
"""
SupremeAI - Ask the Scribe
==========================

A conversational interface to query the project's knowledge base.
It uses a Retrieval-Augmented Generation (RAG) approach to answer questions
about the codebase using the documentation indexed in ChromaDB.

Author: Gemini Code Assist
Date: July 12, 2026
"""

import argparse
import sys
from pathlib import Path

import chromadb
import litellm
from chromadb.utils import embedding_functions

# --- Configuration ---
DB_PATH = "supremeai_knowledge_base"
COLLECTION_NAME = "codebase_docs"

# Add backend to path to import settings
sys.path.insert(0, str(Path(__file__).parent / "backend"))
from core.config import settings

# --- AI Prompt Template (RAG) ---
RAG_PROMPT_TEMPLATE = """
You are a helpful AI assistant for the SupremeAI project, acting as an expert guide to the codebase.
Your task is to answer the user's question based *only* on the context provided below.
The context is extracted from the project's own documentation.

If the context does not contain the information needed to answer the question, state clearly:
"I'm sorry, I don't have information about that in my knowledge base."

Do not make up answers or use external knowledge.

--- CONTEXT FROM CODEBASE ---
{context}
---

QUESTION: {question}

ANSWER:
"""

async def answer_question(question: str) -> str:
    """
    Answers a question about the codebase using RAG.
    This is the core async logic callable from an API.
    """
    litellm.api_key = settings.gemini_api_key.split(',')[0]

    sentence_transformer_ef = embedding_functions.SentenceTransformerEmbeddingFunction(model_name="all-MiniLM-L6-v2")
    client = chromadb.PersistentClient(path=DB_PATH)
    collection = client.get_collection(name=COLLECTION_NAME, embedding_function=sentence_transformer_ef)

    # ১. প্রাসঙ্গিক তথ্যের জন্য ChromaDB কোয়েরি করা
    results = collection.query(query_texts=[question], n_results=7)
    context = "\n\n---\n\n".join(results['documents'][0])

    # ২. কনটেক্সট ব্যবহার করে LLM-কে উত্তর তৈরি করতে বলা
    prompt = RAG_PROMPT_TEMPLATE.format(context=context, question=question)
    response = await litellm.acompletion(model="gemini/gemini-1.5-flash-latest", messages=[{"role": "user", "content": prompt}])
    answer = response.choices[0].message.content
    return answer

async def main(question: str):
    """Answers a question about the codebase using the indexed knowledge."""
    answer = await answer_question(question)
    print("\n🤖 AI Scribe's Answer:\n")
    print(answer)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Ask the AI Scribe about the SupremeAI codebase.")
    parser.add_argument("question", type=str, help="Your question about the codebase.")
    args = parser.parse_args()
    import asyncio
    asyncio.run(main(args.question))
