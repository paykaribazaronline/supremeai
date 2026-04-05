#!/usr/bin/env bash

set -euo pipefail

MODE="${1:-check}" # check | fix
ROOT="."
DOCS_DIR="./docs"

# Root markdown files allowed to stay in root.
ALLOWED_ROOT_MD=(
  "README.md"
  "CODE_OF_CONDUCT.md"
)

is_allowed_root_md() {
  local filename="$1"
  for allowed in "${ALLOWED_ROOT_MD[@]}"; do
    if [[ "$filename" == "$allowed" ]]; then
      return 0
    fi
  done
  return 1
}

resolve_target_dir() {
  local filename="$1"

  # If same file already exists under docs/, treat that as canonical target.
  local existing
  existing=$(find "$DOCS_DIR" -type f -name "$filename" | head -n 1 || true)
  if [[ -n "$existing" ]]; then
    dirname "$existing"
    return
  fi

  case "$filename" in
    ADMIN_*|*ADMIN*) echo "$DOCS_DIR/04-ADMIN" ;;
    AUTH_*|*SECURITY*|*AUTHENTICATION*) echo "$DOCS_DIR/05-AUTHENTICATION-SECURITY" ;;
    *ARCHITECTURE*|*ROADMAP*|*STRUCTURE*) echo "$DOCS_DIR/02-ARCHITECTURE" ;;
    PHASE*|*PHASE*) echo "$DOCS_DIR/03-PHASES" ;;
    *DEPLOY*|*SETUP*|*CONFIG*|*ENVIRONMENT*) echo "$DOCS_DIR/01-SETUP-DEPLOYMENT" ;;
    *TROUBLE*|*FIX*|*ROOT_CAUSE*|*MISTAKE*) echo "$DOCS_DIR/09-TROUBLESHOOTING" ;;
    *IMPLEMENTATION*|*STATUS*|*READINESS*) echo "$DOCS_DIR/10-IMPLEMENTATION" ;;
    *GUIDE*|*TUTORIAL*|HOW_TO_*) echo "$DOCS_DIR/12-GUIDES" ;;
    *REPORT*|*SUMMARY*|*VERIFICATION*|*ANALYSIS*|*COMPARISON*) echo "$DOCS_DIR/13-REPORTS" ;;
    *) echo "$DOCS_DIR/13-REPORTS" ;;
  esac
}

main() {
  if [[ ! -d "$DOCS_DIR" ]]; then
    echo "ERROR: docs directory not found at $DOCS_DIR"
    exit 1
  fi

  mapfile -t root_md_files < <(find "$ROOT" -maxdepth 1 -type f -name "*.md" -printf "%f\n" | sort)

  offenders=()
  for file in "${root_md_files[@]}"; do
    if ! is_allowed_root_md "$file"; then
      offenders+=("$file")
    fi
  done

  if [[ "$MODE" == "check" ]]; then
    if [[ ${#offenders[@]} -gt 0 ]]; then
      echo "Found markdown files in root that must be under docs/:"
      for f in "${offenders[@]}"; do
        echo "  - $f"
      done
      echo
      echo "Run: ./.github/scripts/enforce-doc-layout.sh fix"
      exit 1
    fi

    echo "OK: root markdown layout is clean."
    exit 0
  fi

  if [[ "$MODE" != "fix" ]]; then
    echo "Usage: $0 [check|fix]"
    exit 1
  fi

  moved=0
  for f in "${offenders[@]}"; do
    src="./$f"
    target_dir="$(resolve_target_dir "$f")"
    mkdir -p "$target_dir"

    target_path="$target_dir/$f"
    if [[ -e "$target_path" ]]; then
      ts="$(date +%Y%m%d-%H%M%S)"
      target_path="$target_dir/${f%.md}_from-root_$ts.md"
    fi

    mv "$src" "$target_path"
    echo "Moved: $f -> ${target_path#./}"
    moved=$((moved + 1))
  done

  echo "Done. Moved $moved markdown files from root into docs/."
}

main "$@"
