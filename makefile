JAVA_SRC_DIR=src/main/java
JAVA_SRC_FILES=$(shell find $(JAVA_SRC_DIR) -name '*.java')
JAVA_TARGET_DIR=target/classes
JAR_FILE=jmxsi.jar
TAR_FILE=jmxsi.tar.gz
README_FILE=README.md
BINS=jmxsi hornetqsi bash_completion/jmxsi

JAVAC=javac
JAR=jar

JAVAC_OPT=-source 1.6 -target 1.6
JAR_OPT=

.PHONY: all clean re tar

all: $(TAR_FILE)

re: clean all

clean:
	rm -rf $(JAVA_TARGET_DIR)
	rm -f $(JAR_FILE)
	rm -f $(TAR_FILE)

$(JAR_FILE): $(README_FILE) $(JAVA_SRC_FILES)
	rm -rf $(JAVA_TARGET_DIR)
	mkdir -p $(JAVA_TARGET_DIR)
	$(JAVAC) $(JAVAC_OPT) -d $(JAVA_TARGET_DIR) $(JAVA_SRC_FILES)
	cp -p $(README_FILE) $(JAVA_TARGET_DIR)
	(cd $(JAVA_TARGET_DIR); $(JAR) $(JAR_OPT) cf /dev/stdout .) > $(JAR_FILE)

tar: $(TAR_FILE)

$(TAR_FILE): $(JAR_FILE)
	tar zcf $(TAR_FILE) $(README_FILE) $(BINS) $(JAR_FILE)

