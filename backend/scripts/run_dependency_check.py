import asyncio
import json
from loguru import logger
import sys
import os

# Ensure the backend tools can be imported
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from tools.dependency_manager_agent import DependencyManagerAgent
from tools.auto_pr_pipeline import AutoPRPipeline

async def main():
    """
    Main function to run dependency checks and potentially create PRs.
    """
    logger.info("Starting automated dependency analysis...")
    agent = DependencyManagerAgent()

    # --- Python (pip) dependencies ---
    pip_results = agent.check_pip_dependencies()
    if pip_results.get("success") and pip_results.get("count", 0) > 0:
        logger.info(f"Found {pip_results['count']} outdated pip packages.")
        print("--- Outdated Pip Packages ---")
        print(json.dumps(pip_results['outdated_packages'], indent=2))
        # Here you could add logic to automatically update a specific package
        # For example, to update the first outdated package:
        # outdated_pkg = pip_results['outdated_packages'][0]
        # await agent.auto_update_and_pr(".", outdated_pkg['name'])
    else:
        logger.info("No outdated pip packages found.")

    # --- Node.js (npm) dependencies ---
    # Assuming your frontend is in a directory like 'frontend'
    frontend_path = "./apps/studio-client" # Adjust if necessary
    if os.path.exists(os.path.join(frontend_path, "package.json")):
        npm_results = agent.check_npm_dependencies(project_path=frontend_path)
        if npm_results.get("success") and npm_results.get("count", 0) > 0:
            logger.info(f"Found {npm_results['count']} outdated npm packages.")
            print("--- Outdated NPM Packages ---")
            print(json.dumps(npm_results['outdated_packages'], indent=2))
        else:
            logger.info("No outdated npm packages found.")

    logger.info("Dependency analysis complete.")

if __name__ == "__main__":
    asyncio.run(main())