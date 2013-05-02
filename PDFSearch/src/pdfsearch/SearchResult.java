package pdfsearch;

import java.io.File;

/**
 *
 * @author Lars
 */
public class SearchResult {
	private File _pdf;
	private String _abstract;
	private String _language;
	private int _category;
	
	public SearchResult(int category, String language, String path){
		_category = category;
		_pdf = new File(path);
		_language = language;
	}
	
	public SearchResult(int category, String language, File pdf){
		_pdf = pdf;
		_category = category;
		_language = language;
	}
	
	public File getPdf(){ return _pdf; }
	public int getCategory(){ return _category; }
	public String getAbstract(){ return _abstract; }
	public String getLanguage(){ return _language; }
}
