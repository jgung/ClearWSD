package edu.colorado.clear.wsd.classifier;

import com.clearnlp.dependency.DEPLib;
import com.clearnlp.dependency.DEPLibEn;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.morphology.MPLibEn;
import edu.colorado.clear.wsd.feature.BrownExtractor;
import edu.colorado.clear.wsd.feature.DDNExtractor;
import edu.colorado.clear.wsd.feature.EmbeddingsExtractor;
import edu.colorado.clear.wsd.feature.WordNetExtractor;
import edu.colorado.clear.wsd.verbnet.VnPbMapping;
import edu.colorado.clear.wsd.verbnet.VNInstance;
import edu.colorado.clear.wsd.verbnet.VerbNetReader;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
public class VerbNetClassifier
{ /* maps from lemmas to maps of features to feature indices */
    private static final Logger LOG = LoggerFactory.getLogger(VerbNetClassifier.class);
	protected Map<String, Map<String, Integer>> featureToIndexMap;
	protected Map<String, Map<String, Integer>> senseToIndexMap;
	protected Map<String, VerbNetSingleClassifier> classifiers;

	public static final String featureDirPath = "features";
	public static final String modelsDirPath = "models";
	public static final String senseBankPath = "senseBank";
	public static final String clustersPath = "clusters/640-dim";
	public static final String ddnPath = "ddnIndex";
	public static final String wnPath = "WordNet-3.0/dict";
	public static final String bwcFile = "bwc.txt";
	public static final String mappingFile = "pbvn-map.xml";
	public static final String classListFile = "vnclasses.txt";
	public static final String featureSeparator = "\t";
	protected File featuresDir;
	protected File modelsDir;
	protected File senseDir;
	protected File dataDir;
	protected WordNetExtractor wn;
	protected DDNExtractor ddn;
	protected EmbeddingsExtractor emb;
	protected BrownExtractor brown;
	protected VnPbMapping mapping;
	protected boolean training;
	private static final boolean removeTrainingFeats = true;

	public VerbNetClassifier(File dataDir, boolean training) throws IOException
	{
		this.featureToIndexMap = new HashMap<String, Map<String, Integer>>();
		this.senseToIndexMap = new HashMap<String, Map<String, Integer>>();
		this.dataDir = dataDir;
		this.featuresDir = new File(dataDir, featureDirPath);
		this.modelsDir = new File(dataDir, modelsDirPath);
		this.senseDir = new File(dataDir, senseBankPath); /* initialize feature extractors */
		wn = new WordNetExtractor(new File(dataDir, wnPath));
		ddn = new DDNExtractor(new File(dataDir, ddnPath));
		emb = new EmbeddingsExtractor(new File(dataDir, clustersPath));
		brown = new BrownExtractor(new File(dataDir, bwcFile));
		this.mapping = new VnPbMapping(new File(dataDir, mappingFile),
				new File(dataDir, classListFile)); this.training = training;
		this.classifiers = new HashMap<String, VerbNetSingleClassifier>();
		if (!modelsDir.exists())
			FileUtils.forceMkdir(modelsDir);
		if (!featuresDir.exists())
			FileUtils.forceMkdir(featuresDir);
		if (!senseDir.exists())
			FileUtils.forceMkdir(senseDir);
		if (!training) {
			for (File featureFile : featuresDir.listFiles())
			{
				Map<String, Integer> featureMap = new TreeMap<String, Integer>();
				for (String s : FileUtils.readFileToString(featureFile).split("\n")) {
					String[] fields = s.split(featureSeparator);
					if (s.split(featureSeparator).length > 1)
						featureMap.put(fields[0], Integer.parseInt(fields[1]));
				}
				featureToIndexMap.put(featureFile.getName(), featureMap);
			}
			for (File senseFile : senseDir.listFiles())
			{
				Map<String, Integer> senseMap = new HashMap<String, Integer>();
				for (String s : FileUtils.readFileToString(senseFile).split("\n"))
				{
					String[] fields = s.split(featureSeparator);
					if (s.split(featureSeparator).length > 1)
						senseMap.put(fields[0], Integer.parseInt(fields[1]));
				}
				senseToIndexMap.put(senseFile.getName(), senseMap);
			}
			for (File modelFile : modelsDir.listFiles())
			{
				classifiers.put(modelFile.getName(), getSingleClassifier(modelFile.getName()));
			}
		}
	}

	public void classify(DEPNode node, DEPTree inputTree)
	{
		VerbNetSingleClassifier classifier = this.classifiers.get(node.lemma);
		String vnClass;
		if (classifier != null)
		{
			VNInstance instance = new VNInstance(inputTree, node);
			vnClass = classifier.classify(instance);
		}
		else
		{
			vnClass = this.mapping.getFirstClass(node.lemma);
		}
		if (vnClass != null)
		{
			node.addFeat(DEPLib.FEAT_VN, vnClass);
			mapping.addVNRoles(node);
		}
	}
	public void classify(DEPTree inputTree)
	{
		for (DEPNode node: inputTree)
		{
			if (MPLibEn.isVerb(node.pos))
			{
				classify(node, inputTree);
			}
		}
		/* optionally remove feats added to DEPTree during training */
		if (removeTrainingFeats)
		{
			for (DEPNode node : inputTree)
				VerbNetSingleClassifier.cleanFeats(node);
		}
	}

	public void train(File corpusFile, File annotationsFile) throws IOException
	{
		Set<String> verbs = VerbNetReader.getVerbs(annotationsFile);
		for (String lemma: verbs)
		{
			List<VNInstance> instances = VerbNetReader.readAnnotations(annotationsFile, corpusFile, lemma);
			VerbNetSingleClassifier single = this.getSingleClassifier(lemma);
			if (instances.size() > 0)
			{
				System.out.println(lemma + "\t" + instances.size());
				single.trainSingleClassifier(instances);
			}
		}
	}

	public double test(File corpusFile, File annotationsFile) throws IOException
	{
		Set<String> verbs = VerbNetReader.getVerbs(annotationsFile);
		double correct = 0;
		double total = 0;
		for (String lemma: verbs)
		{
            List<VNInstance> instances = VerbNetReader.readAnnotations(annotationsFile, corpusFile, lemma);
            double lemmaTotal = instances.size();
            double lemmaCorrect = 0;
			for (VNInstance i: instances)
			{
				String cls = i.getVnclass();
				this.classify(i.getNode(), i.getTree());
				String pred = i.getNode().getFeat(DEPLibEn.FEAT_VN);
				if (pred.equals(cls))
					++lemmaCorrect;
			}
            LOG.info("Lemma {} score: {}", lemma, lemmaCorrect/lemmaTotal);
            correct += lemmaCorrect;
            total += lemmaTotal;
		}
		return correct/total;
	}

	public VerbNetSingleClassifier getSingleClassifier(String lemma) throws IOException
	{
		return new VerbNetSingleClassifier(this, lemma, this.training);
	}

}
