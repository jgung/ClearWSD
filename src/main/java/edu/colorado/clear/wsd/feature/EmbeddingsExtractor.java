package edu.colorado.clear.wsd.feature;

import com.clearnlp.dependency.DEPNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbeddingsExtractor
{

	public static int[] K_VALS = {100,320,1000,3200,10000};
	public static final String FILE_NAME = "cluster";
	public static final String cluster_Key = "CLUSTER";
	private Map<String, Map<String, Integer>> clusterings = new HashMap<String, Map<String,Integer>>();

	public EmbeddingsExtractor(File clusterFile) {
		try
		{
			for (int i = 0; i < K_VALS.length; ++i)
			{
				Map<String, Integer> clusterMap = new HashMap<String, Integer>();
				File f = new File(clusterFile, FILE_NAME + "-" + K_VALS[i]);
				if (!f.exists())
				{
					System.err.println("CLUSTERING FILE " + f.getName() + " NOT FOUND");
					continue;
				}
				String[] lines = FileUtils.readFileToString(f).split("\n");
				for (int l = 0; l < lines.length; ++l) {
					String line = lines[l];
					String word = line.split("\t")[0];
					int cl = Integer.parseInt(line.split("\t")[1]);
					clusterMap.put(word, cl);
				}
				clusterings.put("" + K_VALS[i], clusterMap);
			}
		} catch (IOException e){System.err.println("Problem reading cluster files");}
	}

	public void addEmbeddingsFeatures(DEPNode node)
	{
		String clusterStr = "";
		for (String s: getEmbeddingsFeatures(node))
		{
			clusterStr += s + Feature.valSeparator;
		}
		clusterStr = clusterStr.trim();
		node.addFeat(cluster_Key, clusterStr);
	}

	public List<String> getEmbeddingsFeatures(DEPNode node) {

		List<String> features = new ArrayList<String>();
		if (node == null)
		{
			return features;
		}
		String lemma = node.lemma.toUpperCase();
		String token = node.form.toUpperCase();
		for (String k: clusterings.keySet())
		{
			Map<String, Integer> clustering = clusterings.get(k);
			int cluster = -1; // oov
			if (clustering.containsKey(lemma))
				cluster = clustering.get(lemma);
			else if (clustering.containsKey(token))
				cluster = clustering.get(token);
			features.add(cluster_Key + Feature.separator + "K="+k+"-"+cluster);
		}
		return features;
	}

}
