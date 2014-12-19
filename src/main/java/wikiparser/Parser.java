package wikiparser;

import storage.JDBCConnectionWrapper;
import storage.JDBCConnectionWrapper.UpdateStatementWrapper;
import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class Parser {

	public static void main(String[] args) throws Throwable {
	
	
		WikiXMLParser wxsp = WikiXMLParserFactory
				.getSAXParser("d://wiki//enwiki-latest-pages-articles.xml");
		try {
		
			wxsp.setPageCallback(new PageCallbackHandler() {
				int i = 0;
				JDBCConnectionWrapper jdbcConnection = new JDBCConnectionWrapper("com.mysql.jdbc.Driver",
		                 "jdbc:mysql://ec2-175-41-175-218.ap-southeast-1.compute.amazonaws.com:3306/business_graph?useUnicode=true&characterEncoding=UTF-8"
			   				, "release4", "release4");
				UpdateStatementWrapper insert = jdbcConnection.update("insert into crawl_wiki(title,category,wiki_id,links,url) values"
						+ "(?,?,?,?,?)");
				public void process(WikiPage page) {
					System.out.println(i++);
					if(i> 13961685) {
						try {
							insert.setParameter(1, page.getTitle().trim().replace(" ", "_"));
							insert.setParameter(2, new String(page.getCategories().toString().getBytes("utf-8"), "utf-8"));
							insert.setParameter(3, page.getID());
							insert.setParameter(4, new String(page.getLinks().toString().getBytes("utf-8"), "utf-8"));
							insert.setParameter(5, "http://en.wikipedia.org/wiki/"+page.getTitle().trim().replace(" ", "_"));

							insert.commit();
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}
				
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
