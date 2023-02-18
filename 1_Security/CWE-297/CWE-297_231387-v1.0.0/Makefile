.DEFAULT_GOAL=help
.PHONY: run build clean help
TARGET = vlc-android/build/outputs/apk


run: $(TARGET) ## Run the test case

build: $(TARGET) ## Build the test case

$(TARGET): .installed
	./compile.sh -a arm64-v8a || true
	./gradlew assemble

clean: ## Clean the test case

.installed: ## Install the dependencies
	sh install-dependencies.sh

help: ## Show this help
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
