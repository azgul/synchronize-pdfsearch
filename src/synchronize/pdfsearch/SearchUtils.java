/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package synchronize.pdfsearch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;

/**
 *
 * @author Lars
 */
public class SearchUtils {
	public static String getAttribute(Path pdf, String attr){
		try{
			if(!Files.exists(pdf) || !Files.isRegularFile(pdf))
				return "";
			

			UserDefinedFileAttributeView view = Files.getFileAttributeView(pdf,UserDefinedFileAttributeView.class);
			
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
