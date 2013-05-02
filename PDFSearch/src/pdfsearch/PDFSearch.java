/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import ucar.nc2.dataset.conv.BUFRConvention;

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
		String phrase = "piglets";
		
		PDFSearch s = new PDFSearch(new MMapIndexFactory());
		
		s.testBuildIndex();
		s.testSearch(phrase);
	}
	
	public void testSearch(String phrase){
		System.out.println("Searching for '" + phrase + "' in index...");
		long startSearch = System.currentTimeMillis();
		search(phrase);
		long endSearch = System.currentTimeMillis();
		System.out.println("Finished search! Time spent: "+ (endSearch-startSearch) + " ms");
	}
	
	public void testBuildIndex(){
		System.out.println("Adding/Updating PDFs in index");
		System.out.println("-------------------------------------------------");
		long start = System.currentTimeMillis();
		addPDFs();
		long end = System.currentTimeMillis();
		System.out.println("Finished building index! Time spent: "+ (end-start) + " ms");
		System.out.println("-------------------------------------------------");
	}
	
	public void testFileIsModified(){
		Searcher s = new Searcher(factory);
		
		File f = new File("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\PDFs\\1.pdf");
		
		System.out.println(s.fileIsModified(f));
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
	
	public ArrayList<File> getFiles(String dir){
		File start = new File(dir);
		return getFiles(start);
	}
	
	FileFilter pdfAndDirectoryFileFilter = new FileFilter(){
		@Override
		public boolean accept(File file) {
			// If this is a directory, we have a valid type
			if(file.isDirectory())
				return true;

			// Check whether we have a PDF file
			byte[] pdfMagicNumbers = {0x25, 0x50, 0x44, 0x46};
			try(FileInputStream ins = new FileInputStream(file)){
				for(int i = 0; i < pdfMagicNumbers.length; i++){
					if(ins.read() != pdfMagicNumbers[i])
						return false;
				}
			}catch(IOException e){
				return false;
			}
			return true;
		}
	};
	
	public ArrayList<File> getFiles(File start){		
		if(!start.isDirectory())
			return new ArrayList<File>();
		
		// Create an arraylist containing all PDF files
		ArrayList<File> pdfs = new ArrayList<File>();
		
		// Get the files and directories in our current filter
		File[] files = start.listFiles(pdfAndDirectoryFileFilter);
		
		// Iterate through all files/folders and add files to our PDF ArrayList, and recurse on subfolders
		for(File f : files){
			if(f.isDirectory())
				pdfs.addAll(getFiles(f));
			else
				pdfs.add(f);
		}
		
		return pdfs;
	}
	
	public void addPDFs(){
		Indexer indexer = new Indexer(factory);
		
		File dir = new File("C:\\Users\\Lars\\Documents\\GitHub\\synchronize-pdfsearch\\Hamlet-PDFs");
		
		ArrayList<File> files = getFiles(dir);
		for(File f : files)
			indexer.addPDF(0, f);
	}
}
