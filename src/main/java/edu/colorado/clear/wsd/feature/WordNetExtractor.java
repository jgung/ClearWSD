package edu.colorado.clear.wsd.feature;

import com.clearnlp.dependency.DEPNode;
import edu.colorado.clear.wsd.util.WordNetInterface;

import java.io.File;
import java.util.Set;

public class WordNetExtractor
{

	public static final String wn_key = "WN";
	protected WordNetInterface wordNet;
	public WordNetExtractor(File dict)
	{
		this.wordNet = new WordNetInterface(dict);
	}

	public void addWordNetFeatures(DEPNode node)
	{
		Set<String> expandedSet = wordNet.getHypernyms(node.lemma, WordNetInterface.getPos(node.pos));
		String wnString = "";
		for (String s: expandedSet)
		{
			wnString += s + Feature.valSeparator;
		}
		wnString = wnString.trim();
		node.addFeat(wn_key, wnString);
	}

}
