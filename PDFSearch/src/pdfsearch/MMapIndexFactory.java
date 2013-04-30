/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Lars
 */
public class MMapIndexFactory implements IndexFactory {
	public Directory getIndex() throws IOException{
		return new MMapDirectory(new File(Constants.INDEX_DIRECTORY));
	}
	
	public Analyzer getAnalyzer(){
		return new StandardAnalyzer(Version.LUCENE_42);
	}
	
	public IndexWriter getIndexWriter() throws IOException{
		Analyzer analyzer = getAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);
		Directory index = getIndex();
		return new IndexWriter(index, config);
	}
}
