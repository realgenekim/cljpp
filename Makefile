# Default target when running 'make' without arguments
.DEFAULT_GOAL := help

# Add ~/bin to PATH for Clojure CLI tools
export PATH := $(HOME)/bin:$(PATH)

# Start nREPL server (auto-assigns port, writes to .nrepl-port)
nrepl:
	clojure -M:nrepl

# Start REPL
repl:
	clj

# Run tests with kaocha - watch mode
runtests:
	@echo "Running tests with watcher..."
	bin/kaocha --watch --reporter kaocha.report.progress/report

# Run tests once with fail-fast
runtests-once:
	@echo "Running tests with fail-fast..."
	bin/kaocha --fail-fast

# Format code with standard Clojure style
format:
	npx @chrisoakman/standard-clojure-style fix src test deps.edn

# Build uberjar
uberjar:
	@echo "Building uberjar..."
	@clojure -T:build uber
	@cp target/cljpp.jar bin/cljpp.jar
	@echo "#!/usr/bin/env sh" > bin/cljpp
	@echo '# Find the jar in the same directory as this script' >> bin/cljpp
	@echo 'SCRIPT_DIR="$$(cd "$$(dirname "$$0")" && pwd)"' >> bin/cljpp
	@echo 'exec java -cp "$$SCRIPT_DIR/cljpp.jar" clojure.main -m cljp.core "$$@"' >> bin/cljpp
	@chmod +x bin/cljpp
	@echo "✓ Built target/cljpp.jar and bin/cljpp.jar + bin/cljpp wrapper"
	@echo "  Project-local: ./bin/cljpp"
	@echo "  For global install: make installuberjar"

# Install uberjar and wrapper to ~/bin
installuberjar: uberjar
	@echo "Installing cljpp to ~/bin..."
	@mkdir -p ~/bin
	@cp target/cljpp.jar ~/bin/cljpp.jar
	@cp bin/cljpp ~/bin/cljpp
	@chmod +x ~/bin/cljpp
	@echo "✓ Installed ~/bin/cljpp and ~/bin/cljpp.jar"
	@echo "  Run 'cljpp input.cljpp' from anywhere"
	@echo "  (Make sure ~/bin is in your PATH)"

# Install cljpp command globally to ~/bin (symlink version for dev)
install:
	@echo "Installing cljpp to ~/bin/cljpp..."
	@mkdir -p ~/bin
	@ln -sf $(PWD)/bin/cljpp ~/bin/cljpp
	@echo "✓ Installed! Run 'cljpp input.cljpp' from anywhere"
	@echo "  (Make sure ~/bin is in your PATH)"

# Uninstall cljpp command
uninstall:
	@echo "Removing ~/bin/cljpp and ~/bin/cljpp.jar..."
	@rm -f ~/bin/cljpp ~/bin/cljpp.jar
	@echo "✓ Uninstalled"

# Clean compiled artifacts
clean:
	rm -rf .cpcache/ .nrepl-port target/

# Help
help:
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "  CLJP Tokenizer - Make Commands"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "🔧 Setup:"
	@echo "  make nrepl                - Start nREPL server (auto-port, writes to .nrepl-port)"
	@echo ""
	@echo "🧪 Testing:"
	@echo "  make runtests             - Run tests with watcher"
	@echo "  make runtests-once        - Run tests once with fail-fast"
	@echo ""
	@echo "🚀 Development:"
	@echo "  make repl                 - Start basic REPL"
	@echo "  make format               - Format code with standard Clojure style"
	@echo ""
	@echo "📦 Build & Install:"
	@echo "  make uberjar              - Build uberjar and wrapper script"
	@echo "  make installuberjar       - Build + copy cljpp to ~/bin (recommended!)"
	@echo "  make install              - Symlink 'cljpp' to ~/bin (dev mode)"
	@echo "  make uninstall            - Remove 'cljpp' from ~/bin"
	@echo ""
	@echo "🧹 Maintenance:"
	@echo "  make clean                - Clean compiled artifacts"
	@echo "  make help                 - Show this help"
	@echo ""
	@echo "🧪 Fresh Instance Tests:"
	@echo "  make test-generate-clj ITERS=20 PROG=3"
	@echo "      Test regular Clojure generation"
	@echo "  make test-generate-cljpp-pop ITERS=20 PROG=3"
	@echo "      Test CLJ-PP with explicit POP (baseline)"
	@echo "  make test-generate-cljpp-pop-all ITERS=20 PROG=3"
	@echo "      Test CLJ-PP with POP-ALL"
	@echo "  make test-generate-cljpp-pop-line ITERS=20 PROG=3"
	@echo "      Test CLJ-PP with POP-LINE"
	@echo "  make test-generate-cljpp-pop-all-and-line ITERS=20 PROG=3"
	@echo "      Test CLJ-PP with POP-ALL and POP-LINE"
	@echo "  Default: ITERS=10 PROG=3 (factorial/fibonacci)"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Test targets for code generation experiments
# Usage: make test-generate-clj ITERS=20 PROG=3
ITERS ?= 10
PROG ?= 3

test-generate-clj:
	@./test-one-program-clj.sh $(PROG) $(ITERS)

test-generate-cljpp-pop:
	@./test-one-program.sh $(PROG) $(ITERS)

test-generate-cljpp-pop-all:
	@./test-one-program-with-pop-all-only.sh $(PROG) $(ITERS)

test-generate-cljpp-pop-line:
	@./test-one-program-with-pop-line.sh $(PROG) $(ITERS)

test-generate-cljpp-pop-all-and-line:
	@./test-one-program-with-pop-line-all.sh $(PROG) $(ITERS)

.PHONY: nrepl repl runtests runtests-once format uberjar installuberjar install uninstall clean help \
        test-generate-clj test-generate-cljpp-pop test-generate-cljpp-pop-all test-generate-cljpp-pop-line test-generate-cljpp-pop-all-and-line
