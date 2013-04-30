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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

/**
 *
 * @author Lars
 */
public class PDFSearch {	
	private IndexFactory factory;
	
	public PDFSearch(IndexFactory factory){
		this.factory = factory;
	}
	
	public static void main(String[] args) {
		PDFSearch s = new PDFSearch(new MMapIndexFactory());
		
		long start = System.currentTimeMillis();
		s.addPDFs();
		long end = System.currentTimeMillis();
		System.out.println(end-start + " ms");
		s.search("javascript");
		long end2 = System.currentTimeMillis();
		System.out.println(end2-end + " ms");
	}
	
	public void search(String term){
		try{
			Searcher s = new Searcher(factory);
			s.search(term);
		}catch(IOException e){
			System.err.println("IOException in search");
			System.err.println(e);
		}catch(ParseException e){
			System.err.println("ParseException in search");
			System.err.println(e);
		}
	}
	
	public void addPDFs(){
		Indexer indexer = new Indexer(factory);
		
		// Add PDF Files to index
		for(int i = 1; i < 21; i++)
			indexer.addPDF("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\"+i+".pdf");
		indexer.addPDF("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\test.pdf");
	}
}
