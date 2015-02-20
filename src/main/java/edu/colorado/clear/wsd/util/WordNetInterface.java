package edu.colorado.clear.wsd.util;

import com.clearnlp.morphology.MPLibEn;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WordNetInterface
{

	private IDictionary dict;

	public WordNetInterface(File wordNetFile)
	{
		try
		{
			this.dict = new Dictionary(wordNetFile);
			dict.open();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public Set<String> getHypernyms(String string, POS pos)
	{
		Set<String> words = new HashSet<String>();
		if (pos == null || ! pos.equals(POS.NOUN))
			return words;
		IIndexWord idxWord = dict.getIndexWord(string, pos);
		if (idxWord == null)
			return words;
		Set<ISynsetID> synsets = new HashSet<ISynsetID>();
		for (IWordID id: idxWord.getWordIDs())
		{
			ISynset synset = dict.getWord(id).getSynset();
			synsets.add(synset.getID());
			synsets.addAll(synset.getRelatedSynsets(Pointer.HYPERNYM));
		}
		for (ISynsetID id: synsets)
		{
			ISynset s = dict.getSynset(id);
			for (IWord w: s.getWords())
				words.add(w.getLemma());
		}
		return words;
	}



	public List<ISynsetID> getHypernyms(ISynset synset, List<ISynsetID> path)
	{		
		for (ISynsetID id: synset.getRelatedSynsets(Pointer.HYPERNYM))
		{
			if (!path.contains(id))
			{
				path.add(id);
				getHypernyms(dict.getSynset(id), path);
			}
		}
		for (ISynsetID id: synset.getRelatedSynsets(Pointer.HYPERNYM_INSTANCE))
		{
			if (!path.contains(id))
			{
				path.add(id);
				getHypernyms(dict.getSynset(id), path);
			}
		}
		return path;
	}

	public static POS getPos(String posTag)
	{	
		if (MPLibEn.isNoun(posTag))
			return POS.NOUN;
		else if (MPLibEn.isVerb(posTag))
			return POS.VERB;
		else if (MPLibEn.isAdjective(posTag))
			return POS.ADJECTIVE;
		else if (MPLibEn.isAdverb(posTag))
			return POS.ADVERB;
		else
			return null;
	}

}
