#!/bin/bash
# Test POP-ALL v2 variants that focus on preventing underflow
# Hypothesis: POP-ALL will perform better when pitched as "prevents the #1 error you make"

set -x

echo "=========================================="
echo "POP-ALL v2 Experiment: Preventing Underflow"
echo "Date: $(date)"
echo "=========================================="

# Run all three POP-ALL v2 variants
bb bin/test-variant.clj popall-neutral-v2 all 1
bb bin/test-variant.clj popall-persuasive-v2 all 1
bb bin/test-variant.clj popall-negative-v2 all 1

echo "=========================================="
echo "Experiment complete!"
echo "=========================================="
