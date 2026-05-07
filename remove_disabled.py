import os
import glob

def remove_disabled():
    test_dir = '/home/nazifarabbu/OneDrive/supremeai/src/test/java/'
    files = glob.glob(test_dir + '**/*.java', recursive=True)
    count = 0
    for file in files:
        with open(file, 'r') as f:
            content = f.read()
        if '@Disabled' in content:
            new_content = []
            for line in content.split('\n'):
                if '@Disabled' not in line:
                    new_content.append(line)
            with open(file, 'w') as f:
                f.write('\n'.join(new_content))
            count += 1
            print(f'Removed @Disabled from {file}')
    print(f'Total files modified: {count}')

if __name__ == '__main__':
    remove_disabled()
