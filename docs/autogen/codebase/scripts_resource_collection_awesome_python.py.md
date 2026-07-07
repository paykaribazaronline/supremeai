# 📄 ফাইল: scripts/resource_collection/awesome_python.py

**প্রকার:** .py  
**সাইজ:** 1,178 বাইট  
**আপডেট:** 2026-07-07T17:20:39.811214

---

## কোড

```py
"""
Awesome Python scraper for SupremeAI resource collection
Scrapes the awesome-python GitHub repository
"""

import sys
from pathlib import Path

# Add the parent directory to sys.path to import base classes
sys.path.append(str(Path(__file__).parent.parent))

from resource_collection.base_scraper import AwesomeListScraper


class AwesomePythonScraper(AwesomeListScraper):
    """Scraper for awesome-python list"""
    
    def __init__(self, data_dir: Path):
        super().__init__(
            name="awesome-python",
            repo_url="https://github.com/vinta/awesome-python",
            data_dir=data_dir
        )


def main():
    """Main function to run the scraper"""
    # Set up data directory
    data_dir = Path(__file__).parent.parent / "data" / "awesome-python"
    
    # Create and run scraper
    scraper = AwesomePythonScraper(data_dir)
    result = scraper.run()
    
    if result:
        print(f"[SUCCESS] {scraper.name} scraper completed successfully")
        print(f"  Data saved to: {result}")
        return 0
    else:
        print(f"[ERROR] {scraper.name} scraper failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
```