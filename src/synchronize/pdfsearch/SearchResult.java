package synchronize.pdfsearch;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Lars
 */
public class SearchResult {
	private File _pdf;
	private String _title;
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
	
	public SearchResult(String pdf, int category, String language, String title, String descr) {
		this(category, language, pdf);
		_title = title;
		_abstract = descr;
	}
	
	public File getPdf(){ return _pdf; }
	public int getCategory(){ return _category; }
	public String getAbstract(){ return _abstract; }
	public String getLanguage(){ return _language; }
	public String getModifiedDate(){ return new SimpleDateFormat("YYYY-MM-dd").format(new Date(_pdf.lastModified())); }
	public long getModifiedTimestamp(){ return _pdf.lastModified(); }
	public String getTitle(){ return _title; }
	
	@Override
	public String toString(){
		return String.format("%s (%s - %s) %s", getTitle(), getModifiedDate(), _pdf.lastModified(), _pdf.getAbsolutePath());
	}
}
