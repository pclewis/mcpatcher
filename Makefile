MCVER = 1.4_01
MCJAR = ../bin/minecraft.jar
MCJARV = ../bin/minecraft-$(MCVER).jar
MODJAR = ../mcpatcher-mods/mcpatcher-builtin.jar
MCPATCHER = out/artifacts/mcpatcher/mcpatcher.jar
PROFILER4J = $(HOME)/profiler4j-1.0-beta2/agent.jar
CLASSPATH = lib/javassist.jar
PACKAGE = com.pclewis.mcpatcher
DOC_OUT = doc/javadoc
DOC_SRC = $(PACKAGE)
DOC_SRCPATH = utils/src:stubs/src:newcode/src:src:
TEST_LOG = test.log
GOOD_LOG = good.log
TMPDIR = t.1
FILTER = perl -p -e 's/@[[:digit:]]+/@.../g; s/(INVOKE|GET|PUT)(VIRTUAL|STATIC|INTERFACE|FIELD)( 0x[[:xdigit:]]{2}){2}/$$1$$2 0x.. 0x../g;'

.PHONY: default run test testfilter javadoc control profile clean modjar restore

default: build.xml
	ant

run: $(MCPATCHER)
	java -jar $(MCPATCHER)

test: $(MCPATCHER)
	time java -jar $(MCPATCHER) -ignorecustommods -auto -loglevel 5 > $(TEST_LOG) 2>&1
	diff -c $(GOOD_LOG) $(TEST_LOG)
	rm -f $(TEST_LOG)

testfilter: $(MCPATCHER)
	time java -jar $(MCPATCHER) -ignorecustommods -auto -loglevel 5 > $(TEST_LOG) 2>&1
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

profile: $(MCPATCHER) $(PROFILER4J)
	java -Xmx512M -javaagent:$(PROFILER4J)=waitconn=true,verbosity=1 -jar $(MCPATCHER)

clean:
	rm -rf $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1 $(MCPATCHER) $(DOC_OUT) out

modjar: $(MCPATCHER)
	rm -rf $(TMPDIR)
	mkdir -p $(TMPDIR)
	cd $(TMPDIR) && jar -xf ../$(MCPATCHER)
	cd $(TMPDIR) && rm -rf javassist META-INF *.class com/intellij com/pclewis/mcpatcher/*.class
	cd $(TMPDIR) && jar -cf ../$(MODJAR) *
	rm -rf $(TMPDIR)

restore: $(MCJARV)
	cp -pf $(MCJARV) $(MCJAR)
