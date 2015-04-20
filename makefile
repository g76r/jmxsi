JAVA_SRC_DIR=src/main/java
JAVA_SRC_FILES=$(shell find $(JAVA_SRC_DIR) -name '*.java')
JAVA_TARGET_DIR=target/classes
JAR_FILE=jmxsi.jar
README_FILE=README.md

JAVAC=javac
JAR=jar

JAVAC_OPT=-source 1.6 -target 1.6
JAR_OPT=

.PHONY: all clean re

all: $(JAR_FILE)

re: clean all

clean:
	rm -rf $(JAVA_TARGET_DIR)
	rm -f $(JAR_FILE)

$(JAR_FILE): $(README_FILE) $(JAVA_SRC_FILES)
	rm -rf $(JAVA_TARGET_DIR)
	mkdir -p $(JAVA_TARGET_DIR)
	$(JAVAC) $(JAVAC_OPT) -d $(JAVA_TARGET_DIR) $(JAVA_SRC_FILES)
	cp -p $(README_FILE) $(JAVA_TARGET_DIR)
	$(JAR) $(JAR_OPT) cf $(JAR_FILE) $(JAVA_TARGET_DIR)
