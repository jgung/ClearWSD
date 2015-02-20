package edu.colorado.clear.wsd.util;

import com.clearnlp.component.AbstractComponent;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.nlp.NLPGetter;
import com.clearnlp.nlp.NLPMode;
import com.clearnlp.reader.AbstractReader;
import com.clearnlp.tokenization.AbstractTokenizer;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;

import java.util.ArrayList;
import java.util.List;


public class ClearNLPInterface
{
	final String language = AbstractReader.LANG_EN;
	final String modelType  = "general-en";

	AbstractTokenizer tokenizer;
	AbstractComponent tagger;
	AbstractComponent parser;
	AbstractComponent identifier;
	AbstractComponent classifier;
	AbstractComponent labeler;

	List<AbstractComponent> components;

	public ClearNLPInterface()
	{
		try
		{
			Logger.getRootLogger().removeAllAppenders();
			Logger.getRootLogger().addAppender(new NullAppender());
			tokenizer  = NLPGetter.getTokenizer(language);
			tagger     = NLPGetter.getComponent(modelType, language, NLPMode.MODE_POS);
			parser     = NLPGetter.getComponent(modelType, language, NLPMode.MODE_DEP);
			identifier = NLPGetter.getComponent(modelType, language, NLPMode.MODE_PRED);
			classifier = NLPGetter.getComponent(modelType, language, NLPMode.MODE_ROLE);
			labeler    = NLPGetter.getComponent(modelType, language, NLPMode.MODE_SRL);

			components = new ArrayList<AbstractComponent>();
			components.add(tagger);
			components.add(parser);
			components.add(identifier);
			components.add(classifier);
			components.add(labeler);
		} 
		catch (Exception e)
		{
			System.err.println("Error loading ClearNLP dependency parser. ");
			e.printStackTrace();
		}
	}

	public DEPTree process(String sentence)
	{
		DEPTree tree = NLPGetter.toDEPTree(tokenizer.getTokens(sentence));

		for (AbstractComponent component : components)
			component.process(tree);
				
		return tree;
	}

}