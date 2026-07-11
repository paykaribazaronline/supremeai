# 📄 ফাইল: fix.py

**প্রকার:** .py  
**সাইজ:** 6,483 বাইট  
**আপডেট:** 2026-07-11T17:11:02.583694

---

## কোড

```py
import re

for file in ['.github/workflows/supreme-core-ci.yml', '.github/workflows/nightly-maintenance.yml']:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Simple replace since the blocks are consistent
    
    # We have a few variants, let's fix them manually for safety
    old1 = """      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: |
          pip install poetry
          poetry config virtualenvs.in-project true
          poetry install --sync --with dev --without ml,tools"""
          
    new1 = """      - name: Install Poetry
        run: |
          pip install poetry litellm pyyaml httpx
          poetry config virtualenvs.in-project true
      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: poetry install --sync --with dev --without ml,tools"""

    old2 = """      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        run: |
          pip install poetry litellm pyyaml httpx
          cd backend
          poetry config virtualenvs.in-project true
          poetry install --sync --with dev --without ml,tools"""

    new2 = """      - name: Install Poetry
        run: |
          pip install poetry litellm pyyaml httpx
          cd backend && poetry config virtualenvs.in-project true
      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: poetry install --sync --with dev --without ml,tools"""

    old3 = """      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        run: |
          pip install poetry
          cd backend
          poetry config virtualenvs.in-project true
          poetry install --with dev --without ml"""
          
    new3 = """      - name: Install Poetry
        run: |
          pip install poetry litellm pyyaml httpx
          cd backend && poetry config virtualenvs.in-project true
      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: poetry install --with dev --without ml"""

    old4 = """      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Python Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        run: |
          pip install poetry
          cd backend
          poetry config virtualenvs.in-project true
          poetry install --with dev --without ml"""

    new4 = """      - name: Install Poetry
        run: |
          pip install poetry litellm pyyaml httpx
          cd backend && poetry config virtualenvs.in-project true
      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Python Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: poetry install --with dev --without ml"""
        
    old5 = """      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: |
          pip install poetry
          poetry config virtualenvs.in-project true
          poetry install --sync --without ml"""
          
    new5 = """      - name: Install Poetry
        working-directory: backend
        run: |
          pip install poetry litellm pyyaml httpx
          poetry config virtualenvs.in-project true
      - name: Load Cached Virtualenv
        id: cached-poetry-dependencies
        uses: actions/cache@v4
        with:
          path: backend/.venv
          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
      - name: Install Dependencies (Only on Cache Miss)
        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
        working-directory: backend
        run: poetry install --sync --without ml"""

    content = content.replace(old1, new1).replace(old2, new2).replace(old3, new3).replace(old4, new4).replace(old5, new5)
    
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

```