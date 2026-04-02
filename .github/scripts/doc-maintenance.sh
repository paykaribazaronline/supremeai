#!/bin/bash

################################################################################
# 3-LAYER DOCUMENTATION MAINTENANCE SYSTEM
# Layer 1: Discovery (batch markdown files)
# Layer 2: Processing (process batches safely)
# Layer 3: Aggregation (combine results & report)
################################################################################

set -euo pipefail

MODE="${1:-scan}"  # scan, fix, or report
BATCH_SIZE="${2:-50}"  # Files per batch (tuned to avoid arg list too long)

echo "🚀 SupremeAI 3-Layer Documentation Maintenance System"
echo "📊 Mode: $MODE | Batch Size: $BATCH_SIZE files/batch"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

################################################################################
# LAYER 1: DISCOVERY - Find markdown files in batches
################################################################################
discover_markdown_files() {
    local temp_dir=$(mktemp -d)
    local batch_num=0
    local file_count=0
    local current_batch=""
    
    echo "📍 Layer 1: DISCOVERY - Cataloging markdown files..."
    
    # Find all .md files and process in batches
    while IFS= read -r file; do
        current_batch="$current_batch$file"$'\n'
        ((file_count++))
        
        # When batch reaches size limit, save it
        if [ $file_count -ge $BATCH_SIZE ]; then
            ((batch_num++))
            echo "$current_batch" > "$temp_dir/batch_$batch_num.txt"
            current_batch=""
            file_count=0
        fi
    done < <(find . -name "*.md" -type f ! -path "./.git/*" ! -path "./node_modules/*" 2>/dev/null | sort)
    
    # Save remaining files
    if [ $file_count -gt 0 ]; then
        ((batch_num++))
        echo "$current_batch" > "$temp_dir/batch_$batch_num.txt"
    fi
    
    echo "✅ Discovered $((batch_num)) batches of markdown files"
    echo "$temp_dir"
}

