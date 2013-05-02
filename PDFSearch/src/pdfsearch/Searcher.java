/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.File;
import java.io.IOException;
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
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Lars
 */
public class Searcher {
	private IndexFactory factory;
	
	public Searcher(IndexFactory factory){
		this.factory = factory;
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
		try (IndexReader reader = DirectoryReader.open(index)) {
			IndexSearcher searcher = new IndexSearcher(reader);
			
			TopDocs results = searcher.search(mainQuery, Integer.MAX_VALUE);
			ScoreDoc[] hits = results.scoreDocs;

			// 4. display results
			for(int i=0;i<hits.length;++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				searchResult.add(new SearchResult(Integer.parseInt(d.get("category")), d.get("language"), d.get("path")));
			}
		}
		
		return searchResult;
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
	
	public boolean fileIsModified(int category, File f){
		try{
			Directory index = factory.getIndex();
			if(index.listAll().length == 0)
				return true;
			
			Analyzer analyzer = factory.getAnalyzer();
			
			// Create the two separate queries to match the (1) path and (2) modified date
			Query pathQuery = new TermQuery(new Term("path", f.getPath()));
			Query modifiedQuery = NumericRangeQuery.newLongRange("modified", f.lastModified(), f.lastModified(), true, true);
			
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
	
	public boolean fileIsModified(File f){
		return fileIsModified(0, f);
	}
}
