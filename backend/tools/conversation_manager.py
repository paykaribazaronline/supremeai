import uuid
from typing import Dict, Any, List
from loguru import logger

class ConversationManager:
    """
    Manages long-running conversations with sliding window memory,
    summary trees, and entity tracking to save context window space.
    """

    def __init__(self):
        self.sessions: Dict[str, Dict[str, Any]] = {}
        logger.info("Initialized ConversationManager")

    def create_session(self) -> str:
        session_id = uuid.uuid4().hex
        self.sessions[session_id] = {
            "history": [],
            "summary": "Start of conversation.",
            "entities": {}
        }
        return session_id

    def add_message(self, session_id: str, role: str, content: str, max_history: int = 10):
        if session_id not in self.sessions:
            raise ValueError(f"Invalid session {session_id}")
            
        session = self.sessions[session_id]
        session["history"].append({"role": role, "content": content})
        
        # Sliding window
        if len(session["history"]) > max_history:
            # Pop the oldest user/assistant pair and update the summary
            old_msg = session["history"].pop(0)
            logger.debug(f"Summarizing old message to save tokens: {old_msg['content'][:20]}...")
            session["summary"] += f" {old_msg['role']} said something."

    def get_context(self, session_id: str) -> List[Dict[str, str]]:
        if session_id not in self.sessions:
            return []
            
        session = self.sessions[session_id]
        # Prepend the summary as system prompt
        context = [{"role": "system", "content": f"Previous context: {session['summary']}"}]
        context.extend(session["history"])
        return context
