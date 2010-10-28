package com.pclewis.mcpatcher.archive;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Abstract different kinds of archives. Paths ALWAYS use / as a separator.
 */
public interface Archive {
    /**
     * @return list of paths this archive contains
     */
    List<String> getPaths();

    /**
     * @param name path to open
     * @return input stream for the specified file
     */
    InputStream getInputStream(String name) throws FileNotFoundException;

    /**
     * @return source file path
     */
    String getPath();
}
