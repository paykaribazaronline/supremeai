import re

filepath = '.github/workflows/supreme-core-ci.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

pattern1 = re.compile(r"      - name: Setup Poetry\n        uses: snok/install-poetry@v1\n        with:\n          plugins: poetry-plugin-export\n\n      - name: Cache Poetry dependencies[\s\S]*?- name: Install Dependencies via Poetry Export\n          working-directory: backend\n          run: \|\n            poetry config virtualenvs\.create false\n            pip install -r <\(poetry export --with dev --without ml,tools --format requirements\.txt\)")

pattern2 = re.compile(r"      - name: Setup Poetry\n        uses: snok/install-poetry@v1\n        with:\n          virtualenvs-create: true\n          virtualenvs-in-project: true\n      - name: Load Cached Virtualenv[\s\S]*?- name: Install Backend Dependencies \(Only on Cache Miss\)[\s\S]*?run: poetry install --sync --with dev --without ml,tools")

universal_block = """      - name: Setup Poetry
        uses: snok/install-poetry@v1
        with:
          plugins: poetry-plugin-export

      - name: Cache pip dependencies
        uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ runner.os }}-pip-${{ hashFiles('**/poetry.lock') }}
          restore-keys: |
            ${{ runner.os }}-pip-

      - name: Setup Python & Install Dependencies
        working-directory: backend
        run: |
          python -m pip install --upgrade pip
          pip install --no-cache-dir --disable-pip-version-check -r <(poetry export --with dev --without ml,tools --format requirements.txt --output requirements.txt && cat requirements.txt)"""

count1 = len(pattern1.findall(content))
count2 = len(pattern2.findall(content))
print(f"Found {count1} instances of pattern 1")
print(f"Found {count2} instances of pattern 2")

content = pattern1.sub(universal_block, content)
content = pattern2.sub(universal_block, content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Maintenance pipeline
filepath = '.github/workflows/maintenance_pipeline.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    maint_content = f.read()

maint_pattern1 = re.compile(r"      - name: \"♻️ Cache and Install Dependencies\"[\s\S]*?pip install black isort pip-audit ruff")
count3 = len(maint_pattern1.findall(maint_content))
print(f"Found {count3} instances of maint pattern 1")

maint_universal_block = """      - name: Setup Poetry
        uses: snok/install-poetry@v1
        with:
          plugins: poetry-plugin-export

      - name: "♻️ Cache pip dependencies"
        uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ runner.os }}-pip-${{ hashFiles('**/poetry.lock') }}
          restore-keys: |
            ${{ runner.os }}-pip-

      - name: "⚙️ Install Dependencies"
        working-directory: backend
        run: |
          python -m pip install --upgrade pip
          pip install --no-cache-dir --disable-pip-version-check -r <(poetry export --with dev --without ml,tools --format requirements.txt --output requirements.txt && cat requirements.txt)
          pip install black isort pip-audit ruff"""

maint_content = maint_pattern1.sub(maint_universal_block, maint_content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(maint_content)

print('Replacement complete.')
