#!/bin/bash

# Run fresh Claude instances to write 20 CLJ-PP programs
# Each instance has no memory of previous programs

OUTPUT_DIR="test-output-clj-round2"
mkdir -p "$OUTPUT_DIR"

echo "Starting fresh Claude experiment - CLJ-PP Format"
echo "Each program will be written by a fresh Claude instance with no prior context"
echo ""

# Read the prompts
PROMPTS_FILE="test-prompts-cljpp.txt"

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
    full_prompt="Write a CLJ-PP (Clojure Push-Pop) program for this task.

CLJ-PP SYNTAX RULES:
- Use PUSH-( to open lists (functions, calls, etc.)
- Use PUSH-[ to open vectors (parameters, hiccup, data)
- Use PUSH-{ to open maps
- Use POP to close any container (no ), ], or } except in strings)
- Everything else (symbols, keywords, strings, numbers) - write normally

CRITICAL:
- NEVER write closing delimiters ], }, or )
- Each multi-arity function arity must be wrapped: PUSH-( PUSH-[ args POP body POP
- No reader macros - expand #() to PUSH-( fn PUSH-[ x POP body POP

$prompt

Requirements:
- Use namespace examples.program$i
- Output ONLY the CLJ-PP code, no markdown blocks, no explanations
- Complete, valid CLJ-PP that can be transpiled to Clojure"

    # Call fresh Claude instance
    cljpp_file="$OUTPUT_DIR/$(printf "%02d" "$i")-program.cljpp"
    clj_file="$OUTPUT_DIR/$(printf "%02d" "$i")-program.clj"

    # Run Claude in print mode (fresh instance, no context)
    # Strip markdown code blocks
    if echo "$full_prompt" | claude --print \
        --model sonnet \
        --tools "" 2>/dev/null | \
        sed '/^```/d' > "$cljpp_file"; then

        # Transpile CLJPP to CLJ using our assembler
        if clojure -M -m cljp.core "$cljpp_file" "$clj_file" 2>/dev/null; then
            # Try to load the generated Clojure file
            if clojure -M -e "(load-file \"$clj_file\")" 2>/dev/null; then
                echo "✅ Success (transpiled & loaded)"
            else
                echo "⚠️  Transpiled but failed to load"
                errors=$((errors + 1))
            fi
        else
            echo "⚠️  CLJ-PP transpilation error"
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
