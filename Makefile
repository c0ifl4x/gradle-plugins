GRADLEW := ./gradlew
PLUGINS := esp-idf \
           web-embed

.PHONY: all $(PLUGINS)

all: $(PLUGINS)

$(PLUGINS):
	$(GRADLEW) :plugin-$@:check :plugin-$@:publishToMavenLocal
