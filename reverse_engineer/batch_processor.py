"""
Batch Processor - Plan 23 Optimization
Process multiple URLs at once for efficiency
"""
from main import ReverseEngineer
from self_healer import SelfHealer
import json
from typing import List, Dict

class BatchProcessor:
    def __init__(self, urls: List[str]):
        self.urls = urls
        self.results = []
        self.healer = SelfHealer("")
    
    def process_all(self, credentials_map: Dict[str, dict] = None) -> List[dict]:
        """Process all URLs efficiently"""
        credentials_map = credentials_map or {}
        
        for url in self.urls:
            print(f"\n{'='*60}")
            print(f"Processing: {url}")
            print('='*60)
            
            creds = credentials_map.get(url, {})
            engine = ReverseEngineer(url, creds)
            
            try:
                result = engine.run_full_pipeline()
                self.results.append({
                    'url': url,
                    'status': 'success',
                    'connector': result.get('connector_file'),
                    'endpoints': len(result.get('endpoints', []))
                })
            except Exception as e:
                # Try self-healing
                error_analysis = self.healer.analyze_error(str(e))
                healing_success = self.healer.apply_healing(error_analysis, {'name': 'heal'})
                
                self.results.append({
                    'url': url,
                    'status': 'failed' if not healing_success else 'healed',
                    'error': str(e),
                    'healed': healing_success
                })
        
        return self.results
    
    def export_summary(self, filename: str = "batch_report.json"):
        """Export batch results"""
        summary = {
            'total': len(self.results),
            'success': sum(1 for r in self.results if r['status'] == 'success'),
            'failed': sum(1 for r in self.results if r['status'] == 'failed'),
            'healed': sum(1 for r in self.results if r['status'] == 'healed'),
            'results': self.results
        }
        
        with open(filename, 'w') as f:
            json.dump(summary, f, indent=2)
        
        print(f"\n{'='*60}")
        print(f"BATCH SUMMARY")
        print('='*60)
        print(f"Total: {summary['total']}")
        print(f"Success: {summary['success']}")
        print(f"Failed: {summary['failed']}")
        print(f"Healed: {summary['healed']}")
        print(f"Report saved to: {filename}")

if __name__ == "__main__":
    # Test with multiple URLs
    urls = [
        "https://example.com",
        "https://www.google.com",
        "https://www.github.com"
    ]
    
    processor = BatchProcessor(urls)
    results = processor.process_all()
    processor.export_summary()
