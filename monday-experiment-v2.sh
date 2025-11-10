#!/bin/bash
# Rerun persuasion experiment with improved prompts
# - Added format enforcement (no explanatory text before code)
# - Added preprocessing to strip text before first PUSH-
# - Added new POP-ALL variant (counting-free approach)

set -x

echo "=========================================="
echo "Persuasion Experiment v2"
echo "Date: $(date)"
echo "=========================================="

# Run all four variants on all 40 programs
bb bin/test-variant.clj neutral all 1
bb bin/test-variant.clj persuasive all 1
bb bin/test-variant.clj negative all 1
bb bin/test-variant.clj popall-exp all 1

echo "=========================================="
echo "Experiment complete!"
echo "=========================================="
