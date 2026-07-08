# 📄 ফাইল: test_saga.py

**প্রকার:** .py  
**সাইজ:** 1,195 বাইট  
**আপডেট:** 2026-07-08T19:16:36.383243

---

## কোড

```py
import sys
import os
import sqlite3

# Add backend to path
sys.path.insert(0, os.path.abspath('backend'))

from core.evolution_engine import EvolutionEngine
from unittest.mock import patch
import database.supabase_client

def test_saga():
    db_path = 'test_evolution_saga.db'
    if os.path.exists(db_path):
        os.remove(db_path)
        
    engine = EvolutionEngine(db_path=db_path)
    
    print("--- Testing Supabase Failure Scenario ---")
    
    # Force db.client to be truthy so it enters the block
    database.supabase_client.db.client = True 
    
    with patch('database.supabase_client.db.insert_task_history', side_effect=Exception('Simulated Supabase Failure')):
        res = engine.learn_from_success('saga_test_task', 'test_approach', 'test_result')
        print("Saga Response:", res)
        
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM task_history WHERE task='saga_test_task'")
        rows = cursor.fetchall()
        print("SQLite Rows (Should be empty):", rows)
        conn.close()
        
    if os.path.exists(db_path):
        os.remove(db_path)

if __name__ == '__main__':
    test_saga()

```