package edu.colorado.clear.wsd.feature;

import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TokenFeatures
{

	public static final String form_key = "FORM";
	public static final String lemma_key = "LEMMA";
	public static final String pos_key = "POS";
	public static final String dep_key = "DEP";


	public static void addFeatures(DEPTree tree)
	{
		for (int i = 1; i < tree.size(); ++i)
			addFeatures(tree.get(i));
	}

	public static void addFeatures(DEPNode node)
	{
		node.addFeat(form_key, node.form);
		node.addFeat(lemma_key, node.lemma);
		node.addFeat(pos_key, node.pos);
		node.addFeat(dep_key, node.getLabel());
	}

//	public static void addWNFeatures(DEPTree tree, WordNetExtractor wn)
//	{
//		for (int i = 1; i < tree.size(); ++i)
//		{
//			addWNFeatures(tree.get(i), wn);
//		}
//	}
//	public static void addWNFeatures(DEPNode node, WordNetExtractor wn)
//	{
//		wn.addWordNetFeatures(node);
//	}

	public static List<Feature> extractTokenFeatures(DEPNode node, Set<String> feats)
	{
		List<Feature> features = new ArrayList<Feature>();
		for (String s: feats)
		{
			String featStr = node.getFeat(s);
			if (featStr != null)
			{
				features.add(new Feature(s, featStr));
			}
		}
		return features;
	}


	public static List<Feature> extractContextFeatures(DEPTree sentence, DEPNode node, int[] offsets, Set<String> feats, boolean addIndex)
	{
		List<Feature> features = new ArrayList<Feature>();
		int nodeIndex = sentence.indexOf(node);

		for (Integer i: offsets)
		{
			int index = nodeIndex + i;
			if (index >= 1 && index <= sentence.size())
			{
				DEPNode contextNode = sentence.get(index);
				if (contextNode == null)
					continue;
				List<Feature> nodeFeatures = extractTokenFeatures(contextNode, feats);
				for (Feature f: nodeFeatures)
				{
					if (addIndex)
						f.setName(f.getName() + Feature.separator + i);
					else
						f.setName(f.getName() + Feature.separator + "context");
				}

				features.addAll(nodeFeatures);
			}
		}
		return features;
	}

	public static List<Feature> extractNGramFeatures(DEPTree sentence, int n, Set<String> feats)
	{
		List<List<DEPNode>> ngrams = extractNGrams(sentence, n);
		List<Feature> features = new ArrayList<Feature>();
		for (List<DEPNode> ngram: ngrams)
		{
			for (String s: feats)
			{
				StringBuilder featStr = new StringBuilder();
				for (DEPNode node: ngram)
				{
					if (node == null) /* element of ngram is out of bounds (not in the DEPTree) */
					{
						featStr.append("OOB ");
					}
					else
					{
						String featValue = node.getFeat(s);
						if (featValue != null)
						{
							featStr.append(featValue).append(" ");
						}
					}
				}
				String featString = featStr.toString().trim().replace(" ", Feature.separator);
				features.add(new Feature(s + Feature.separator + ngram.size() + Feature.ngramStr, featString));
			}
		}
		return features;
	}

	public static List<List<DEPNode>> extractNGrams(DEPTree sentence, int n)
	{
		List<List<DEPNode>> ngrams = new ArrayList<List<DEPNode>>();
		for (int i = 1 - (n-1); i < sentence.size(); ++i)
		{
			List<DEPNode> ngram = new ArrayList<DEPNode>();
			for (int offset = 0; offset < n; ++offset) /* create a list of n sequential DEPNodes */
			{
				int index = i + offset;
				if (index < 1 || index > sentence.size()) // out of sentence tokens
				{
					ngram.add(null);
				}
				else
				{
					DEPNode current = sentence.get(i + offset);
					ngram.add(current);
				}
			}
			ngrams.add(ngram);
		}
		return ngrams;
	}

}
