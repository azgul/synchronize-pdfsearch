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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
		return search(searchTerm, new HashSet<Integer>(), new HashSet<String>());
	}
	
	public List<SearchResult> search(String searchTerm, Set<Integer> categories) throws IOException, ParseException{
		return search(searchTerm, categories, new HashSet<String>());
	}
	
	/**
	 * Check whether the index already exists
	 * @return boolean - true if index is found, false otherwise
	 */
	public boolean indexExists() {
		try {
			return DirectoryReader.indexExists(factory.getIndex());
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public List<SearchResult> search(String searchTerm, Set<Integer> categories, Set<String> languages) throws IOException, ParseException{
		Directory index = factory.getIndex();
		Analyzer analyzer = factory.getAnalyzer();
		
		String[] searchFields = {"title", "contents", "keywords", "filename"};
		
		Query mainQuery;		
		if(categories.isEmpty() && languages.isEmpty() && (searchTerm == null || searchTerm.isEmpty()))
			return new ArrayList<SearchResult>();
		else if(!categories.isEmpty() || !languages.isEmpty()) {
			// Our main query should be a boolean query
			mainQuery = new BooleanQuery();
			
			// Add the field and category queries
			BooleanQuery bq = (BooleanQuery)mainQuery;
			
			// Create our inner boolean query, which should match any of the categories or languages
			if(!categories.isEmpty()){
				BooleanQuery categoryQuery = generateQueryI(categories, "category");
				bq.add(categoryQuery, BooleanClause.Occur.MUST);	
			}
			if(!languages.isEmpty()){
				BooleanQuery categoryQuery = generateQueryS(languages, "language");
				bq.add(categoryQuery, BooleanClause.Occur.MUST);	
			}
			
			// Add the field query
			if(searchTerm != null && !searchTerm.isEmpty()) {
				Query fieldQuery = new MultiFieldQueryParser(Version.LUCENE_42, searchFields, analyzer).parse(searchTerm);
				bq.add(fieldQuery, BooleanClause.Occur.MUST);
			}
		} else {
			mainQuery = new MultiFieldQueryParser(Version.LUCENE_42, searchFields, analyzer).parse(searchTerm); 
		}
		
		ArrayList<SearchResult> searchResult = new ArrayList<>();
		if(!DirectoryReader.indexExists(index)) {
			// if no index exists we build the index
			buildIndex();
		}
		try (IndexReader reader = DirectoryReader.open(index)) {
			IndexSearcher searcher = new IndexSearcher(reader);
			System.out.println(mainQuery);
			TopDocs results = searcher.search(mainQuery, Integer.MAX_VALUE);
			ScoreDoc[] hits = results.scoreDocs;
			System.out.println("Hits: " + hits.length);

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
	
	protected BooleanQuery generateQueryI(Set<Integer> items, String field){
		BooleanQuery q = new BooleanQuery();

		// Iterate through all categories and create queries
		for(int item : items)
			if(item > 0){
				Query num = NumericRangeQuery.newIntRange(field, item, item, true, true);
				q.add(num, BooleanClause.Occur.SHOULD);
			}

		return q;
	}
	
	protected BooleanQuery generateQueryS(Set<String> items, String field){
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
	
	public int buildIndex() {
		Indexer indexer = new Indexer(factory);
		// TODO: fetch real category instead of random
		Random rand = new Random();
		
		DirectoryStream<Path> paths;
		int added = 0;
		try {
			paths = Files.newDirectoryStream(searchPath, getSearchFilter());

			for(Path p : paths){
				if(indexer.addPDF(rand.nextInt(4)+1, p))
					added++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return added;
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
