import re

filepath = '.github/workflows/supreme-core-ci.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

pattern1 = re.compile(r"      - name: Setup Poetry\n        uses: snok/install-poetry@v1[\s\S]*?pip install -r <\(poetry export --with dev --without ml,tools --format requirements\.txt\)")

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
print(f"Found {count1} instances of pattern 1")

content = pattern1.sub(universal_block, content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
