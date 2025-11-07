#!/bin/bash

# Test POP-ALL v2 with explicit instruction to print code only

set -e

ITERATIONS=${1:-20}

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="experiments/popall-v2-direct/run-${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"

echo "Testing CLJ-PP with POP-ALL v2 (direct code output)"
echo "Iterations: $ITERATIONS"
echo "Output: $OUTPUT_DIR"
echo ""

# Read the prompt
PROMPT=$(cat CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md)

# The test program with EXPLICIT instruction to just print code
PROGRAM_SPEC="Write recursive factorial and fibonacci functions with a cond for base cases and recursion.

Requirements:
- factorial(n): returns 1 if n <= 1, else n * factorial(n-1)
- fibonacci(n): returns 0 if n=0, 1 if n=1, else fibonacci(n-1) + fibonacci(n-2)
- Use cond for conditional logic
- Put both functions in namespace examples.test3

IMPORTANT: Output ONLY the CLJ-PP code. No explanations, no asking for permission, just the code starting with PUSH-( ns examples.test3 POP-ALL"

success_count=0
transpile_errors=0
load_errors=0

for i in $(seq 1 $ITERATIONS); do
    echo "Iteration $i..."

    # Generate CLJ-PP code
    if claude --print "${PROMPT}\n\n${PROGRAM_SPEC}" > "$OUTPUT_DIR/iter${i}.cljpp" 2>&1; then
        # Try to transpile
        if bin/cljpp "$OUTPUT_DIR/iter${i}.cljpp" > "$OUTPUT_DIR/iter${i}.clj" 2>&1; then
            # Try to load
            if clojure -M -e "(load-file \"$OUTPUT_DIR/iter${i}.clj\")" > /dev/null 2>&1; then
                echo "  ✓ Success"
                success_count=$((success_count + 1))
            else
                echo "  ✗ Load error"
                load_errors=$((load_errors + 1))
            fi
        else
            echo "  ✗ Transpile error"
            transpile_errors=$((transpile_errors + 1))
        fi
    else
        echo "  ✗ Generation failed"
        transpile_errors=$((transpile_errors + 1))
    fi
done

echo ""
echo "=========================================="
echo "RESULTS"
echo "=========================================="
echo "Success:          $success_count/$ITERATIONS"
echo "Transpile errors: $transpile_errors"
echo "Load errors:      $load_errors"
echo ""

success_pct=$((success_count * 100 / ITERATIONS))
echo "Success rate: $success_pct%"
echo ""
echo "Output saved to: $OUTPUT_DIR"

# Save results
cat > "$OUTPUT_DIR/RESULTS.md" << EOF
# POP-ALL v2 Test Results

**Date:** $(date)
**Iterations:** $ITERATIONS

## Results

- Success: $success_count/$ITERATIONS ($success_pct%)
- Transpile errors: $transpile_errors
- Load errors: $load_errors

## Comparison

| Approach | Success Rate |
|----------|--------------|
| CLJ-PP Baseline (explicit POP) | 90% (9/10) |
| CLJ-PP + POP-ALL v1 | 80% (8/10) |
| CLJ-PP + POP-ALL v2 | $success_pct% ($success_count/$ITERATIONS) |

## Hypothesis

POP-ALL v2 improved the prompt with:
- Explicit "THE CRITICAL RULE" section
- WRONG pattern examples with ❌
- "POP-ALL means STOP" repeated throughout
- Dedicated error section for "POPs After POP-ALL"

Goal: Achieve 90%+ success rate (matching baseline).
EOF
