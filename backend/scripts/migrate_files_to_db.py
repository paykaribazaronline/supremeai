import json
import os

# backend ফোল্ডারের parent ডিরেক্টরিকে sys.path-এ যোগ করা হচ্ছে
# যাতে core, tools ইত্যাদি মডিউল ইম্পোর্ট করা যায়
import sys
import xml.etree.ElementTree as ET

import psycopg2
from loguru import logger


backend_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, backend_dir)

try:
    from core.config import settings
    from tools.headless_agent_registry import get_headless_agent_configs
except ImportError as e:
    logger.error(f"Failed to import necessary modules: {e}. Make sure the script is run from the correct directory.")
    sys.exit(1)


def get_db_connection():
    """
    mcp_supabase.py থেকে অনুপ্রাণিত হয়ে ডাটাবেস কানেকশন তৈরি করে।
    """
    db_url = getattr(settings, "supabase_database_url", None)
    if not db_url:
        logger.error("SUPABASE_DATABASE_URL is not configured in settings.")
        return None
    try:
        conn = psycopg2.connect(db_url)
        logger.info("🐘 Successfully connected to the database.")
        return conn
    except psycopg2.OperationalError as e:
        logger.error(f"Database connection failed: {e}")
        return None


def migrate_skills(conn):
    """
    'skills' ফোল্ডার থেকে .py ফাইলগুলো পড়ে ডাটাবেসের 'skills' টেবিলে মাইগ্রেট করে।
    """
    logger.info("Starting skill migration...")
    skills_dir = os.path.join(backend_dir, "core", "skills")
    if not os.path.isdir(skills_dir):
        logger.warning(f"Skills directory not found at {skills_dir}. Skipping.")
        return

    cursor = conn.cursor()
    migrated_count = 0
    for filename in os.listdir(skills_dir):
        if filename.endswith(".py") and filename != "__init__.py":
            skill_name = os.path.splitext(filename)[0]
            file_path = os.path.join(skills_dir, filename)

            try:
                with open(file_path, encoding="utf-8") as f:
                    code = f.read()
                    # একটি সাধারণ ডেসক্রিপশন তৈরি করা হলো
                    description = f"Skill for {skill_name.replace('_', ' ')}. Automatically migrated."

                    cursor.execute(
                        """
                        INSERT INTO skills (skill_name, description, code, status, version)
                        VALUES (%s, %s, %s, 'active', 1)
                        ON CONFLICT (skill_name) DO NOTHING;
                        """,
                        (skill_name, description, code),
                    )
                    if cursor.rowcount > 0:
                        migrated_count += 1
                        logger.success(f"  -> Migrated skill: {skill_name}")

            except Exception as e:  # noqa: BLE001
                logger.error(f"Failed to migrate skill {skill_name}: {e}")

    conn.commit()
    cursor.close()
    logger.info(f"Skill migration completed. Migrated {migrated_count} new skills.")


def migrate_rules(conn):
    """
    'docs/context_modules' থেকে .xml ফাইল পড়ে 'rules' টেবিলে মাইগ্রেট করে।
    """
    logger.info("Starting rule migration...")
    rules_dir = os.path.join(backend_dir, "docs", "context_modules")
    if not os.path.isdir(rules_dir):
        logger.warning(f"Rules directory not found at {rules_dir}. Skipping.")
        return

    cursor = conn.cursor()
    migrated_count = 0
    for filename in os.listdir(rules_dir):
        if filename.endswith(".xml"):
            file_path = os.path.join(rules_dir, filename)
            try:
                tree = ET.parse(file_path)
                root = tree.getroot()
                category = os.path.splitext(filename)[0].replace("_context", "")

                for rule_tag in root.findall("rule"):
                    rule_key = rule_tag.get("key", f"{category}_{migrated_count}")
                    value = rule_tag.text.strip()
                    description = rule_tag.get("description", f"Rule for {category}")

                    cursor.execute(
                        """
                        INSERT INTO rules (rule_key, category, value, description, is_enabled)
                        VALUES (%s, %s, %s, %s, TRUE)
                        ON CONFLICT (rule_key) DO NOTHING;
                        """,
                        (rule_key, category, value, description),
                    )
                    if cursor.rowcount > 0:
                        migrated_count += 1
                        logger.success(f"  -> Migrated rule: {rule_key}")

            except Exception as e:  # noqa: BLE001
                logger.error(f"Failed to parse or migrate rules from {filename}: {e}")

    conn.commit()
    cursor.close()
    logger.info(f"Rule migration completed. Migrated {migrated_count} new rules.")


def migrate_agent_configs(conn):
    """
    'tools/headless_agent_registry.py' থেকে কনফিগারেশন নিয়ে 'agent_configs' টেবিলে মাইগ্রেট করে।
    """
    logger.info("Starting agent configuration migration...")
    try:
        agent_configs = get_headless_agent_configs()
        cursor = conn.cursor()
        migrated_count = 0

        for agent_name, config in agent_configs.items():
            description = config.get("description", f"Configuration for {agent_name}")
            config_json = json.dumps(config)

            cursor.execute(
                """
                INSERT INTO agent_configs (agent_name, description, config_json, status)
                VALUES (%s, %s, %s, 'active')
                ON CONFLICT (agent_name) DO NOTHING;
                """,
                (agent_name, description, config_json),
            )
            if cursor.rowcount > 0:
                migrated_count += 1
                logger.success(f"  -> Migrated agent config: {agent_name}")

        conn.commit()
        cursor.close()
        logger.info(f"Agent configuration migration completed. Migrated {migrated_count} new agent configs.")

    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to migrate agent configurations: {e}")


def create_tables(conn):
    """
    ডাটাবেস টেবিলগুলো তৈরি করে যদি না থাকে।
    """
    logger.info("Ensuring database tables exist...")
    cursor = conn.cursor()
    try:
        cursor.execute("""
        DROP TABLE IF EXISTS skills CASCADE;
        DROP TABLE IF EXISTS rules CASCADE;
        DROP TABLE IF EXISTS agent_configs CASCADE;

        -- Skills Table
        CREATE TABLE IF NOT EXISTS skills (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            skill_name VARCHAR(255) UNIQUE NOT NULL,
            description TEXT,
            code TEXT NOT NULL,
            status VARCHAR(50) NOT NULL DEFAULT 'active',
            version INT NOT NULL DEFAULT 1,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );

        -- Rules Table
        CREATE TABLE IF NOT EXISTS rules (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            rule_key VARCHAR(255) UNIQUE NOT NULL,
            category VARCHAR(100) NOT NULL,
            value TEXT NOT NULL,
            description TEXT,
            is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );

        -- Agent Configurations Table
        CREATE TABLE IF NOT EXISTS agent_configs (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            agent_name VARCHAR(255) UNIQUE NOT NULL,
            description TEXT,
            config_json JSONB NOT NULL,
            status VARCHAR(50) NOT NULL DEFAULT 'active',
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
        """)
        conn.commit()
        logger.success("Tables created successfully.")
    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to create tables: {e}")
        conn.rollback()
    finally:
        cursor.close()


if __name__ == "__main__":
    logger.info("🚀 Starting CODE_TO_DATABASE migration...")
    db_conn = get_db_connection()
    if db_conn:
        create_tables(db_conn)
        migrate_skills(db_conn)
        migrate_rules(db_conn)
        migrate_agent_configs(db_conn)
        db_conn.close()
        logger.info("✅ Migration process finished.")
    else:
        logger.error("Could not run migration due to database connection failure.")
