# CLJ-PP Tokenizer Project

This project implements a stack-based intermediate format for generating Clojure code.

## Commands

```bash
# Run tests
make runtests-once

# Test specific features
./test-one-program.sh 3 10
```

## File Organization

- `src/` - Source code (tokenizer, assembler)
- `test/` - Unit tests
- `experiments/` - All experiment runs (timestamped, never overwritten)
- Test output directories contain results from experiments
