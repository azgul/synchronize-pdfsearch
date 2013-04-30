/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
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
	
	public void search(String searchTerm) throws IOException, ParseException{
		Directory index = factory.getIndex();
		Analyzer analyzer = factory.getAnalyzer();
		
		Query q = new QueryParser(Version.LUCENE_42, "title", analyzer).parse(searchTerm);

		
		int hitsPerPage = 10;
		try (IndexReader reader = DirectoryReader.open(index)) {
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// 4. display results
			System.out.println("Found " + hits.length + " hits.");
			for(int i=0;i<hits.length;++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("isbn") + "\t" + d.get("title"));
			}
		}
	}
}
