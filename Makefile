MCVER = 1.4
MCJAR = ../bin/minecraft.jar
MCJARV = ../bin/minecraft-$(MCVER).jar
MODJAR = ../mcpatcher-mods/mcpatcher-builtin.jar
MCPATCHER = out/artifacts/mcpatcher/mcpatcher.jar
PROFILER4J = $(HOME)/profiler4j-1.0-beta2/agent.jar
CLASSPATH = lib/javassist.jar:jgoodies-common-1.1.1.jar:jgoodies-forms-1.4.0.jar
PACKAGE = com.pclewis.mcpatcher
DOC_OUT = doc/javadoc
DOC_SRC = $(PACKAGE)
DOC_SRCPATH = utils/src:stubs/src:newcode/src:src:
TEST_LOG = test.log
GOOD_LOG = good.log
TMPDIR = t.1

default:

run: $(MCPATCHER)
	java -jar $(MCPATCHER)

test: $(MCPATCHER)
	java -jar $(MCPATCHER) -ignorecustommods -auto -loglevel 5 > $(TEST_LOG) 2>&1
	diff -c $(GOOD_LOG) $(TEST_LOG)
	rm -f $(TEST_LOG)

javadoc:
	rm -rf $(DOC_OUT)
	mkdir -p $(DOC_OUT)
	javadoc -protected -splitindex -classpath $(CLASSPATH) -d $(DOC_OUT) $(DOC_SRC) -sourcepath $(DOC_SRCPATH)

control: $(TEST_LOG)
	cp -f $(TEST_LOG) $(GOOD_LOG)

profile: $(MCPATCHER) $(PROFILER4J)
	java -Xmx512M -javaagent:$(PROFILER4J)=waitconn=true,verbosity=1 -jar $(MCPATCHER)

clean:
	rm -rf $(TEST_LOG) $(MCPATCHER) $(DOC_OUT)

modjar: $(MCPATCHER)
	rm -rf $(TMPDIR)
	mkdir -p $(TMPDIR)
	cd $(TMPDIR) && jar -xf ../$(MCPATCHER)
	cd $(TMPDIR) && rm -rf javassist META-INF *.class com/intellij com/jgoodies com/pclewis/mcpatcher/*.class
	cd $(TMPDIR) && jar -cf ../$(MODJAR) *
	rm -rf $(TMPDIR)

restore: $(MCJARV)
	cp -pf $(MCJARV) $(MCJAR)
