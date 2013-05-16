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
	@Override
	public Directory getIndex() throws IOException{
		File index = new File(Constants.INDEX_DIRECTORY);
		
		if(!index.isDirectory()){
			// Create index directory
			if(!index.mkdirs())
				return null;
		}
		
		return MMapDirectory.open(index);
	}
	
	@Override
	public Analyzer getAnalyzer(){
		return new StandardAnalyzer(Version.LUCENE_42);
	}
	
	@Override
	public IndexWriter getIndexWriter(boolean create) throws IOException{
		Analyzer analyzer = getAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);

		if(create)
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		else
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

		Directory index = getIndex();
		return new IndexWriter(index, config);
	}
	
	@Override
	public IndexWriter getIndexWriter() throws IOException{
		return getIndexWriter(false);
	}
}
