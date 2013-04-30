/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
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
		Searcher s = new Searcher(factory);
		// Check if the file has been modified before adding to Index
		if(!s.fileIsModified(pdf))
			return true;
		
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
			
			// Create the Lucene Document
			Document doc = new Document();
			
			// Initialize the indexing fields
			TextField titleField = new TextField("title", (metadata.get("title") == null ? "" : metadata.get("title")), Field.Store.YES);
			TextField contentsField = new TextField("contents", textHandler.toString(), Field.Store.NO);
			TextField keywordField = new TextField("keywords", "", Field.Store.NO);
			StringField pathField = new StringField("path", pdf.getPath(), Field.Store.YES);
			LongField modifiedField = new LongField("modified", pdf.lastModified(), Field.Store.YES);
			
			/*if(pdf.getPath().equals("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\test.pdf")){
				System.out.println("Fixing keywords!");
				keywordField = new TextField("keywords", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc quis tellus sed orci vulputate tristique. Quisque pellentesque cursus nisl, vel pulvinar mauris sollicitudin ac. Aliquam sed nulla tortor. Cras egestas javascript dui accumsan nisl molestie eu tincidunt ipsum luctus. Proin placerat pharetra purus, quis viverra purus gravida in. Pellentesque orci tellus.", Field.Store.NO);
				//keywordField.setBoost(3);
			}*/
			
			// Add fields to document
			doc.add(titleField);
			doc.add(contentsField);
			doc.add(keywordField);
			doc.add(modifiedField);
			doc.add(pathField);
			
			// Check if we should update or add the document to our index
			if(w.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE){
				w.addDocument(doc);
			}else{
				w.updateDocument(new Term("path", pdf.getPath()), doc);
			}
			
			// Finally close and commit changes
			w.close();
		}catch(	IOException | SAXException | TikaException e){
			// Do somethin
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
