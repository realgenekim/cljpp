#!/bin/bash

# Test factorial and fibonacci with improved POP-ALL v2 prompt
# This is a focused test to see if the improved prompt helps

set -e

PROGRAM_NUM=${1:-3}
ITERATIONS=${2:-10}

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="experiments/popall-v2-tests/run-${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"

echo "Testing CLJ-PP with POP-ALL v2 (improved prompt)"
echo "Program: Factorial/Fibonacci"
echo "Iterations: $ITERATIONS"
echo "Output: $OUTPUT_DIR"
echo ""

# Read the prompt
PROMPT=$(cat CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md)

# The test program
PROGRAM_SPEC="Write recursive factorial and fibonacci functions with a cond for base cases and recursion.

Requirements:
- factorial(n): returns 1 if n <= 1, else n * factorial(n-1)
- fibonacci(n): returns 0 if n=0, 1 if n=1, else fibonacci(n-1) + fibonacci(n-2)
- Use cond for conditional logic
- Put both functions in namespace examples.test${PROGRAM_NUM}

Write the code in CLJ-PP format."

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
