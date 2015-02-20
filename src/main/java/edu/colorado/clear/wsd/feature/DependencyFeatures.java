package edu.colorado.clear.wsd.feature;

import com.clearnlp.dependency.DEPLibEn;
import com.clearnlp.dependency.DEPNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DependencyFeatures
{

	public static final String rootPathStr = "rootPath";
	public static final String depChildStr = "depChild";

	public static List<Feature> extractChildFeatures(DEPNode node, Set<String> feats)
	{
		List<Feature> features = new ArrayList<Feature>();
		List<DEPNode> children = new ArrayList<DEPNode>();
		for (DEPNode child: node.getDependentNodeList())
		{
			children.add(child);
		}
		for (String s: feats)
		{
			for (DEPNode child: children)
			{
				String featVal = child.getFeat(s);
				if (featVal != null)
					features.add(new Feature(s + Feature.separator + depChildStr, featVal));
			}
		}
		return features;
	}

	public static void addWNFeatures(DEPNode node, WordNetExtractor wn)
	{
		for (DEPNode child: node.getDependentNodeList())
		{
			if (DEPLibEn.isSubject(child.getLabel())) // subject
			{
				wn.addWordNetFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_IOBJ))  // indirect object
			{
				wn.addWordNetFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_DOBJ)) // direct object
			{
				wn.addWordNetFeatures(child);
			}
			else if (child.getLabel().equals(DEPLibEn.DEP_POBJ)) // pp object
			{
				wn.addWordNetFeatures(child);
			}
		}
	}

	public static void addDDNFeatures(DEPNode node, DDNExtractor ddn)
	{
		for (DEPNode child: node.getDependentNodeList())
		{
			if (DEPLibEn.isSubject(child.getLabel())) // subject
			{
				ddn.addDDNFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_IOBJ))  // indirect object
			{
				ddn.addDDNFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_DOBJ)) // direct object
			{
				ddn.addDDNFeatures(child);
			}
			else if (child.getLabel().equals(DEPLibEn.DEP_POBJ)) // pp object
			{
				ddn.addDDNFeatures(child);
			}
		}
	}

	public static void addClusterFeatures(DEPNode node, EmbeddingsExtractor clusters)
	{
		for (DEPNode child: node.getDependentNodeList())
		{
			if (DEPLibEn.isSubject(child.getLabel())) // subject
			{
				clusters.addEmbeddingsFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_IOBJ))  // indirect object
			{
				clusters.addEmbeddingsFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_DOBJ)) // direct object
			{
				clusters.addEmbeddingsFeatures(child);
			}
			else if (child.getLabel().equals(DEPLibEn.DEP_POBJ)) // pp object
			{
				clusters.addEmbeddingsFeatures(child);
			}
		}
	}

	public static void addBrownClusterFeatures(DEPNode node, BrownExtractor brownExtractor)
	{
		for (DEPNode child: node.getDependentNodeList())
		{
			if (DEPLibEn.isSubject(child.getLabel())) // subject
			{
				brownExtractor.addBWCFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_IOBJ))  // indirect object
			{
				brownExtractor.addBWCFeatures(child);
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_DOBJ)) // direct object
			{
				brownExtractor.addBWCFeatures(child);
			}
			else if (child.getLabel().equals(DEPLibEn.DEP_POBJ)) // pp object
			{
				brownExtractor.addBWCFeatures(child);
			}
		}
	}

	public static List<Feature> extractArgFeatures(DEPNode node, Set<String> feats)
	{
		List<Feature> features = new ArrayList<Feature>();
		if (node == null)
			return features;
		for (DEPNode child: node.getDependentNodeList())
		{
			if (DEPLibEn.isSubject(child.getLabel())) // subject
			{
				features.addAll(getUnigramFeatureStrings(child, feats, DEPLibEn.DEP_SUBJ));
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_IOBJ))  // indirect object
			{
				features.addAll(getUnigramFeatureStrings(child, feats, DEPLibEn.DEP_IOBJ));
			}
			else if (child.getLabel().contains(DEPLibEn.DEP_DOBJ)) // direct object
			{
				features.addAll(getUnigramFeatureStrings(child, feats, DEPLibEn.DEP_DOBJ));
			}
			else if (child.getLabel().equals(DEPLibEn.DEP_POBJ)) // pp object
			{
				features.addAll(getUnigramFeatureStrings(child, feats, DEPLibEn.DEP_POBJ));
			}
		}
		return features;
	}

	/* get list of features with a variable number of values, i.e. wordnet hypernyms */
	public static List<Feature> getUnigramFeatureStrings(DEPNode node, Set<String> feats, String label)
	{
		List<Feature> features = new ArrayList<Feature>();
		for (String s: feats)
		{
			String featStr = node.getFeat(s);
			if (featStr != null)
			{
				String[] unigramFeats = featStr.split(Feature.valSeparator);
				for (String feat: unigramFeats)
				{
					features.add(new Feature(label + Feature.separator + s, feat));
				}
			}
		}
		return features;
	}

	public static List<Feature> rootPathFeatures(DEPNode node, Set<String> feats)
	{
		List<Feature> features = new ArrayList<Feature>();

		List<DEPNode> rootPath = new ArrayList<DEPNode>();
		if (node == null)
			return features;
		rootPath.add(node);

		String label = node.getLabel();
		while (!label.equals(DEPLibEn.DEP_ROOT) && node.getHead() != null)
		{
			node = node.getHead();
			if (node != null)
				rootPath.add(node);
		}
		for (String s: feats)
		{
			StringBuilder featStr = new StringBuilder();
			for (DEPNode n: rootPath)
			{
				String featVal = n.getFeat(s);
				if (featVal != null)
					featStr.append(featVal).append(" ");
			}
			String featString = featStr.toString().trim().replace(" ", Feature.separator);
			features.add(new Feature(s + Feature.separator + rootPathStr, featString));
		}
		return features;
	}
}
