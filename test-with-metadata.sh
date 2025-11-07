#!/bin/bash

# Enhanced test script with metadata generation and new naming convention
# Usage: ./test-with-metadata.sh VARIANT ITERATIONS [PROGRAM]

set -e

VARIANT=${1:-"baseline"}      # pop-all-v2, baseline, pop-line-all, etc.
ITERATIONS=${2:-10}
PROGRAM=${3:-3}

# New naming convention: variant-YYYYMMDD-HHMMSS
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="experiments/${VARIANT}-${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"

echo "=========================================="
echo "CLJ-PP Experiment with Metadata"
echo "=========================================="
echo "Variant:    $VARIANT"
echo "Iterations: $ITERATIONS"
echo "Program:    $PROGRAM"
echo "Output:     $OUTPUT_DIR"
echo ""

# Determine prompt file based on variant
case "$VARIANT" in
    "baseline"|"pop")
        PROMPT_FILE="CLJPP-PROMPT.md"
        DESCRIPTION="CLJ-PP with explicit POP (baseline)"
        ;;
    "pop-all-v1")
        PROMPT_FILE="CLJPP-PROMPT-WITH-POP-ALL-ONLY.md"
        DESCRIPTION="CLJ-PP with POP-ALL (v1)"
        ;;
    "pop-all-v2")
        PROMPT_FILE="CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md"
        DESCRIPTION="CLJ-PP with POP-ALL (v2 - improved prompt)"
        ;;
    "pop-line")
        PROMPT_FILE="CLJPP-PROMPT-WITH-POP-LINE.md"
        DESCRIPTION="CLJ-PP with POP-LINE only"
        ;;
    "pop-line-all"|"pop-all-and-line")
        PROMPT_FILE="CLJPP-PROMPT-WITH-POP-LINE-ALL.md"
        DESCRIPTION="CLJ-PP with POP-LINE and POP-ALL"
        ;;
    "clj"|"clojure")
        PROMPT_FILE=""
        DESCRIPTION="Regular Clojure (no CLJ-PP)"
        ;;
    *)
        echo "Unknown variant: $VARIANT"
        echo "Valid variants: baseline, pop-all-v1, pop-all-v2, pop-line, pop-line-all, clj"
        exit 1
        ;;
esac

# Generate metadata header
echo "Generating metadata..."
bb bin/generate-metadata.clj \
    --prompt "${PROMPT_FILE}" \
    --iterations "$ITERATIONS" \
    --program "$PROGRAM" \
    --description "$DESCRIPTION" \
    > "$OUTPUT_DIR/metadata.edn"

# Copy prompt for reproducibility
if [ -n "$PROMPT_FILE" ] && [ -f "$PROMPT_FILE" ]; then
    cp "$PROMPT_FILE" "$OUTPUT_DIR/prompt-used.md"
    echo "✓ Copied prompt: $PROMPT_FILE"
fi

# Load prompt
if [ -n "$PROMPT_FILE" ]; then
    PROMPT=$(cat "$PROMPT_FILE")
else
    PROMPT=""  # Regular Clojure, no prompt
fi

# Test program specification
PROGRAM_SPEC="Write recursive factorial and fibonacci functions with a cond for base cases and recursion.

Requirements:
- factorial(n): returns 1 if n <= 1, else n * factorial(n-1)
- fibonacci(n): returns 0 if n=0, 1 if n=1, else fibonacci(n-1) + fibonacci(n-2)
- Use cond for conditional logic
- Put both functions in namespace examples.test${PROGRAM}

Write in CLJ-PP format. Output the code only, starting with PUSH-("

# Run iterations
success_count=0
transpile_errors=0
load_errors=0
start_time=$(date +%s%3N)

for i in $(seq 1 $ITERATIONS); do
    echo "Iteration $i..."
    iter_start=$(date +%s%3N)

    # Generate code
    claude --print "${PROMPT}\n\n${PROGRAM_SPEC}" > "$OUTPUT_DIR/iter${i}.raw.cljpp" 2>&1

    # Clean markdown fences
    sed -e '/^```clojure$/d' -e '/^```$/d' "$OUTPUT_DIR/iter${i}.raw.cljpp" > "$OUTPUT_DIR/iter${i}.cljpp"

    # Try to transpile
    if bin/cljpp "$OUTPUT_DIR/iter${i}.cljpp" "$OUTPUT_DIR/iter${i}.clj" 2>/dev/null; then
        # Try to load
        if clojure -M -e "(load-file \"$OUTPUT_DIR/iter${i}.clj\")" > /dev/null 2>&1; then
            echo "  ✓ Success"
            success_count=$((success_count + 1))
            result="success"
        else
            echo "  ✗ Load error"
            load_errors=$((load_errors + 1))
            result="load-error"
        fi
    else
        echo "  ✗ Transpile error"
        transpile_errors=$((transpile_errors + 1))
        result="transpile-error"
    fi

    iter_end=$(date +%s%3N)
    iter_time=$((iter_end - iter_start))

    # Append iteration result to metadata
    echo "{:iteration $i :success? $([ "$result" = "success" ] && echo "true" || echo "false") :result \"$result\" :time-ms $iter_time}" \
        >> "$OUTPUT_DIR/iterations.edn"
done

end_time=$(date +%s%3N)
total_time=$((end_time - start_time))

# Calculate success rate
success_rate=$(awk "BEGIN {printf \"%.1f\", ($success_count * 100.0 / $ITERATIONS)}")

echo ""
echo "=========================================="
echo "RESULTS"
echo "=========================================="
echo "Success:          $success_count/$ITERATIONS ($success_rate%)"
echo "Transpile errors: $transpile_errors"
echo "Load errors:      $load_errors"
echo "Total time:       ${total_time}ms"
echo ""
echo "Output saved to: $OUTPUT_DIR"

# Update metadata with final results
cat > "$OUTPUT_DIR/results-summary.edn" << EOF
{:success-count $success_count
 :transpile-errors $transpile_errors
 :load-errors $load_errors
 :success-rate $success_rate
 :total-time-ms $total_time
 :iterations-file "iterations.edn"}
EOF

echo "✓ Metadata saved to: $OUTPUT_DIR/metadata.edn"
echo "✓ Results summary: $OUTPUT_DIR/results-summary.edn"
echo "✓ Iteration details: $OUTPUT_DIR/iterations.edn"
