package edu.colorado.clear.wsd.classifier;

import com.clearnlp.dependency.DEPNode;
import com.google.common.collect.Maps;
import de.bwaldvogel.liblinear.*;
import edu.colorado.clear.wsd.feature.*;
import edu.colorado.clear.wsd.feature.Feature;
import edu.colorado.clear.wsd.verbnet.VNInstance;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VerbNetSingleClassifier
{

	protected static double C = 0.01;
	protected static double eps = 0.001;

	/* maps from lemmas to maps of features to feature indices */
	protected Map<String, Integer> featureMap;
	public Map<String, Integer> senseToIndex;
	public Map<Integer, String> indexToSense;
	protected static final int BIAS_INDEX = 1;

	public static final String featureSeparator = "\t";

	protected File modelFile;
	protected File featureFile;
	protected File senseFile;

	protected Model model;

	protected boolean training;
	protected String lemma;

	protected VerbNetClassifier parent;

	protected static final String[] TOKEN_FEATURES = new String[] {
			TokenFeatures.lemma_key,
			TokenFeatures.pos_key,
			TokenFeatures.dep_key,
	};
	protected static Set<String> featSet = new HashSet<String>(Arrays.asList(TOKEN_FEATURES));

	protected static final String[] BOW_FEATURES = new String[] {
			TokenFeatures.lemma_key,
			TokenFeatures.pos_key,
			TokenFeatures.dep_key,
			WordNetExtractor.wn_key,
			DDNExtractor.ddn_key,
			BrownExtractor.brown_key,
			EmbeddingsExtractor.cluster_Key
	};
	protected static Set<String> bowFeatSet = new HashSet<String>(Arrays.asList(BOW_FEATURES));

	public VerbNetSingleClassifier(
			VerbNetClassifier parent, String lemma, boolean training) throws IOException
	{

		this.parent = parent;
		this.lemma = lemma;
		this.training = training;

		this.featureMap = parent.featureToIndexMap.get(lemma);
		this.senseToIndex = parent.senseToIndexMap.get(lemma);

		this.modelFile = new File(parent.modelsDir, lemma);
		this.featureFile = new File(parent.featuresDir, lemma);
		this.senseFile = new File(parent.senseDir, lemma);

		if (training)
		{
			this.featureMap = new TreeMap<String, Integer>();
			this.senseToIndex = new HashMap<String, Integer>();
		}
		else
		{
			this.model = Model.load(modelFile);
		}

		this.indexToSense = getReverseMap(senseToIndex);
	}

	public void writeFeatureMap(File featureFile) throws IOException
	{
		StringBuilder outFile = new StringBuilder();
		for (String key: featureMap.keySet())
		{
			String line = key + featureSeparator + featureMap.get(key) + "\n";
			outFile.append(line);
		}
		FileUtils.writeStringToFile(featureFile, outFile.toString());
	}

	public void writeSenseBank(File senseFile) throws IOException
	{
		StringBuilder outFile = new StringBuilder();
		for (String key: senseToIndex.keySet())
		{
			String line = key + featureSeparator + senseToIndex.get(key) + "\n";
			outFile.append(line);
		}
		FileUtils.writeStringToFile(senseFile, outFile.toString());
	}

	public FeatureNode[] getFeatures(VNInstance instance)
	{
		List<Feature> features = new ArrayList<Feature>();
		TokenFeatures.addFeatures(instance.getTree());
		DependencyFeatures.addWNFeatures(instance.getNode(), parent.wn);
		DependencyFeatures.addDDNFeatures(instance.getNode(), parent.ddn);
		DependencyFeatures.addClusterFeatures(instance.getNode(), parent.emb);
		DependencyFeatures.addBrownClusterFeatures(instance.getNode(), parent.brown);

		/* extract features here */
		features.addAll(TokenFeatures.extractNGramFeatures(instance.getTree(), 1, featSet));
		features.addAll(TokenFeatures.extractContextFeatures(
				instance.getTree(), instance.getNode(), new int[]{-2, -1, 0, 1, 2}, featSet, false));
		features.addAll(DependencyFeatures.rootPathFeatures(instance.getNode(), featSet));

		features.addAll(DependencyFeatures.extractArgFeatures(instance.getNode(), bowFeatSet));
		return convertFeatures(features);
	}

	public FeatureNode[] convertFeatures(List<Feature> features)
	{
		Map<Integer, FeatureNode> featureNodes = Maps.newTreeMap();
		featureNodes.put(BIAS_INDEX, new FeatureNode(BIAS_INDEX, 1));
		for (Feature f: features)
		{
			if (!featureMap.containsKey(f.toString()))
			{
				if (training)
				{
					featureMap.put(f.toString(), this.featureMap.size() + 1);
				} else
					continue;
			}
			int index = featureMap.get(f.toString());
			featureNodes.put(index, new FeatureNode(index, 1.0));
		}
		FeatureNode[] featureNodeArray = new FeatureNode[featureNodes.size()];
		/* add feature indices to feature node array */
		int ind = 0;
		for (Integer index : featureNodes.keySet())
		{
			featureNodeArray[ind] = featureNodes.get(index);
			++ind;
		}
		return featureNodeArray;
	}

	public void trainSingleClassifier(List<VNInstance> instances) throws IOException
	{
	    /* extract features */
		List<Double> yVals = new ArrayList<Double>();
		List<FeatureNode[]> features = new ArrayList<FeatureNode[]>();
		for (VNInstance instance : instances)
		{
			FeatureNode[] featuresList = getFeatures(instance);
			if (!senseToIndex.containsKey(instance.getVnclass()))
				senseToIndex.put(instance.getVnclass(), senseToIndex.size() + 1);
			int index = senseToIndex.get(instance.getVnclass());
			yVals.add((double) index);
			features.add(featuresList);
		}

	    /* set up liblinear problem */
		Problem problem = new Problem();
		problem.y = new double[features.size()];
		for (int i = 0; i < yVals.size(); ++i)
		{
			problem.y[i] = yVals.get(i);
		}
		problem.l = features.size(); /* number of training examples */
		problem.n = featureMap.size(); /* number of features */
		problem.x = features.toArray(new FeatureNode[features.size()][]); /* feature nodes */
		SolverType solver = SolverType.MCSVM_CS;
		Parameter parameter = new Parameter(solver, C, eps);
	    /* train and save model */
		Linear.disableDebugOutput();
		this.model = Linear.train(problem, parameter);
		writeFeatureMap(featureFile); /* save feature map */
		writeSenseBank(senseFile); /* save sense map */
		this.indexToSense = getReverseMap(senseToIndex);
		model.save(modelFile);
	}

	public double[] getPrediction(VNInstance instance)
	{
		FeatureNode[] features = getFeatures(instance);
		double[] probabilities = new double[senseToIndex.size()];
		Linear.predictValues(model, features, probabilities);
		return probabilities;
	}

	public String classify(VNInstance instance)
	{
		FeatureNode[] features = getFeatures(instance);
		double[] probabilities = new double[senseToIndex.size()];
		Linear.predictValues(model, features, probabilities);
		String maxSense = "";
		double score = -Double.MAX_VALUE;
		for (int ind = 0; ind < probabilities.length; ++ind)
		{
			if (probabilities[ind] > score)
			{
				score = probabilities[ind];
				maxSense = this.getSense(ind);
			}
		}
		return maxSense;
	}

	public Map<Integer, String> getReverseMap(Map<String, Integer> map)
	{
		Map<Integer, String> newMap = new HashMap<Integer, String>();
		for (String key: map.keySet())
		{
			newMap.put(map.get(key), key);
		}
		return newMap;
	}

	public String getSense(int index)
	{
		return indexToSense.get(index+1);
	}

	public static void cleanFeats(DEPNode node)
	{
		for (String s: BOW_FEATURES)
		{
			node.removeFeat(s);
		}
		node.removeFeat(TokenFeatures.form_key);
	}

}
