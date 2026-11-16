.PHONY: help build test clean run install install-bin uninstall-bin publish-local

# Ink — terminal design system. Modules: library (core), mosaic (Mosaic components), example (Arcana-style demo window)

help:
	@echo "Ink — Terminal Design System"
	@echo ""
	@echo "Targets:"
	@echo "  make build          Compile and test every module"
	@echo "  make test           Run the tests only"
	@echo "  make run            Build the demo and run it (needs a real terminal; q quits)"
	@echo "  make install-bin    Put an `ink` command in ~/.local/bin (a symlink to the built app)"
	@echo "  make uninstall-bin  Remove that command"
	@echo "  make publish-local  Publish com.idea:ink to ~/.m2 (what Arcana / CosmoCLI depend on)"
	@echo "  make clean          Remove build output"

build:
	./gradlew build

test:
	./gradlew test

install:
	./gradlew :example:installDist

# `gradle run` would take the terminal's input for itself, so the installed start script is run directly.
run: install
	example/build/install/ink/bin/ink

BIN_DIR ?= $(HOME)/.local/bin

# The start script finds its jars next to itself, so it is linked, not copied: `make clean` removes them, `make install` brings them back.
install-bin: install
	@mkdir -p "$(BIN_DIR)"
	ln -sfn "$(CURDIR)/example/build/install/ink/bin/ink" "$(BIN_DIR)/ink"
	@echo "Linked $(BIN_DIR)/ink -> the built app. Run: ink"

uninstall-bin:
	rm -f "$(BIN_DIR)/ink"

publish-local:
	./gradlew publishToMavenLocal
	@echo "Published com.idea:ink to ~/.m2/repository/com/idea/"

clean:
	./gradlew clean
