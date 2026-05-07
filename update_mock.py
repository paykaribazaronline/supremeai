import os
import glob
import re

def update_mock():
    test_dir = '/home/nazifarabbu/OneDrive/supremeai/src/test/java/'
    files = glob.glob(test_dir + '**/*.java', recursive=True)
    count = 0
    for file in files:
        with open(file, 'r') as f:
            content = f.read()
        
        if 'lenient' in content and '@Mock' in content:
            new_content = re.sub(r'@Mock\s*\(\s*lenient\s*=\s*true\s*\)', '@Mock', content)
            if new_content != content:
                # Add @MockitoSettings at the class level if not present
                if '@MockitoSettings' not in new_content:
                    new_content = re.sub(
                        r'(class \w+Test\s*\{)',
                        r'@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)\n\1',
                        new_content
                    )
                with open(file, 'w') as f:
                    f.write(new_content)
                count += 1
                print(f'Updated {file}')
    print(f'Total files modified: {count}')

if __name__ == '__main__':
    update_mock()
