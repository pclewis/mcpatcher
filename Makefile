MCVER = 1.4.5
MCJAR = ../bin/minecraft.jar
MCJARV = ../bin/minecraft-$(MCVER).jar
MODJAR = ../mcpatcher-mods/mcpatcher-builtin.jar
MCPATCHER = out/artifacts/mcpatcher/mcpatcher.jar
JIP = $(HOME)/jip-1.2/profile/profile.jar
LAUNCH4J = $(HOME)/launch4j/launch4j
LAUNCH4J_XML = launch4j.xml
CLASSPATH = lib/javassist.jar
PACKAGE = com.pclewis.mcpatcher
DOC_OUT = doc/javadoc
DOC_SRC = $(PACKAGE)
DOC_SRCPATH = shared/src:stubs/src:newcode/src:src:
TEST_OPTS = -ignoresavedmods -ignorecustommods -enableallmods -auto -loglevel 5
TEST_LOG = test.log
GOOD_LOG = good.log
TMPDIR = t.1
FILTER = ./testfilter.pl

.PHONY: default build release run test testfilter javadoc control profile clean modjar restore

default:

build: $(MCPATCHER)
	ant

release: $(MCPATCHER)
	cp -pf $(MCPATCHER) mcpatcher-$(shell java -jar $(MCPATCHER) -version).jar
	sed -e 's/VERSION/$(shell java -jar $(MCPATCHER) -version)/g' $(LAUNCH4J_XML) > $(LAUNCH4J_XML).tmp
	$(LAUNCH4J) $(shell pwd)/$(LAUNCH4J_XML).tmp
	rm -f $(LAUNCH4J_XML).tmp

run: $(MCPATCHER)
	java -jar $(MCPATCHER)

runexp: $(MCPATCHER)
	java -jar $(MCPATCHER) -experimental

test: $(MCPATCHER)
	time java -jar $(MCPATCHER) $(TEST_OPTS) > $(TEST_LOG) 2>&1
	diff -c $(GOOD_LOG) $(TEST_LOG)
	rm -f $(TEST_LOG)

testfilter: $(MCPATCHER)
	time java -jar $(MCPATCHER) $(TEST_OPTS) > $(TEST_LOG) 2>&1
	@$(FILTER) $(TEST_LOG) > $(TEST_LOG).1
	@$(FILTER) $(GOOD_LOG) > $(GOOD_LOG).1
	diff -c $(GOOD_LOG).1 $(TEST_LOG).1
	rm -f $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1

javadoc:
	rm -rf $(DOC_OUT)
	mkdir -p $(DOC_OUT)
	javadoc -protected -splitindex -classpath $(CLASSPATH) -d $(DOC_OUT) $(DOC_SRC) -sourcepath $(DOC_SRCPATH)

control: $(TEST_LOG)
	cp -f $(TEST_LOG) $(GOOD_LOG)

profile: $(MCPATCHER) $(JIP)
	java -Xmx512M -javaagent:$(JIP) -Dprofile.properties=profile.properties -jar $(MCPATCHER) $(TEST_OPTS) > $(TEST_LOG) 2>&1

testclean:
	rm -f $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1

clean: testclean
	rm -rf $(MCPATCHER) $(DOC_OUT) $(MODJAR) out $(LAUNCH4J_XML).tmp mcpatcher-*.jar mcpatcher-*.exe profile.txt profile.xml

modjar: $(MCPATCHER)
	rm -rf $(TMPDIR)
	mkdir -p $(TMPDIR)
	cd $(TMPDIR) && jar -xf ../$(MCPATCHER)
	cd $(TMPDIR) && rm -rf javassist META-INF *.class com/intellij com/pclewis/mcpatcher/*.class
	cd $(TMPDIR) && jar -cf ../$(MODJAR) *
	rm -rf $(TMPDIR)

restore: $(MCJARV)
	cp -pf $(MCJARV) $(MCJAR)
