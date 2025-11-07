#!/bin/bash

# Test a single program repeatedly to tune the prompt

PROGRAM_NUM=${1:-3}  # Default to program 3 (recursive - one that failed)
ITERATIONS=${2:-10}

OUTPUT_DIR="test-tuning"
mkdir -p "$OUTPUT_DIR"

# Read the full prompt document
PROMPT_DOC=$(cat CLJPP-PROMPT.md)

# Get the program description
get_prompt() {
    local num=$1
    awk -v n="$num" '
        /^Program [0-9]+:/ {
            if (prog == n) exit
            prog = $2
            prog = substr(prog, 1, length(prog)-1)
            if (prog == n) { printing=1; print; next }
        }
        printing && /^Program [0-9]+:/ { exit }
        printing { print }
    ' test-prompts-cljpp.txt
}

TASK=$(get_prompt "$PROGRAM_NUM")

echo "Testing Program $PROGRAM_NUM: Factorial/Fibonacci"
echo "Running $ITERATIONS iterations..."
echo ""

successes=0
transpile_failures=0
load_failures=0

for i in $(seq 1 $ITERATIONS); do
    printf "Iteration %2d: " "$i"

    # Create full prompt
    full_prompt="You are writing CLJ-PP (Clojure Push-Pop) code.

$PROMPT_DOC

TASK:
$TASK

Requirements:
- Use namespace examples.test$i
- Output ONLY the CLJ-PP code
- No markdown blocks, no explanations
- Complete, valid CLJ-PP"

    cljpp_file="$OUTPUT_DIR/iter${i}.cljpp"
    clj_file="$OUTPUT_DIR/iter${i}.clj"

    # Call Claude
    if echo "$full_prompt" | claude --print --model sonnet --tools "" 2>/dev/null | \
        sed '/^```/d' > "$cljpp_file"; then

        # Transpile
        if ./bin/cljpp "$cljpp_file" "$clj_file" 2>/dev/null; then
            # Load
            if clojure -M -e "(load-file \"$clj_file\")" 2>/dev/null; then
                echo "✅ SUCCESS"
                successes=$((successes + 1))
            else
                echo "⚠️  Transpiled but load failed"
                load_failures=$((load_failures + 1))
            fi
        else
            echo "❌ Transpile failed"
            transpile_failures=$((transpile_failures + 1))
            # Show first error
            ./bin/cljpp "$cljpp_file" "$clj_file" 2>&1 | head -3
        fi
    else
        echo "❌ Claude failed"
        transpile_failures=$((transpile_failures + 1))
    fi

    sleep 0.5
done

echo ""
echo "===== Results ====="
echo "Successes: $successes / $ITERATIONS ($(( successes * 100 / ITERATIONS ))%)"
echo "Transpile failures: $transpile_failures"
echo "Load failures: $load_failures"
echo ""
echo "Files saved to: $OUTPUT_DIR/"
