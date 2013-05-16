/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;

/**
 *
 * @author Lars
 */
public class SearchUtils {
	public static String getAttribute(File pdf, String attr){
		try{
			if(!pdf.exists() || !pdf.isFile())
				return "";
			
			Path pdfPath = FileSystems.getDefault().getPath(pdf.getPath());

			UserDefinedFileAttributeView view = Files.getFileAttributeView(pdfPath,UserDefinedFileAttributeView.class);
			
			if(view.list().isEmpty() || !view.list().contains(attr))
				return "";
			
			int size = view.size(attr);
			ByteBuffer buf = ByteBuffer.allocate(size);
			view.read(attr, buf);
			buf.flip();
			return Charset.defaultCharset().decode(buf).toString();
		}catch(IOException e){
			return "";
		}
	}
}
