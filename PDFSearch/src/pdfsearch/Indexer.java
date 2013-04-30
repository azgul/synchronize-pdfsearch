/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Lars
 */
public class Indexer {
	private IndexFactory factory;
	
	public Indexer(IndexFactory factory){
		this.factory = factory;
	}
	
	public boolean addPDF(String pdfPath){
		File f = new File(pdfPath);
		
		if(!f.isFile())
			return false;
		
		return addPDF(f);
	}
	
	public boolean deletePDF(String path){
		try{
			IndexWriter w = factory.getIndexWriter();
			w.deleteDocuments(new Term("path", path));
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean addPDF(File pdf){
		try{
			IndexWriter w = factory.getIndexWriter();
			
			// read and add pdf contents
			
			ContentHandler textHandler;
			PDFParser parser = new PDFParser();
			Metadata metadata;
			try (InputStream input = new FileInputStream(pdf)) {
				textHandler = new BodyContentHandler(-1);
				
				metadata = new Metadata();
				parser.parse(input, textHandler, metadata, new ParseContext());
			}
			
			Document doc = new Document();
			
			doc.add(new TextField("title", (metadata.get("title") == null ? "" : metadata.get("title")), Field.Store.YES));
			doc.add(new TextField("contents", textHandler.toString(), Field.Store.NO));
			doc.add(new LongField("modified", pdf.lastModified(), Field.Store.NO));
			
			Field pathField = new StringField("path", pdf.getPath(), Field.Store.YES);
			doc.add(pathField);
			
			if(w.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE){
				w.addDocument(doc);
			}else{
				w.updateDocument(new Term("path", pdf.getPath()), doc);
			}
			
			w.close();
		}catch(	IOException | SAXException | TikaException e){
			// Do somethin
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
