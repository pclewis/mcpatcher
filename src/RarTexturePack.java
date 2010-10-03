import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

import java.io.*;
import java.util.List;

public class RarTexturePack extends TexturePack {
	Archive rar;
	List<FileHeader> headers;
	public RarTexturePack(String path) throws RarException, IOException {
		super();
		rar = new Archive(new File(path));
		headers = rar.getFileHeaders();
	}

	protected DataInputStream openFile(String name) throws IOException {
		for(FileHeader h : headers) {
			String fileName = h.getFileNameString().replace("\\", "/");
			if(fileName.equals(name)) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(h.getDataSize());
				try {
					rar.extractFile(h, baos);
				} catch(RarException e) {
					throw new IOException("Rar exception", e);
				}
				return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
			}
		}
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
