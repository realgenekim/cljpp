#!/bin/bash

# Parallelized Comprehensive CLJ-PP Experiment Runner
# Tests all 20 programs with 3 different approaches IN PARALLEL
# 1. Regular Clojure (baseline)
# 2. CLJ-PP with explicit POP only
# 3. CLJ-PP with POP-ALL (improved prompt v2)

set -e

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RESULTS_DIR="experiments/comprehensive-test-parallel-${TIMESTAMP}"
mkdir -p "${RESULTS_DIR}"

echo "========================================"
echo "Comprehensive CLJ-PP Experiment (PARALLEL)"
echo "Testing all 20 programs with 3 approaches"
echo "Results will be saved to: ${RESULTS_DIR}"
echo "========================================"
echo ""

MAX_PARALLEL=10  # Run 10 programs in parallel max

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
    ' test-prompts.txt
}

# Function to test a single program (all 3 approaches)
test_program() {
    local i=$1
    local PROGRAM_NUM=$(printf "%02d" "$i")
    local PROGRAM_PROMPT=$(get_prompt "$i")

    if [ -z "$PROGRAM_PROMPT" ]; then
        return
    fi

    local DESCRIPTION=$(echo "$PROGRAM_PROMPT" | head -1)
    local PROGRAM_SPEC="Write a Clojure program for this task:

$PROGRAM_PROMPT

Requirements:
- Use namespace examples.program${i}
- Write idiomatic Clojure
- Output ONLY the code, starting with (ns or PUSH-(
- No explanations or markdown"

    # Test 1: Regular Clojure
    claude --print "$PROGRAM_SPEC" > "${RESULTS_DIR}/${PROGRAM_NUM}-clj.raw" 2>&1
    sed -e '/^```clojure$/d' -e '/^```$/d' "${RESULTS_DIR}/${PROGRAM_NUM}-clj.raw" > "${RESULTS_DIR}/${PROGRAM_NUM}-clj.clj"
    if clojure -M -e "(load-file \"${RESULTS_DIR}/${PROGRAM_NUM}-clj.clj\")" > /dev/null 2>&1; then
        echo "${PROGRAM_NUM},clj,success" >> "${RESULTS_DIR}/results.csv"
    else
        echo "${PROGRAM_NUM},clj,failed" >> "${RESULTS_DIR}/results.csv"
    fi

    # Test 2: CLJ-PP with explicit POP only
    PROMPT_POP=$(cat CLJPP-PROMPT.md)
    claude --print "${PROMPT_POP}

${PROGRAM_SPEC}" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.raw" 2>&1
    sed -e '/^```clojure$/d' -e '/^```$/d' "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.raw" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.cljpp"
    if bin/cljpp "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.cljpp" "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.clj" 2>/dev/null; then
        if clojure -M -e "(load-file \"${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.clj\")" > /dev/null 2>&1; then
            echo "${PROGRAM_NUM},cljpp-pop,success" >> "${RESULTS_DIR}/results.csv"
        else
            echo "${PROGRAM_NUM},cljpp-pop,load-error" >> "${RESULTS_DIR}/results.csv"
        fi
    else
        echo "${PROGRAM_NUM},cljpp-pop,transpile-error" >> "${RESULTS_DIR}/results.csv"
    fi

    # Test 3: CLJ-PP with POP-ALL (v2 improved prompt)
    PROMPT_POPALL=$(cat CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md)
    claude --print "${PROMPT_POPALL}

${PROGRAM_SPEC}" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.raw" 2>&1
    sed -e '/^```clojure$/d' -e '/^```$/d' "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.raw" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.cljpp"
    if bin/cljpp "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.cljpp" "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.clj" 2>/dev/null; then
        if clojure -M -e "(load-file \"${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.clj\")" > /dev/null 2>&1; then
            echo "${PROGRAM_NUM},cljpp-popall,success" >> "${RESULTS_DIR}/results.csv"
        else
            echo "${PROGRAM_NUM},cljpp-popall,load-error" >> "${RESULTS_DIR}/results.csv"
        fi
    else
        echo "${PROGRAM_NUM},cljpp-popall,transpile-error" >> "${RESULTS_DIR}/results.csv"
    fi

    echo "Program ${PROGRAM_NUM} complete" >> "${RESULTS_DIR}/progress.log"
}

# Export function so background jobs can use it
export -f test_program
export -f get_prompt
export RESULTS_DIR

# Initialize results file
echo "program,approach,result" > "${RESULTS_DIR}/results.csv"
echo "" > "${RESULTS_DIR}/progress.log"

# Run all programs in parallel with max concurrent limit
pids=()
for i in {1..20}; do
    # Run in background
    test_program $i &
    pids+=($!)

    # Limit parallelism
    if [ ${#pids[@]} -ge $MAX_PARALLEL ]; then
        wait ${pids[0]}
        pids=("${pids[@]:1}")
    fi
done

# Wait for all remaining jobs
wait

echo "=========================================="
echo "All tests complete!"
echo "=========================================="

# Count results
clj_success=$(grep ",clj,success" "${RESULTS_DIR}/results.csv" | wc -l | tr -d ' ')
pop_success=$(grep ",cljpp-pop,success" "${RESULTS_DIR}/results.csv" | wc -l | tr -d ' ')
popall_success=$(grep ",cljpp-popall,success" "${RESULTS_DIR}/results.csv" | wc -l | tr -d ' ')

echo "Regular Clojure:        ${clj_success}/20"
echo "CLJ-PP (explicit POP):  ${pop_success}/20"
echo "CLJ-PP (POP-ALL v2):    ${popall_success}/20"
echo ""
echo "Results saved to: ${RESULTS_DIR}/results.csv"
