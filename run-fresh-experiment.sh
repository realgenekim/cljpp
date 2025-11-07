#!/bin/bash

# Run fresh Claude instances to write 20 Clojure programs
# Each instance has no memory of previous programs

OUTPUT_DIR="test-output-clj-round2"
mkdir -p "$OUTPUT_DIR"

echo "Starting fresh Claude experiment - Round 2"
echo "Each program will be written by a fresh Claude instance with no prior context"
echo ""

# Read the prompts
PROMPTS_FILE="test-prompts.txt"

# Function to extract prompt for a specific program number
get_prompt() {
    local num=$1
    awk -v n="$num" '
        /^Program [0-9]+:/ {
            if (prog == n) exit
            prog = $2
            prog = substr(prog, 1, length(prog)-1)  # Remove colon
            if (prog == n) { printing=1; print; next }
        }
        printing && /^Program [0-9]+:/ { exit }
        printing { print }
    ' "$PROMPTS_FILE"
}

# Counter for errors
errors=0
total=0

# Process each program
for i in {1..20}; do
    printf "Program %02d: " "$i"
    total=$((total + 1))

    # Get the prompt for this program
    prompt=$(get_prompt "$i")

    if [ -z "$prompt" ]; then
        echo "❌ Could not find prompt"
        errors=$((errors + 1))
        continue
    fi

    # Create full prompt for Claude
    full_prompt="Write a Clojure program for this task. Output ONLY the Clojure code, no explanations.

$prompt

Requirements:
- Use namespace examples.program$i
- Write idiomatic Clojure
- No comments
- Complete, runnable code"

    # Call fresh Claude instance
    output_file="$OUTPUT_DIR/$(printf "%02d" "$i")-program.clj"

    # Run Claude in print mode (fresh instance, no context)
    # Strip markdown code blocks
    if echo "$full_prompt" | claude --print \
        --model sonnet \
        --tools "" 2>/dev/null | \
        sed '/^```/d' > "$output_file"; then

        # Try to load the file with Clojure to check syntax
        if clojure -M -e "(load-file \"$output_file\")" 2>/dev/null; then
            echo "✅ Success"
        else
            echo "⚠️  Syntax error"
            errors=$((errors + 1))
        fi
    else
        echo "❌ Claude failed"
        errors=$((errors + 1))
    fi

    # Small delay to avoid rate limiting
    sleep 1
done

echo ""
echo "===== Results ====="
echo "Total programs: $total"
echo "Successful: $((total - errors))"
echo "Errors: $errors"
echo "Success rate: $(( (total - errors) * 100 / total ))%"
