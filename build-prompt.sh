#!/bin/bash
# Build CLJPP prompt from plan and deploy it

set -e

PLAN_FILE="plans/variant-hybrid-minimal-dense.md"
OUTPUT_FILE="CLJPP-PROMPT-v4.md"

if [ ! -f "$PLAN_FILE" ]; then
    echo "Error: Plan file not found: $PLAN_FILE"
    exit 1
fi

echo "Building CLJPP prompt v4 from $PLAN_FILE..."

# Extract just the prompt section (after "## The Prompt")
awk '/^## The Prompt$/,0' "$PLAN_FILE" | tail -n +2 > "$OUTPUT_FILE"

echo "✓ Created $OUTPUT_FILE"
echo ""
echo "Prompt stats:"
echo "  Lines: $(wc -l < "$OUTPUT_FILE")"
echo "  Words: $(wc -w < "$OUTPUT_FILE")"
echo "  Chars: $(wc -c < "$OUTPUT_FILE")"
echo ""
echo "To test this prompt:"
echo "  ./test-one-program.sh <program_num> <iterations> v4"
echo ""
echo "To run comprehensive test:"
echo "  ./run-comprehensive-experiment.sh v4"
