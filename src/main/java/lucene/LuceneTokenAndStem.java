package lucene;
 
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
 
 
 public class LuceneTokenAndStem {
     
     
     public static void main(String sa[]) throws Exception {
         
    	 removeStopWordsAndStem("hello xiangtao dogs");
     }
	public static List<String> removeStopWordsAndStem(String input) throws IOException {
		Version matchVersion = Version.LUCENE_4_9;
		// BufferedReader stopwordsReader = new BufferedReader( new// FileReader("a.txt"));
		// Analyzer analyzer = new StandardAnalyzer(matchVersion,// stopwordsReader);
		Analyzer analyzer = new StandardAnalyzer(matchVersion,StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		List<String> result = new ArrayList<String>();
		TokenStream ts = analyzer.tokenStream(null, new StringReader(input.replaceAll("'", "")));
		ts = new PorterStemFilter(ts);
		CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
		ts.reset();
		while (ts.incrementToken()) {
			String term = charTermAttribute.toString();
			result.add(term);
		}
		ts.end();
		ts.close();
		analyzer.close();
		return result;
	}
	public static List<String> removeStem(String input) throws IOException {
		Version matchVersion = Version.LUCENE_4_9;
		// BufferedReader stopwordsReader = new BufferedReader( new// FileReader("a.txt"));
		// Analyzer analyzer = new StandardAnalyzer(matchVersion,// stopwordsReader);
		Analyzer analyzer = new StandardAnalyzer(matchVersion);
		List<String> result = new ArrayList<String>();
		TokenStream ts = analyzer.tokenStream(null, new StringReader(input.replaceAll("'", "")));
		ts = new PorterStemFilter(ts);
		CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
		ts.reset();
		while (ts.incrementToken()) {
			String term = charTermAttribute.toString();
			result.add(term);
		}
		ts.end();
		ts.close();
		analyzer.close();
		return result;
	}
 }