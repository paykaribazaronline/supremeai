from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.responses import StreamingResponse
from loguru import logger
from pydantic import BaseModel

from api.dependencies import get_tenant_db
from api.deps import get_current_user_token
from core.cache.multi_layer_cache import multi_layer_cache
from core.llm.llm_gateway import llm_gateway
from core.circuit_breaker import RedisCircuitBreaker

# Global circuit breaker instance
main_llm_circuit = RedisCircuitBreaker(name="llm_gateway", failure_threshold=3, recovery_timeout=30.0)

router = APIRouter(prefix="/api/chat", tags=["AI-Orchestration"], dependencies=[Depends(get_current_user_token)])


class ChatPayload(BaseModel):
    prompt: str
    model_name: str = "gemini-2.5-pro"


# ⚡ ১. Fully Async Standard Completion with Multi-Layer Caching
@router.post("/get_completion")
async def get_completion(request: Request, payload: ChatPayload, db=Depends(get_tenant_db)):
    """Non-blocking Async LLM Completion with 5-Layer Caching"""
    logger.info(f"⚡ Async API Hit: Generating completion for tenant: {db.tenant_id}")

    # Extract session ID from headers for session-based caching
    session_id = request.headers.get("X-Session-ID")

    # Check multi-layer cache first
    cached_result = await multi_layer_cache.get(
        prompt=payload.prompt, model_name=payload.model_name, session_id=session_id
    )

    if cached_result:
        logger.info(f"🚀 CACHE HIT: {cached_result['source']}")
        return {
            "success": True,
            "response": cached_result["response"],
            "cached": True,
            "cache_source": cached_result["source"],
            "latency_ms": cached_result.get("latency_ms", 0),
        }

    # Cache miss - generate response from AI model with memory context
    logger.info("❌ CACHE MISS: Generating new response from AI model with memory recall")
    try:
        # Retrieve long-term memory facts for tenant/user context
        memory_ctx = ""
        try:
            from memory.long_term_memory import LongTermMemory
            ltm = LongTermMemory(session_id=session_id or "default")
            mem_facts = ltm.build_context()
            if mem_facts and mem_facts != "No memory available.":
                memory_ctx = f"[Relevant Memory Context:\n{mem_facts}]\n\n"
        except Exception as mem_err:
            logger.debug(f"Memory retrieval bypassed: {mem_err}")

        # Retrieve System Knowledge Base (Cold-Start RAG)
        try:
            from services.memory_service import recall_memories
            rag_results = await recall_memories(task_description=payload.prompt, limit=3, threshold=0.55)
            if rag_results:
                rag_facts = []
                for r in rag_results:
                    metadata = r.get("metadata", {})
                    content = metadata.get("content", r.get("summary", ""))
                    if content:
                        rag_facts.append(f"- {content}")
                if rag_facts:
                    memory_ctx += "[System Knowledge Base:\n" + "\n".join(rag_facts) + "]\n\n"
        except Exception as rag_err:
            logger.debug(f"RAG Retrieval bypassed: {rag_err}")

        enriched_prompt = f"{memory_ctx}{payload.prompt}" if memory_ctx else payload.prompt
        
        if await main_llm_circuit.should_attempt_external():
            try:
                # বাংলা মন্তব্য: সরাসরি গুগল নেটিভ ক্লায়েন্ট কল না করে ইউনিভার্সাল llm_gateway ব্যবহার করে এপিআই কল করা হচ্ছে
                response = await llm_gateway.acompletion(prompt=enriched_prompt, task_type="chat", stream=False)
                await main_llm_circuit.record_success()
                response_text = response.get("text", "") if isinstance(response, dict) else str(response)

                # Store response in multi-layer cache for future requests
                await multi_layer_cache.set(
                    prompt=payload.prompt,
                    response=response_text,
                    model_name=payload.model_name,
                    session_id=session_id,
                )

                return {
                    "success": True,
                    "response": response_text,
                    "cached": False,
                    "cache_source": "L5_AI_MODEL",
                    "source": "external"
                }
            except Exception as e:
                logger.warning(f"External LLM API fail: {e!s} — falling back")
                await main_llm_circuit.record_failure()
                # Fall through to fallback logic

        # --- Fallback Path ---
        try:
            from services.memory_service import recall_memories
            fallback_results = await recall_memories(task_description=payload.prompt, limit=1, threshold=0.75)
            if fallback_results:
                best = fallback_results[0]
                metadata = best.get("metadata", {})
                answer = metadata.get("content", best.get("summary", ""))
                
                similarity = best.get("similarity", 0.8)
                disclaimer = " (এই উত্তরটি সম্পূর্ণ নিশ্চিত নাও হতে পারে।)" if similarity < 0.8 else ""
                
                response_text = answer + disclaimer
                return {
                    "success": True,
                    "response": response_text,
                    "cached": False,
                    "cache_source": "KNOWLEDGE_BASE_FALLBACK",
                    "source": "knowledge_base"
                }
        except Exception as e:
            logger.exception(f"Knowledge base fallback query failed: {e}")
            
        return {
            "success": True,
            "response": "দুঃখিত, এই মুহূর্তে আপনার প্রশ্নের উত্তর দিতে পারছি না। একটু পরে আবার চেষ্টা করুন।",
            "cached": False,
            "cache_source": "FALLBACK_NO_MATCH",
            "source": "no_match"
        }
    except Exception as e:
        logger.error(f"Async LLM Error: {e!s}")
        raise HTTPException(status_code=500, detail="AI Gateway Timeout.") from e


