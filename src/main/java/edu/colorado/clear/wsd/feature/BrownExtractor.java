package edu.colorado.clear.wsd.feature;

import com.clearnlp.dependency.DEPNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BrownExtractor
{

	public static final String brown_key = "BWC";
	private HashMap<String, String> mapping;
	public static List<Integer> prefixes = Arrays.asList(4, 6, 10, 20);

	public BrownExtractor(File brownClustersFile)
	{
		mapping = new HashMap<String, String>();
		try
		{
			for (String line: FileUtils.readFileToString(brownClustersFile).split("\n"))
			{
				String[] fields = line.split("\t");
				mapping.put(fields[1], fields[0]);
			}
		} catch (IOException e) {System.err.println("Error reading BWC file: "); e.printStackTrace();}
	}

	public void addBWCFeatures(DEPNode node)
	{
		Set<String> bwcFeatures = this.getBWCFeatures(node);
		String bwcStr = "";
		for (String s: bwcFeatures)
		{
			bwcStr += s + Feature.valSeparator;
		}
		bwcStr = bwcStr.trim();
		node.addFeat(brown_key, bwcStr);
	}

	public Set<String> getBWCFeatures(DEPNode node)
	{
		Set<String> bwcFeatures = new HashSet<String>();
		if (node == null)
			return bwcFeatures;

		String lemma = node.lemma;
		String token = node.form.toLowerCase();
		if (mapping.containsKey(lemma))
		{
			String cluster = mapping.get(lemma);
			for (int prefix: prefixes)
			{
				if (prefix > cluster.length())
					break;
				else
					bwcFeatures.add((prefix + "-" + cluster.substring(0,prefix)));
			}
			if (bwcFeatures.size() == 0)
				bwcFeatures.add(4 + "-" + cluster);
		} else if (mapping.containsKey(token))
		{
			String cluster = mapping.get(token);
			for (int prefix: prefixes) {
				if (prefix > cluster.length())
					break;
				else
					bwcFeatures.add(prefix + "-" + cluster.substring(0,prefix));
			}
			if (bwcFeatures.size() == 0)
				bwcFeatures.add(4 + "-" + cluster);
		}

		return bwcFeatures;
	}

}
