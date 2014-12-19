package lucene;

import org.apache.lucene.util.Version;
import org.junit.Test;

public class LuceneIndexWrapperTest {
	
	@Test
	public void textQuery() throws Throwable {

		LuceneIndexWrapper wrapper = LuceneIndexWrapper.init("D://LuceneBIG", Version.LUCENE_4_9, false, false);
	
//		wrapper.indexDocWithTextField("text", "big");
//		wrapper.indexDocWithTextField("text", "wangxiangtao hello wangxiangtao");
//		wrapper.indexDocWithTextField("text", "wangxiangtao");

		wrapper.commit(true);

		System.out.println("total index count: "+wrapper.findIndexDbCount());
//		System.out.println("search wrong test: "+wrapper.searchCommenQuery("wangxiangtao2").size());
//		System.out.println("search wrong test: "+wrapper.searchCommenQuery("wangxiangtao hello").size());
//		System.out.println("search true test: "+wrapper.searchCommenQuery("wangxiangtao").size());
//		System.out.println("search true test: "+wrapper.searchCommenQuery("hello").size());
		System.out.println("search true test: "+wrapper.getDocumentFrequency("big"));
//
//		wrapper.clear();
//		wrapper.commit(true);
//		System.out.println("delete all--------------");
//		System.out.println("total index count: "+wrapper.findIndexDbCount());
//		System.out.println("search wrong test: "+wrapper.searchTermQuery("text", "wangxiangtao2").size());
//		System.out.println("search wrong test: "+wrapper.searchTermQuery("text", "wangxiangtao hello").size());
//		System.out.println("search true test: "+wrapper.searchTermQuery("text", "wangxiangtao").size());

	}

}