# ⚡ ২. Fully Async Streaming Generator
@router.post("/stream_chat")
async def stream_chat(payload: ChatPayload, db=Depends(get_tenant_db)):
    """High-Concurrency Async SSE Streamer.

    বাংলা: SSE-এর জন্য ক্রিটিক্যাল হেডার যোগ করা হলো (Cache-Control: no-cache,
    X-Accel-Buffering: no) যাতে nginx/CDN/proxy স্ট্রিম বাফার না করে। ক্লায়েন্ট
    ডিসকানেক্ট হলে generator বন্ধ হবে।
    """
    logger.info(f"🌊 SSE Stream Initiated for tenant: {db.tenant_id}")

    async def async_generator():
        try:
            # Retrieve System Knowledge Base (Cold-Start RAG)
            memory_ctx = ""
            try:
                from services.memory_service import recall_memories
                rag_results = await recall_memories(task_description=payload.prompt, limit=3, threshold=0.55)
                if rag_results:
                    rag_facts = []
                    for r in rag_results:
                        metadata = r.get("metadata", {})
                        content = metadata.get("content", r.get("summary", ""))
                        if content:
                            rag_facts.append(f"- {content}")
                    if rag_facts:
                        memory_ctx = "[System Knowledge Base:\n" + "\n".join(rag_facts) + "]\n\n"
            except Exception as rag_err:
                logger.debug(f"RAG Retrieval bypassed in stream: {rag_err}")
                
            enriched_prompt = f"{memory_ctx}{payload.prompt}" if memory_ctx else payload.prompt

            if await main_llm_circuit.should_attempt_external():
                try:
                    # বাংলা: ইউনিভার্সাল llm_gateway ব্যবহার করে স্ট্রিমিং সম্পন্ন করা হচ্ছে
                    response_stream = await llm_gateway.acompletion(prompt=enriched_prompt, task_type="chat", stream=True)

                    async for chunk in response_stream:
                        if chunk:
                            # SSE (Server-Sent Events) স্ট্যান্ডার্ড ফরম্যাট
                            yield f"data: {chunk}\n\n"

                    yield "data: [DONE]\n\n"
                    await main_llm_circuit.record_success()
                    return
                except Exception as e:
                    logger.warning(f"External LLM API stream fail: {e!s} — falling back")
                    await main_llm_circuit.record_failure()
                    
            # --- Fallback Path ---
            try:
                from services.memory_service import recall_memories
                fallback_results = await recall_memories(task_description=payload.prompt, limit=1, threshold=0.75)
                if fallback_results:
                    best = fallback_results[0]
                    metadata = best.get("metadata", {})
                    answer = metadata.get("content", best.get("summary", ""))
                    
                    similarity = best.get("similarity", 0.8)
                    disclaimer = " (এই উত্তরটি সম্পূর্ণ নিশ্চিত নাও হতে পারে।)" if similarity < 0.8 else ""
                    
                    response_text = answer + disclaimer
                    yield f"data: {response_text}\n\n"
                    yield "data: [DONE]\n\n"
                    return
            except Exception as e:
                logger.exception(f"Knowledge base stream fallback failed: {e}")
                
            yield "data: দুঃখিত, এই মুহূর্তে আপনার প্রশ্নের উত্তর দিতে পারছি না। একটু পরে আবার চেষ্টা করুন।\n\n"
            yield "data: [DONE]\n\n"
        except Exception as e:
            logger.error(f"Stream broken: {e!s}")
            yield f"data: [ERROR] {e!s}\n\n"

    # বাংলা: SSE হেডার — proxy/CDN বাফারিং রোধে ক্রিটিক্যাল।
    return StreamingResponse(
        async_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # nginx বাফারিং রোধে
            "Content-Encoding": "identity",  # কম্প্রেশন বন্ধ — SSE-এর জন্য প্রয়োজন
        },
    )
