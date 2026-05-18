import os
import re

src_dir = "/home/nazifarabbu/supremeai/dashboard/src"
for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith((".tsx", ".ts")):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Check if Space is used as a React component
            has_space_usage = "<Space" in content or "Space.Compact" in content
            
            if has_space_usage:
                # Check if Space is imported from 'antd' or elsewhere
                # e.g., import { ... Space ... } from 'antd'
                # or import Space from ...
                has_space_import = False
                for line in content.split('\n'):
                    if 'import' in line and 'Space' in line:
                        has_space_import = True
                        break
                
                if not has_space_import:
                    print(f"MISSING IMPORT IN: {filepath}")
                    # Print lines containing Space
                    for i, line in enumerate(content.split('\n')):
                        if 'Space' in line:
                            print(f"  Line {i+1}: {line}")
