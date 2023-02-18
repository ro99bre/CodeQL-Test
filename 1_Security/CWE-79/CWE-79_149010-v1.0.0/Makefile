.DEFAULT_GOAL=help
.PHONY: run build clean help
TARGET = build/com/ecyrd/jspwiki


run: $(TARGET) ## Run the test case

build: $(TARGET) ## Build the test case

$(TARGET): .installed
	ant -Dbuild.properties=build.properties -find

clean: ## Clean the test case
	ant clean

.installed: ## Install the dependencies
	sh install-dependencies.sh

help: ## Show this help
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