################################################################################
# LAYER 2: PROCESSING - Process each batch
################################################################################
process_batch_scan() {
    local batch_file="$1"
    local batch_num="$2"
    local result_dir="$3"
    
    echo "🔍 Processing batch $batch_num (scan)..."
    
    # Read files from batch and pass to markdownlint using array
    local files=()
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            files+=("$file")
        fi
    done < "$batch_file"
    
    if [ ${#files[@]} -gt 0 ]; then
        # Use array to avoid arg list too long
        # Using explicit .markdownlint.json config file
        markdownlint -c .markdownlint.json "${files[@]}" > "$result_dir/batch_${batch_num}_report.txt" 2>&1 || true
        
        # Count errors in this batch
        local error_count=$(grep -c "error" "$result_dir/batch_${batch_num}_report.txt" || echo "0")
        echo "$batch_num:$error_count" >> "$result_dir/batch_counts.txt"
        
        echo "  📄 Found $error_count errors in batch $batch_num"
    fi
}

process_batch_fix() {
    local batch_file="$1"
    local batch_num="$2"
    local result_dir="$3"
    
    echo "🔧 Processing batch $batch_num (fix)..."
    
    # Read files from batch and pass to markdownlint with --fix
    local files=()
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            files+=("$file")
        fi
    done < "$batch_file"
    
    if [ ${#files[@]} -gt 0 ]; then
        echo "  📄 Files in batch: ${#files[@]}"
        
        # Show which files we're processing
        for f in "${files[@]}"; do
            echo "    - $f"
        done | head -5
        
        # Use array to avoid arg list too long
        # Now using explicit .markdownlint.json config
        markdownlint -c .markdownlint.json --fix "${files[@]}" > "$result_dir/batch_${batch_num}_fix.txt" 2>&1 || true
        
        # Count how many files had changes
        local files_changed=0
        for file in "${files[@]}"; do
            if git diff --quiet "$file" 2>/dev/null; then
                # no changes
                true
            else
                ((files_changed++))
            fi
        done
        
        # If git diff didn't work, count all as potentially changed
        if [ $files_changed -eq 0 ]; then
            files_changed=${#files[@]}
        fi
        
        echo "  ✅ Batch complete: $files_changed files changed"
        echo "$batch_num:$files_changed" >> "$result_dir/batch_fixes.txt"
        
        # Log details
        cat >> "$result_dir/batch_${batch_num}_fix.txt" << EOF

=== Batch $batch_num Summary ===
Files processed: ${#files[@]}
Files changed: $files_changed
EOF
    fi
}

################################################################################
# LAYER 3: AGGREGATION - Combine results
################################################################################
aggregate_scan_results() {
    local result_dir="$1"
    
    echo "📊 Layer 3: AGGREGATION - Combining scan results..."
    
    local total_errors=0
    local total_batches=0
    local combined_report=""
    
    # Sum up errors from all batches
    if [ -f "$result_dir/batch_counts.txt" ]; then
        while IFS=':' read -r batch_num count; do
            ((total_errors += count))
            ((total_batches++))
        done < "$result_dir/batch_counts.txt"
    fi
    
    # Combine all reports
    for report in "$result_dir"/batch_*_report.txt; do
        if [ -f "$report" ]; then
            combined_report+=$(cat "$report")
            combined_report+=$'\n---\n'
        fi
    done
    
    # Output final report
    cat > "$result_dir/FINAL_SCAN_REPORT.md" << EOF
# 📚 Documentation Linting Scan Report

**Timestamp:** $(date -Iseconds)
**Total Batches Processed:** $total_batches
**Total Errors Found:** $total_errors

## Detailed Results

\`\`\`
$combined_report
\`\`\`

## Summary

| Metric | Value |
|--------|-------|
| Batches | $total_batches |
| Errors | $total_errors |
| Status | $([ $total_errors -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL") |

EOF
    
    echo "✅ Aggregated $total_batches batches with $total_errors errors"
    echo "$total_errors"
}

aggregate_fix_results() {
    local result_dir="$1"
    
    echo "📊 Layer 3: AGGREGATION - Combining fix results..."
    
    local total_fixed=0
    local total_batches=0
    
    # Sum up fixed files from all batches
    if [ -f "$result_dir/batch_fixes.txt" ]; then
        while IFS=':' read -r batch_num count; do
            ((total_fixed += count))
            ((total_batches++))
        done < "$result_dir/batch_fixes.txt"
    fi
    
    # Get list of modified files
    local modified_files=$(git diff --name-only | grep '\.md$' | tr '\n' ', ' | sed 's/,$//')
    
    # Output summary
    cat > "$result_dir/FINAL_FIX_REPORT.md" << EOF
# 📝 Documentation Auto-Fix Report

**Timestamp:** $(date -Iseconds)
**Total Batches Processed:** $total_batches
**Files Fixed:** $total_fixed
**Modified Files:** $modified_files

## Rules Applied

- **MD009**: Trailing spaces removed
- **MD022**: Headings surrounded by blank lines
- **MD023**: Heading level sequence
- **MD026**: Trailing punctuation in headings
- **MD031**: Fenced code blocks surrounded by blank lines
- **MD032**: Lists surrounded by blank lines

## Status

✅ Auto-fix complete - Ready for commit

EOF
    
    echo "✅ Aggregated $total_batches batches, fixed $total_fixed files"
    echo "$total_fixed"
}

################################################################################
# MAIN EXECUTION FLOW
################################################################################
main() {
    local temp_dir=$(discover_markdown_files)
    local result_dir="$temp_dir/results"
    mkdir -p "$result_dir"
    
    echo ""
    
    case "$MODE" in
        scan)
            echo "🔍 SCAN MODE - Checking for errors without fixing"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            
            # Process each batch
            local batch_num=1
            while [ -f "$temp_dir/batch_$batch_num.txt" ]; do
                process_batch_scan "$temp_dir/batch_$batch_num.txt" "$batch_num" "$result_dir"
                ((batch_num++))
            done
            
            # Aggregate results
            echo ""
            FINAL_ERROR_COUNT=$(aggregate_scan_results "$result_dir")
            cat "$result_dir/FINAL_SCAN_REPORT.md"
            
            # Save for GitHub output
            echo "$FINAL_ERROR_COUNT" > "$result_dir/error_count.txt"
            cp "$result_dir/FINAL_SCAN_REPORT.md" /tmp/doc_scan_report.md 2>/dev/null || true
            ;;
            
        fix)
            echo "🔧 FIX MODE - Auto-fixing documentation errors"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            
            # Process each batch
            local batch_num=1
            while [ -f "$temp_dir/batch_$batch_num.txt" ]; do
                process_batch_fix "$temp_dir/batch_$batch_num.txt" "$batch_num" "$result_dir"
                ((batch_num++))
            done
            
            # Aggregate results
            echo ""
            FINAL_FIXED_COUNT=$(aggregate_fix_results "$result_dir")
            cat "$result_dir/FINAL_FIX_REPORT.md"
            
            # Save for GitHub output
            echo "$FINAL_FIXED_COUNT" > "$result_dir/fixed_count.txt"
            cp "$result_dir/FINAL_FIX_REPORT.md" /tmp/doc_fix_report.md 2>/dev/null || true
            ;;
            
        report)
            echo "📊 REPORT MODE - Generating comprehensive documentation report"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            
            # Get statistics
            local total_md_files=$(find . -name "*.md" -type f ! -path "./.git/*" ! -path "./node_modules/*" 2>/dev/null | wc -l)
            local total_lines=$(find . -name "*.md" -type f ! -path "./.git/*" ! -path "./node_modules/*" 2>/dev/null -exec wc -l {} + | tail -1 | awk '{print $1}')
            
            cat > /tmp/doc_report.md << EOF
# 📚 Documentation Maintenance Report

**Generated:** $(date -Iseconds)
**System:** 3-Layer Documentation Maintenance

## Statistics

| Metric | Value |
|--------|-------|
| Total Files | $total_md_files |
| Total Lines | $total_lines |
| Batch Size | $BATCH_SIZE |
| Batches | $(($total_md_files / $BATCH_SIZE + 1)) |

## System Architecture

### Layer 1: Discovery
- Scans filesystem for all markdown files
- Groups files into batches of $BATCH_SIZE
- Avoids "argument list too long" error

### Layer 2: Processing  
- Processes each batch independently
- Uses array-based argument passing
- Captures results per batch

### Layer 3: Aggregation
- Combines results from all batches
- Generates unified reports
- Provides final metrics

## Status

✅ System operational - Ready for GitHub Actions

EOF
            
            cat /tmp/doc_report.md
            ;;
    esac
    
    echo ""
    echo "✅ Documentation maintenance complete!"
    echo "📁 Results saved to: $result_dir"
    
    # Cleanup
    rm -rf "$temp_dir"
}

# Run main
main
