package com.pclewis.mcpatcher;

import java.io.IOException;
import java.util.jar.JarFile;

public class JarTexturePack extends ZipTexturePack {
	public JarTexturePack(String path) throws IOException {
		super(new JarFile(path, false));
	}
}
