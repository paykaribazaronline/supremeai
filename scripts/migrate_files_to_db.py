import os
import json
import asyncio
import sys

# Append backend to path so we can import modules
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'backend'))

from tools.mcp_supabase import supabase_execute_sql, ExecuteQueryInput
from tools.headless_agent_registry import get_headless_agent_configs

async def migrate_skills():
    print("Migrating Skills...")
    skills_dir = os.path.join(os.path.dirname(__file__), '..', 'backend', 'core', 'skills')
    
    if not os.path.exists(skills_dir):
        print(f"Skills directory not found: {skills_dir}")
        return
        
    for filename in os.listdir(skills_dir):
        if filename.endswith('.py') and filename != '__init__.py':
            filepath = os.path.join(skills_dir, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                code_content = f.read()
                
            skill_name = filename.replace('.py', '')
            description = f"Core skill from {filename}"
            
            # Using parameterization to avoid SQL injection / quote issues
            query = """
            INSERT INTO skills (skill_name, description, code, status) 
            VALUES (%s, %s, %s, 'active')
            ON CONFLICT (skill_name) DO UPDATE 
            SET code = EXCLUDED.code, description = EXCLUDED.description, updated_at = NOW();
            """
            
            try:
                await supabase_execute_sql(ExecuteQueryInput(
                    query=query,
                    params=[skill_name, description, code_content]
                ))
                print(f"  - Migrated skill: {skill_name}")
            except Exception as e:
                print(f"  - Failed to migrate skill {skill_name}: {e}")

async def migrate_rules():
    print("Migrating Rules...")
    # Rules are scattered in docs/context_modules/ or AGENTS.md
    # For now, let's migrate a placeholder rule to establish the table and show it works.
    rules = [
        {
            "rule_key": "default_security",
            "category": "security",
            "value": "Do not execute arbitrary downloaded code without sandboxing.",
            "description": "Default security policy for agents."
        }
    ]
    
    for rule in rules:
        query = """
        INSERT INTO rules (rule_key, category, value, description)
        VALUES (%s, %s, %s, %s)
        ON CONFLICT (rule_key) DO UPDATE
        SET value = EXCLUDED.value, updated_at = NOW();
        """
        try:
            await supabase_execute_sql(ExecuteQueryInput(
                query=query,
                params=[rule['rule_key'], rule['category'], rule['value'], rule['description']]
            ))
            print(f"  - Migrated rule: {rule['rule_key']}")
        except Exception as e:
            print(f"  - Failed to migrate rule {rule['rule_key']}: {e}")

async def migrate_agent_configs():
    print("Migrating Agent Configs...")
    try:
        configs = get_headless_agent_configs()
        for agent_name, config in configs.items():
            query = """
            INSERT INTO agent_configs (agent_name, description, config_json)
            VALUES (%s, %s, %s)
            ON CONFLICT (agent_name) DO UPDATE
            SET config_json = EXCLUDED.config_json, description = EXCLUDED.description, updated_at = NOW();
            """
            await supabase_execute_sql(ExecuteQueryInput(
                query=query,
                params=[agent_name, config.get('description', ''), json.dumps(config)]
            ))
            print(f"  - Migrated agent config: {agent_name}")
    except Exception as e:
        print(f"Failed to migrate agent configs: {e}")

async def main():
    print("Starting CODE_TO_DATABASE migration...")
    await migrate_skills()
    await migrate_rules()
    await migrate_agent_configs()
    print("Migration completed successfully!")

if __name__ == "__main__":
    asyncio.run(main())
