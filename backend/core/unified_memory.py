"""
Unified Memory Interface

This module provides a single point of access for all memory-related operations
within SupremeAI, abstracting the underlying implementations:
- Long-term memory (Eternal Brain): CascadeMemoryService
- Short-term memory (Context Window): SlidingWindowMemory
- Task state persistence: CheckpointManager
"""

from typing import Any, Dict, List, Optional
from loguru import logger

# Import the underlying services
from services.memory_service import CascadeMemoryService
from memory.sliding_window import SlidingWindowMemory
from tools.checkpoint_manager import CheckpointManager


class UnifiedMemoryInterface:
    """
    A facade providing a unified API for interacting with different memory systems.
    """

    def __init__(self):
        self.long_term_memory = CascadeMemoryService()
        self.short_term_memory = SlidingWindowMemory()
        self.checkpoint_manager = CheckpointManager()

    # --- Long-term Memory (Eternal Brain) ---
    def store_long_term_memory(
        self,
        session_id: str,
        agent_type: str,
        task_type: str,
        content: str,
        metadata: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Store information in the long-term 'Eternal Brain' memory."""
        try:
            # Parse content and extract summary/structure using the service's built-in logic
            # This might need adjustment based on how content is passed
            # For now, assuming it's called from an agent context where summary is pre-made
            summary = content[:200] # Placeholder
            structure = "{}" # Placeholder
            self.long_term_memory.store_memory(
                file_path=session_id, # Map session_id to file_path for now
                content=content,
                summary=summary,
                structure=structure,
                session_id=session_id,
                agent_type=agent_type,
                task_type=task_type,
                metadata=metadata or {}
            )
            return True
        except Exception as e:
            logger.error(f"Failed to store long-term memory: {e}")
            return False

    def query_long_term_memory(
        self,
        query: str,
        top_k: int = 5,
        session_id: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """Query the long-term 'Eternal Brain' memory."""
        try:
            return self.long_term_memory.query_context(prompt=query, top_k=top_k, session_id=session_id)
        except Exception as e:
            logger.error(f"Failed to query long-term memory: {e}")
            return []

    # --- Short-term Memory (Context Window) ---
    def store_short_term_memory(
        self,
        session_id: str,
        text: str
    ) -> bool:
        """Store information in the short-term conversation context."""
        try:
            self.short_term_memory.chunk(text=text, session_id=session_id)
            return True
        except Exception as e:
            logger.error(f"Failed to store short-term memory: {e}")
            return False

    def get_short_term_memory(
        self,
        session_id: str,
        limit: int = 20
    ) -> List[Dict[str, Any]]:
        """Retrieve information from the short-term conversation context."""
        try:
            return self.short_term_memory.recall(session_id=session_id, limit=limit)
        except Exception as e:
            logger.error(f"Failed to retrieve short-term memory: {e}")
            return []

    # --- Task Checkpointing ---
    def save_checkpoint(
        self,
        task_id: str,
        step_index: int,
        state: Dict[str, Any]
    ) -> bool:
        """Save the current state of a task."""
        try:
            return self.checkpoint_manager.save(task_id=task_id, step_index=step_index, state=state)
        except Exception as e:
            logger.error(f"Failed to save checkpoint: {e}")
            return False

    def load_checkpoint(
        self,
        task_id: str
    ) -> Optional[Dict[str, Any]]:
        """Load the state of a task."""
        try:
            cp_obj = self.checkpoint_manager.load(task_id=task_id)
            if cp_obj:
                return {
                    "task_id": cp_obj.task_id,
                    "step_index": cp_obj.step_index,
                    "state": cp_obj.state,
                    "resumed": cp_obj.resumed
                }
            return None
        except Exception as e:
            logger.error(f"Failed to load checkpoint: {e}")
            return None


# Global instance (singleton pattern)
unified_memory = UnifiedMemoryInterface()