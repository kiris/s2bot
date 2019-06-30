################################################################
# Misc
################################################################
help: ## Display this messages
	@grep -E '^[a-zA-Z/_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

################################################################
# Development
################################################################
compile: ## Compile code
	sbt compile

clean: ## Delete generated file for target
	sbt clean

test: ## Run unit testing
	sbt test

test/continuous: ## Run unit testing
	sbt ~test
