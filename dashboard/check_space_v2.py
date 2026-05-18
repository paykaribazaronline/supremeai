import os
import re

src_dir = "/home/nazifarabbu/supremeai/dashboard/src"
for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith((".tsx", ".ts")):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Check if Space is used
            has_space_usage = "<Space" in content or "Space.Compact" in content
            
            if has_space_usage:
                # Find all import blocks
                # import blocks can span multiple lines until from '...' or similar
                # Let's find all text matching import ... from ...
                imports = re.findall(r'import\s+[\s\S]*?\s+from\s+[\'"].*?[\'"]', content)
                
                # Also check for dynamic imports or direct imports
                # e.g., import '...';
                direct_imports = re.findall(r'import\s+[\'"].*?[\'"]', content)
                
                all_imports_text = "\n".join(imports + direct_imports)
                
                # Check if Space is imported (as a word, e.g. not namespace or substring)
                # We want to match Space as a whole word in the imports
                has_space_import = bool(re.search(r'\bSpace\b', all_imports_text))
                
                if not has_space_import:
                    print(f"REAL MISSING IMPORT IN: {filepath}")
                    for i, line in enumerate(content.split('\n')):
                        if 'Space' in line:
                            print(f"  Line {i+1}: {line}")
