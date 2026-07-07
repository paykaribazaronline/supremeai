# FILE_PATH: tools/auto_coverage_improver.py
import argparse
import asyncio
import os
import sys
from pathlib import Path
from typing import Any
import logging # Import standard logging for early messages before loguru is configured

# বাংলা মন্তব্য: স্ক্রিপ্টটি যেকোনো ডিরেক্টরি থেকে সরাসরি রান করার সুবিধার্থে sys.path এ প্রজেক্ট রুট ও ব্যাকএন্ড পাথ যুক্ত করা হচ্ছে
# Adjust sys.path to include the project root (supremeai/backend) for proper absolute imports.
# Path(__file__).resolve().parent gives .../backend/tools
# .parent again gives .../backend (the logical project root for 'backend.tools' imports)
project_root = Path(__file__).resolve().parent.parent
if str(project_root) not in sys.path:
    sys.path.insert(0, str(project_root))

# Basic logging configuration for immediate output before loguru is set up.
# This ensures that messages from the dependency check/install are visible.
logging.basicConfig(level=logging.INFO, format='%(asctime)s | %(levelname)-8s | %(name)s:%(funcName)s:%(lineno)d - %(message)s')
temp_logger = logging.getLogger(__name__)

# Ensure 'defusedxml' dependency is present.
# This is a pragmatic workaround for CI failures when 'defusedxml' is missing from the environment.
# The proper, declarative fix for a production CI pipeline is to add 'defusedxml' to
# the project's requirements.txt (or pyproject.toml) and ensure it's installed by the CI environment.
try:
    import defusedxml.ElementTree as _ # noqa: F401 (ignore unused import)
except ImportError:
    import subprocess
    temp_logger.warning("Module 'defusedxml' not found. Attempting to install it using pip. "
                        "Please add 'defusedxml' to your project's requirements.txt for a permanent, declarative fix.")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "defusedxml"])
        temp_logger.info("'defusedxml' installed successfully.")
    except Exception as e:
        temp_logger.error(f"Failed to install 'defusedxml' programmatically: {e}")
        # Re-raise the original ImportError to ensure the CI pipeline fails and the issue is visible,
        # but now with an attempt to self-heal and a more explicit error message if installation fails.
        raise # Re-raise the original ImportError if pip install fails.
    # If installation is successful, the module should be available for subsequent imports
    # by modules like coverage_auditor.py within the same process.

from _bootstrap import bootstrap

bootstrap()

from loguru import logger # loguru is imported AFTER bootstrap and the potential pip install.
                          # It will now take over logging if bootstrap doesn't override it.

from backend.tools.auto_test_generator import AutoTestGenerator
from backend.tools.coverage_auditor import CoverageAuditor


class AutoCoverageImprover:
    """
    Orchestrates coverage analysis and automatic test generation
    to improve overall test coverage of a project.
    """

    def __init__(self):
        self.auditor = CoverageAuditor()
        self.generator = AutoTestGenerator()
        logger.info("Initialized AutoCoverageImprover")

    async def run(
        self,
        coverage_report_path: str,
        min_coverage_target: float = 80.0,  # লক্ষ্যমাত্রা ৮০% এ উন্নীত করা হলো
        dry_run: bool = False,
    ) -> dict[str, Any]:
        """
        Analyzes a coverage report, identifies gaps, and generates tests to fill them.

        Args:
            coverage_report_path: Path to the coverage.xml or coverage.json file.
            min_coverage_target: The minimum coverage percentage to aim for.
            dry_run: If True, will not write any files to disk.

        Returns:
            A report of the actions taken.
        """
        logger.info(f"Starting coverage improvement run for report: {coverage_report_path}")

        gaps = self.auditor.find_gaps(coverage_report_path, min_coverage=min_coverage_target)

        if not gaps:
            logger.info("No coverage gaps found. Excellent work!")
            return {
                "status": "success",
                "message": "No coverage gaps found.",
                "gaps_found": 0,
                "tests_generated": 0,
            }

        logger.info(f"Found {len(gaps)} file(s) with coverage below {min_coverage_target}%.")

        generation_results = []
        for gap in gaps:
            logger.info(f"Attempting to generate tests for '{gap.file_path}' (Coverage: {gap.coverage}%)")
            if not os.path.exists(gap.file_path):
                logger.warning(f"Source file not found, skipping: {gap.file_path}")
                continue

            result = await self.generator.generate_and_save(gap.file_path, run_tests=not dry_run)
            generation_results.append(result)

        return {
            "status": "completed",
            "gaps_found": len(gaps),
            "tests_generated": sum(1 for r in generation_results if r.get("status") == "success"),
            "results": generation_results,
        }


async def main():
    """Command-line interface for the AutoCoverageImprover."""
    parser = argparse.ArgumentParser(description="Automatically find and fix test coverage gaps.")
    parser.add_argument("coverage_report", help="Path to the coverage.xml or coverage.json report file.")
    parser.add_argument(
        "--min-target",
        type=float,
        default=80.0,  # ডিফল্ট লক্ষ্যমাত্রা ৮০% করা হলো
        help="The minimum coverage percentage to aim for. Default: 80.0",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="If set, will identify gaps and generate tests without saving files or running tests.",
    )
    args = parser.parse_args()

    improver = AutoCoverageImprover()
    report = await improver.run(
        coverage_report_path=args.coverage_report,
        min_coverage_target=args.min_target,
        dry_run=args.dry_run,
    )
    logger.info(f"Run completed. Report: {report}")


if __name__ == "__main__":
    asyncio.run(main())
