package lucene;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class LuceneIndexWrapper {

	protected final static String LUCENE_OBJECT_LOCK = "LUCENE_OBJECT_LOCK";
	protected final static int BATCH_SIZE = 10000;
	protected final static int INDEX_COMMIT_SIZE = 500;

	private Version version = null;
	private Directory directory = null;
	private IndexWriter indexWriter = null;
	private IndexReader indexReader = null;
	private QueryParser queryParser = null;

	private static String keyFieldName = "id";
	private static String defaultFieldName = "text";

	private static LuceneIndexWrapper wrapper;

	LuceneIndexWrapper() {}

	public static synchronized LuceneIndexWrapper init(
			String luceneIndexDirectory, Version version, boolean inMemory,
			boolean clearIndex) throws Throwable {

		if (wrapper == null) {
			wrapper = new LuceneIndexWrapper();
			wrapper.version = version;
			if (inMemory)
				wrapper.directory = new RAMDirectory();
			else {
				File luceneIndexDir = new File(luceneIndexDirectory);
				if (!luceneIndexDir.exists())
					luceneIndexDir.mkdirs();
				wrapper.directory = FSDirectory.open(luceneIndexDir,
						new SingleInstanceLockFactory());
			}
			return wrapper;
		}
		return wrapper;
	}

	public LuceneIndexWrapper initConfig(int minGram, int maxGram, boolean stem)
			throws Throwable {
		Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();

		analyzerMap.put(defaultFieldName, new MyAnalyzer(minGram, maxGram,
				stem, wrapper.version));

		Analyzer analyzerWrapper = new PerFieldAnalyzerWrapper(
				new StandardAnalyzer(wrapper.version), analyzerMap);

		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
				wrapper.version, analyzerWrapper);

		wrapper.indexWriter = new IndexWriter(wrapper.directory,
				indexWriterConfig);
		wrapper.indexReader = DirectoryReader.open(wrapper.directory);
		wrapper.queryParser = new QueryParser(wrapper.version,
				defaultFieldName, analyzerWrapper);
		return wrapper;
	}

	public void index(Document doc) throws Throwable {
		this.indexWriter.addDocument(doc);
	}

	public void indexDocWithStringField(String value) throws Throwable {
		Document doc = new Document();
		doc.add(new StringField(defaultFieldName, value, Field.Store.YES));
		this.indexWriter.addDocument(doc);
	}

	public void indexDocWithDefinedField(String id, String text)
			throws Throwable {
		Document doc = new Document();
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setStored(true);
		type.setStoreTermVectors(true);
		type.setTokenized(true);
		Field field1 = new StringField(keyFieldName, id, Field.Store.YES);
		Field field2 = new Field(defaultFieldName, text, type);
		doc.add(field1);
		doc.add(field2);
		this.indexWriter.addDocument(doc);
	}

	public List<Document> searchCommenQuery(String searchQuery)
			throws Throwable {
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		Query query = this.queryParser.parse(searchQuery);

		TopScoreDocCollector collector = TopScoreDocCollector.create(500, true);

		List<Document> results = new ArrayList<Document>();
		ScoreDoc[] scoreDoc = null;

		IndexSearcher indexSearcher = new IndexSearcher(this.indexReader);

		indexSearcher.search(query, collector);

		scoreDoc = collector.topDocs().scoreDocs;

		for (int i = 0; i < scoreDoc.length; i++) {
			results.add(indexSearcher.doc(scoreDoc[i].doc));
		}

		return results;
	}

	public int findIndexDbCount() {
		return this.indexReader.numDocs();
	}

	public List<Document> searchTermQuery(String searchText) throws Throwable {
		Query q = new TermQuery(new Term(defaultFieldName, searchText));
		IndexSearcher indexSearcher = new IndexSearcher(this.indexReader);
		TopDocs td = indexSearcher.search(q, 1000);
		List<Document> results = new ArrayList<Document>();
		for (ScoreDoc scoreDoc : td.scoreDocs) {
			Document d = indexSearcher.doc(scoreDoc.doc);
			results.add(d);
		}
		return results;
	}

	public List<Document> searchTermFilter(String searchText) throws Throwable { // faster,
																					// without
																					// score
		Query q = new TermQuery(new Term(defaultFieldName, searchText));
		IndexSearcher indexSearcher = new IndexSearcher(this.indexReader);
		QueryWrapperFilter queryFilter = new QueryWrapperFilter(q);
		TopDocs td = indexSearcher.search(new MatchAllDocsQuery(), queryFilter,
				1000);
		List<Document> results = new ArrayList<Document>();
		for (ScoreDoc scoreDoc : td.scoreDocs) {
			Document doc = indexSearcher.doc(scoreDoc.doc);
			results.add(doc);
		}
		return results;
	}

	public int getDocumentFrequencyByTerm(String value) throws Throwable {
		Term term = new Term(defaultFieldName, value);
		int df = this.indexReader.docFreq(term);
		return df;
	}

	public int getDocumentFrequency(String searchQuery) throws Throwable {
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		Query query = this.queryParser.parse(searchQuery);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		IndexSearcher indexSearcher = new IndexSearcher(this.indexReader);
		indexSearcher.search(query, collector);
		return collector.topDocs().totalHits;
	}

	public Map<String, Integer> getTermVectorById(String id) throws Throwable {
		Query query = this.queryParser.parse(keyFieldName + ":" + id);
		QueryWrapperFilter queryFilter = new QueryWrapperFilter(query);
		IndexSearcher indexSearcher = new IndexSearcher(this.indexReader);
		TopDocs td = indexSearcher.search(new MatchAllDocsQuery(), queryFilter,
				1);
		int docId = -1;
		for (ScoreDoc scoreDoc : td.scoreDocs) {
			docId = scoreDoc.doc;
		}
		Map<String, Integer> frequencies = new HashMap<>();
		if (docId != -1) {
			try {
				Terms vector = this.indexReader.getTermVector(docId,
						defaultFieldName);
				TermsEnum termsEnum = null;

				termsEnum = vector.iterator(termsEnum);
				BytesRef text = null;
				while ((text = termsEnum.next()) != null) {
					String term = text.utf8ToString();
					int freq = (int) termsEnum.totalTermFreq();
					frequencies.put(term, freq);
				}
			} catch (Exception e) {
				System.out.println("no vector for id and docId:" + id + ","
						+ docId);
			}
		}
		return frequencies;
	}

	public List<String> getTerms() throws Throwable {
		Fields fields = MultiFields.getFields(this.indexReader);
		Terms terms = fields.terms(defaultFieldName);
		TermsEnum iterator = terms.iterator(null);
		BytesRef byteRef = null;
		List<String> termlist = new ArrayList<String>();
		while ((byteRef = iterator.next()) != null) {
			String term = new String(byteRef.bytes, byteRef.offset,
					byteRef.length);
			termlist.add(term);
		}
		return termlist;
	}

	public void commit(boolean reopenReader) throws Throwable {
		this.indexWriter.commit();
		if (reopenReader) {
			synchronized (this) {
				IndexReader newReader = DirectoryReader.openIfChanged(
						(DirectoryReader) this.indexReader, this.indexWriter,
						false);// reader.reopen();
				if (newReader != null) {
					this.indexReader.close();
					this.indexReader = newReader;
				}
			}
		}
	}

	public void clear() throws Throwable {
		this.indexWriter.deleteAll();
	}

	public List<Document> readAll() throws Throwable {
		int num = this.indexReader.numDocs();
		List<Document> documents = new ArrayList<Document>();
		for (int i = 0; i < num; i++) {
			Document d = this.indexReader.document(i);
			documents.add(d);
		}
		return documents;
	}

	public void delete(String id) throws Throwable {
		Query query = this.queryParser.parse(keyFieldName + ":" + id);
		this.indexWriter.deleteDocuments(query);
		commit(true);
	}

	public void closeAll() throws Throwable {
		this.indexReader.close();
		this.indexWriter.close();
		this.directory.close();
	}
}