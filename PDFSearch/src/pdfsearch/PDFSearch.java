/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

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
		
		//s.addPDFs();
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
