package edu.colorado.clear.wsd.feature;

import com.clearnlp.dependency.DEPNode;
import com.clearnlp.morphology.MPLibEn;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DDNExtractor
{

	public static int MAX_DDN = 50; // Maximum number of DDN features
	public static int MAX_SEARCH = 1000; // Maximum number of hits when searching Lucene index
	private IndexSearcher iSearcher;
	private Analyzer analyzer;

	public static final String ddn_key = "DDN";

	public DDNExtractor(File ddnIndexDir)
	{
		IndexReader iReader;
		try {
			Directory dir = FSDirectory.open(ddnIndexDir);
			iReader = IndexReader.open(dir);
			iSearcher = new IndexSearcher(iReader);
			analyzer = new StandardAnalyzer(Version.LUCENE_36);
		} catch (IOException e) {System.err.println("Unable to locate Lucene index");}
	}

	public void addDDNFeatures(DEPNode node)
	{
		Set<String> ddns = this.getDDNs(node);
		String ddnStr = "";
		for (String s: ddns)
		{
			ddnStr += s + Feature.valSeparator;
		}
		ddnStr = ddnStr.trim();
		node.addFeat(ddn_key, ddnStr);
	}

	public Set<String> getDDNs(DEPNode node)
	{
		Set<String> ddnFeature = new HashSet<String>();
		if (node == null)
			return ddnFeature;


		if (!MPLibEn.isNoun(node.pos)) // only consider noun objects
			return ddnFeature;

		if (!Pattern.matches("^[a-z]+$", node.form))
			return ddnFeature;

		String lemma = node.lemma; // lemmatize object headword
		ArrayList<VerbFrequency> verbFreqs = searchGW(lemma, "object");
		// sort verbFreqs based on frequency
		Collections.sort(verbFreqs);
		Collections.reverse(verbFreqs);

		if (verbFreqs.size() == 0)
			return ddnFeature;

		int n = MAX_DDN;

		if (verbFreqs.size() < MAX_DDN)
			n = verbFreqs.size();

		for (int i = 0; i < n; ++i)
		{
			ddnFeature.add(verbFreqs.get(i).verb);
		}

		return ddnFeature;
	}

	private ArrayList<VerbFrequency> searchGW(String word, String field)
	{
		ArrayList<VerbFrequency> verbFreqs = new ArrayList<VerbFrequency>();
		QueryParser qParser = new QueryParser(Version.LUCENE_36, field, analyzer);
		Query query;
		try
		{
			query = qParser.parse(word);
			TopDocs t = iSearcher.search(query, MAX_SEARCH);
			ScoreDoc[] doc = t.scoreDocs;

			for (int i = 0; i < MAX_SEARCH && i < doc.length; ++i)
			{
				int docID = doc[i].doc;
				Document d = iSearcher.doc(docID);
				String verb = d.get("verb");
				String frequency = d.get("frequency");
				verbFreqs.add(new VerbFrequency(Integer.parseInt(frequency), verb));
			}
			return verbFreqs;
		} catch (ParseException e)
		{
			System.err.println("Error searching Lucene index");
			e.printStackTrace();
		} catch (IOException e)
		{
			System.err.println("Error searching Lucene index");
			e.printStackTrace();
		}
		return null;
	}

	private class VerbFrequency implements Comparable<VerbFrequency>
	{
		private int freq;
		private String verb;
		public VerbFrequency(int i, String s)
		{
			freq = i;
			verb = s;
		}
		public int compareTo(VerbFrequency o)
		{
			return this.freq - o.freq;
		}
	}

}
