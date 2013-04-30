/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


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
		
		s.addPDFs();
		s.search("Magnesium");
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
		Indexer i = new Indexer(factory);
		
		// Add PDF Files to index
		i.addPDF("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\ds200gb01spc.pdf");
		i.addPDF("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\ds300gb01spc.pdf");
		i.addPDF("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\ds310gb01spc.pdf");
		i.addPDF("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\ds350non-gmgb02spc.pdf");
	}
}
