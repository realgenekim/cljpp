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
	@echo "🧹 Maintenance:"
	@echo "  make clean                - Clean compiled artifacts"
	@echo "  make help                 - Show this help"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

.PHONY: nrepl repl runtests runtests-once format clean help
