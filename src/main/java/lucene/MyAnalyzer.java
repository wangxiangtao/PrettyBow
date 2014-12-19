package lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

public class MyAnalyzer extends Analyzer {

	private int minGram;
	private int maxGram;
	private Version version;
	private boolean stem;

	public MyAnalyzer(int minGram, int maxGram, boolean stem, Version version) {
		this.minGram = minGram;
		this.maxGram = maxGram;
		this.version = version;
		this.stem = stem;
	}

	@Override
	protected TokenStreamComponents createComponents(String arg0, Reader reader) {

		Tokenizer source = new StandardTokenizer(version, reader);

		ShingleFilter shinglefilter = null;

		TokenStream filter = null;
		if (maxGram > 1) {
			if (minGram == 1) {
				shinglefilter = new ShingleFilter(source, minGram + 1, maxGram);
				shinglefilter.setOutputUnigrams(true);
			} else {
				shinglefilter = new ShingleFilter(filter, minGram, maxGram);
				shinglefilter.setOutputUnigrams(false);
			}
			filter = shinglefilter;
		}

		filter = new LowerCaseFilter(version, filter);
		filter = new StopFilter(version, filter,
				StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		if (stem)
			filter = new PorterStemFilter(filter);
		filter = new MyFilter(filter);
		return new TokenStreamComponents(source, filter);
	}

}
