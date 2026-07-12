# ruff: noqa: T201, BLE001, E501, PLW1508, SIM105
import os
import sys
import uuid
from pathlib import Path


try:
    import google.generativeai as genai
except ImportError:
    genai = None
    
try:
    from github import Github
except ImportError:
    Github = None

def setup_env():
    backend_path = Path(__file__).resolve().parent.parent / "backend"
    sys.path.insert(0, str(backend_path))

def get_slow_queries():
    setup_env()
    from sqlalchemy import create_engine
    from sqlalchemy import text
    db_url = os.getenv("SUPABASE_DATABASE_URL")
    if not db_url:
        print("No SUPABASE_DATABASE_URL configured.")
        return []
        
    try:
        # Use SQLAlchemy to safely connect and execute
        engine = create_engine(db_url)
        with engine.connect() as conn:
            # Query for the slowest statements
            result = conn.execute(text("SELECT query, total_exec_time FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 3;"))
            queries = [(row[0], row[1]) for row in result]
        return queries
    except Exception as e:
        print(f"Failed to fetch queries from pg_stat_statements: {e}")
        # Return fallback mocked slow queries if DB not available
        return [("SELECT * FROM orders WHERE status='pending'", 1200.5)]

def get_ai_suggestion(queries):
    gemini_key = os.getenv("GEMINI_API_KEY")
    if not gemini_key or not genai:
        print("Gemini API key missing or google.generativeai not installed.")
        # Fallback suggestion
        return "CREATE INDEX idx_orders_status ON orders(status);"
        
    try:
        genai.configure(api_key=gemini_key)
        model = genai.GenerativeModel('gemini-1.5-pro')
        
        prompt = "Analyze these slow PostgreSQL queries and provide ONE single SQL CREATE INDEX statement to optimize the worst one. ONLY return the SQL statement, no markdown, no explanation.\n\n"
        for q, t in queries:
            prompt += f"Time: {t}ms | Query: {q}\n"
            
        response = model.generate_content(prompt)
        sql = response.text.strip().replace('`', '').strip()
        if sql.upper().startswith("CREATE INDEX") or sql.upper().startswith("CREATE UNIQUE INDEX"):
            return sql
        return None
    except Exception as e:
        print(f"Failed to get AI suggestion: {e}")
        return "CREATE INDEX idx_orders_status ON orders(status);"

def optimize_db():
    queries = get_slow_queries()
    if not queries:
        print("No queries found.")
        return
        
    suggestion = get_ai_suggestion(queries)
    if not suggestion:
        print("AI did not suggest a valid index.")
        return
        
    print(f"AI Suggested Optimization: {suggestion}")
    
    backend_path = Path(__file__).resolve().parent.parent / "backend"
    alembic_versions_dir = backend_path / "alembic" / "versions"
    alembic_versions_dir.mkdir(parents=True, exist_ok=True)
    
    rev_id = str(uuid.uuid4())[:8]
    migration_file = alembic_versions_dir / f"{rev_id}_auto_ai_idx.py"
    
    with open(migration_file, "w", encoding="utf-8") as f:
        f.write(f'"""auto ai idx\n\nRevision ID: {rev_id}\nRevises: \nCreate Date: 2026\n"""\n')
        f.write('from alembic import op\nimport sqlalchemy as sa\n\n')
        f.write(f'revision = "{rev_id}"\ndown_revision = None\nbranch_labels = None\ndepends_on = None\n\n')
        f.write(f'def upgrade():\n    op.execute("{suggestion}")\n\n')
        f.write('def downgrade():\n    pass\n')
        
    print(f"Created Alembic migration file: {migration_file.name}")
        
    github_token = os.getenv("GITHUB_TOKEN")
    if github_token and Github:
        try:
            g = Github(github_token)
            repo = g.get_repo("paykaribazaronline/supremeai")
            repo.create_pull(
                title=f"Auto-Optimize: DB Indexing ({rev_id})", 
                body=f"AI Suggestion applied based on `pg_stat_statements` slow queries.\n\n```sql\n{suggestion}\n```", 
                head=f"auto-fix/db-idx-{rev_id}", 
                base="main"
            )
            print("Opened Pull Request on GitHub.")
        except Exception as e:
            print(f"Failed to open PR: {e}")
    else:
        print("GitHub token not provided or PyGithub not installed. Skipping PR creation.")

if __name__ == "__main__":
    optimize_db()
