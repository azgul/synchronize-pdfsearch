/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfsearch;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

/**
 *
 * @author Lars
 */
public interface IndexFactory {
	public Directory getIndex() throws IOException;
	public Analyzer getAnalyzer();
	public IndexWriter getIndexWriter() throws IOException;
}
