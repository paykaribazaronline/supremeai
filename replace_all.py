import re

filepath = '.github/workflows/supreme-core-ci.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# The regex to find the blocks:
# It starts with "      - name: Setup Poetry"
# And ends with either "run: poetry install ..." or "run: |\n          poetry config virtualenvs.create false..."
pattern = r"      - name: Setup Poetry\s+uses: snok/install-poetry@v1[\s\S]*?(?:run: poetry install[^\n]*|run: \|\s+poetry config virtualenvs\.create false[\s\S]*?cat requirements\.txt\))"

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

content = re.sub(pattern, universal_block, content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Now do it for maintenance_pipeline.yml
filepath = '.github/workflows/maintenance_pipeline.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace existing requirements.txt installation in setup job
content = re.sub(
    r"      - name: \"♻️ Cache and Install Dependencies\"[\s\S]*?pip install black isort pip-audit ruff",
    """      - name: Setup Poetry
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
          pip install black isort pip-audit ruff""",
    content
)

# And in auto-generate-docs job (which was moved here)
content = re.sub(pattern, universal_block, content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print('Replacement complete.')
