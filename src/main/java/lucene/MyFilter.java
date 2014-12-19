package lucene;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

class MyFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
//  private final PositionIncrementAttribute posAtt = addAttribute(PositionIncrementAttribute.class);
    
    public MyFilter(TokenStream input) {
        super(input);
    }
    
    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        String terms[] = termAtt.toString().split(" ");
        CharArraySet stopword = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
        for(String term : terms){
    	 if(!term.matches("[a-zA-Z ]+") || term.length() > 15 || stopword.contains(term) )
        	 {
        		 termAtt.setEmpty();
        	 }
        }
        return true;
    }
}