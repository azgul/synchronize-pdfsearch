/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Lars
 */
public class Searcher {
	private IndexFactory factory;
	private Path searchPath;
	private DirectoryStream.Filter<Path> filter;
	
	public Searcher(IndexFactory factory, Path searchPath, DirectoryStream.Filter<Path> filter) {
		this(factory, searchPath);
		this.filter = filter;
	}
	
	public Searcher(IndexFactory factory, Path searchPath) {
		this(factory);
		this.searchPath = searchPath;
	}
	
	public Searcher(IndexFactory factory){
		this.factory = factory;
		this.searchPath = FileSystems.getDefault().getPath("pdfs");
	}
	
	public List<SearchResult> search(String searchTerm) throws IOException, ParseException{
		return search(searchTerm, new ArrayList<Integer>(), new ArrayList<String>());
	}
	
	public List<SearchResult> search(String searchTerm, List<Integer> categories) throws IOException, ParseException{
		return search(searchTerm, categories, new ArrayList<String>());
	}
	
	public List<SearchResult> search(String searchTerm, List<Integer> categories, List<String> languages) throws IOException, ParseException{
		Directory index = factory.getIndex();
		Analyzer analyzer = factory.getAnalyzer();
		
		String[] searchFields = {"title", "contents", "keywords"};
		
		Query mainQuery;
		Query fieldQuery = new MultiFieldQueryParser(Version.LUCENE_42, searchFields, analyzer).parse(searchTerm);
		if(categories.isEmpty() && languages.isEmpty())
			mainQuery = fieldQuery;
		else{
			// Our main query should be a boolean query
			mainQuery = new BooleanQuery();
			
			// Add the field and category queries
			BooleanQuery bq = (BooleanQuery)mainQuery;
			bq.add(fieldQuery, BooleanClause.Occur.MUST);
			
			// Create our inner boolean query, which should match any of the categories or languages
			if(!categories.isEmpty()){
				BooleanQuery categoryQuery = generateQueryI(categories, "category");
				bq.add(categoryQuery, BooleanClause.Occur.MUST);	
			}
			if(!languages.isEmpty()){
				BooleanQuery categoryQuery = generateQueryS(languages, "language");
				bq.add(categoryQuery, BooleanClause.Occur.MUST);	
			}
		}
		
		ArrayList<SearchResult> searchResult = new ArrayList<>();
		if(!DirectoryReader.indexExists(index)) {
			// if no index exists we build the index
			buildIndex();
		}
		try (IndexReader reader = DirectoryReader.open(index)) {
			IndexSearcher searcher = new IndexSearcher(reader);
			
			TopDocs results = searcher.search(mainQuery, Integer.MAX_VALUE);
			ScoreDoc[] hits = results.scoreDocs;

			// 4. display results
			for(int i=0;i<hits.length;++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				
				int category = (d.get("category") == null ? 0 : Integer.parseInt(d.get("category")));
				String language = d.get("language");
				String path = d.get("path");
				
				searchResult.add(new SearchResult(category, language, path));
			}
		}
		
		return searchResult;
	}
	
	protected void generateIndex() {
		
	}
	
	protected BooleanQuery generateQueryI(List<Integer> items, String field){
		BooleanQuery q = new BooleanQuery();

		// Iterate through all categories and create queries
		for(int item : items)
			if(item > 0){
				Query num = NumericRangeQuery.newIntRange(field, item, item, true, true);
				q.add(num, BooleanClause.Occur.SHOULD);
			}

		return q;
	}
	
	protected BooleanQuery generateQueryS(List<String> items, String field){
		Analyzer a = factory.getAnalyzer();
		BooleanQuery q = new BooleanQuery();

		// Iterate through all categories and create queries
		for(String item : items)
			if(!item.isEmpty()){
				try{
					Query query = new QueryParser(Version.LUCENE_42, field, a).parse(item);
					q.add(query, BooleanClause.Occur.SHOULD);
				}catch(ParseException e){
					// Could not parse item
				}
			}

		return q;
	}
	
	public boolean fileIsModified(int category, Path p){
		try{
			Directory index = factory.getIndex();
			if(index.listAll().length == 0)
				return true;
			
			// Create the two separate queries to match the (1) path and (2) modified date
			long lastModified = Files.getLastModifiedTime(p).toMillis();
			Query pathQuery = new TermQuery(new Term("path", p.toString()));
			Query modifiedQuery = NumericRangeQuery.newLongRange("modified", lastModified, lastModified, true, true);
			
			// Combine the queries in a Boolean Query, which states that both the two previous queries must occur
			BooleanQuery bq = new BooleanQuery();
			bq.add(pathQuery, BooleanClause.Occur.MUST);
			bq.add(modifiedQuery, BooleanClause.Occur.MUST);
			
			if(category > 0){
				Query categoryQuery = NumericRangeQuery.newIntRange("category", category, category, true, true);
				bq.add(categoryQuery, BooleanClause.Occur.MUST);
			}

			// Get the result of the query
			int hitsPerPage = 1;
			try (IndexReader reader = DirectoryReader.open(index)) {
				IndexSearcher searcher = new IndexSearcher(reader);

				TopDocs results = searcher.search(bq, 5 * hitsPerPage);
				
				// If we have no hits, then our file is modified
				return results.totalHits == 0;
			}
		}catch(IOException e){
			return false;
		}
	}
	
	public boolean fileIsModified(Path p){
		return fileIsModified(0, p);
	}
	
	protected void buildIndex() {
		Indexer indexer = new Indexer(factory);
		
		DirectoryStream<Path> paths;
		try {
			paths = Files.newDirectoryStream(searchPath, getSearchFilter());

			for(Path p : paths){
				indexer.addPDF(0, p);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The search filter will by default search for PDF files.
	 * If a filter was supplied to the constructor of the searcher that filter will be used.
	 * @return DirectoryStream.Filter<Path>
	 */
	protected DirectoryStream.Filter<Path> getSearchFilter() {
		if(filter == null){
			filter = new DirectoryStream.Filter<Path>() {
		        @Override
		        public boolean accept(Path entry) throws IOException 
		        {
		        	// If this is a directory, we have a valid type
					if(Files.isDirectory(entry))
						return true;

					// Check whether we have a PDF file
					byte[] pdfMagicNumbers = {0x25, 0x50, 0x44, 0x46};
					
					try(InputStream ins = Files.newInputStream(entry)){
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
		}
		return filter;
	}
}
