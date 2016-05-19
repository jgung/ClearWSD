package edu.colorado.clear.wsd.run;

import com.clearnlp.dependency.DEPLib;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import edu.colorado.clear.wsd.classifier.VerbNetClassifier;
import edu.colorado.clear.wsd.util.ClearNLPInterface;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * VerbNet classification
 */
public class VNClassify
{
	protected String dataPath;
	protected String inputPath;
	protected String outputPath;
	protected boolean outputDep = false;
	public static final String outputExtension = "vn";
	private static final Logger LOG = LoggerFactory.getLogger(VNClassify.class);

	public boolean handleArgs(String... args)
	{
		String inputPath = "";
		String dataPath = "";
		String outputPath = "";

		try
		{
			if (args.length < 2)
				throw new IllegalArgumentException();
			for (int i = 0; i < args.length; ++i)
			{
				String arg = args[i];
				String val = "";
				if (i + 1 < args.length) val = args[i + 1];
				if (arg.length() != 2 || arg.charAt(0) != '-')
				{
					System.err.println("Invalid argument: " + arg);
					throw new IllegalArgumentException();
				}
				switch (arg.charAt(1))
				{
					case 'i':
						inputPath = val;
						++i;
						break;
					case 'd':
						dataPath = val;
						++i;
						break;
					case 'o':
						outputPath = val;
						++i;
						break;
					case 'v':
						outputDep = true;
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
			if (inputPath.length() == 0 || dataPath.length() == 0)
			{
				System.err.println("Missing required parameter.");
				throw new IllegalArgumentException();
			} else
			{
				this.inputPath = inputPath;
				this.dataPath = dataPath;
				this.outputPath = outputPath;
				return true;
			}
		} catch (IllegalArgumentException e)
		{
			System.err.println("Usage:\n" +
					" -i <inputPath> : path to input file (one word per line, sentences separated by blank lines)\n" +
					" -d <dataPath> : path to data directory\n" +
					" -o <outputName> : name of output file\n" +
					" -v <verboseOutput> : include ClearNLP output (optional)");
		}
		return false;
	}

	public VNClassify(String... args)
	{
		if (handleArgs(args))
		{
			try
			{
				VerbNetClassifier classifier = new VerbNetClassifier(new File(dataPath), false);
				classify(new File(inputPath), outputPath, classifier);
			} catch (IOException e)
			{
				System.err.println("Error loading classifier.");
				e.printStackTrace();
			}
		}
	}

	/**
     	* Performs VSD on a given list of sentences.
     	* @param sentences {"The man eats his food.","The dog barks."}
     	* changes: kaustubhdhole
     	*/
    	public static  Map<String, Map<String, String>> VNClassifySentences(String dataPath, String[] sentences) 
    	{
        	Map<String, Map<String, String>> sentenceToSenses = null;
        	try 
        	{
            		VerbNetClassifier classifier = new VerbNetClassifier(new File(dataPath), false);
            		sentenceToSenses = classify(sentences, classifier);
            		
        	} catch (IOException e) 
        	{
            		System.err.println("Error loading classifier.");
            		e.printStackTrace();
        	}
        	for(String sentence:sentenceToSenses.keySet())
        	{
            		Map<String,String> tokenMap = sentenceToSenses.get(sentence);
            		LOG.info(sentence);
            		for(String verb:tokenMap.keySet()){
                		LOG.info(verb + "\t" + tokenMap.get(verb));
            		}
        	}
        	return sentenceToSenses;
    	}

	public static void main(String... args)
	{
		String dataPath = "/home/../../ClearWSD-master/data/vndata";
        	String[] sentences = {"The man eats Bombay food.","The mother speaks and the child smiles."};
        	VNClassifySentences( dataPath, sentences);
	}

	public void classify(File inputFile, String outputName, VerbNetClassifier classifier)
	{
		String input;
		try
		{
			input = FileUtils.readFileToString(inputFile);
		} catch (IOException e)
		{
			System.err.println("Error reading input file: " + inputPath);
			e.printStackTrace();
			return;
		}
		String[] sentences = input.split("\n\n");
		LOG.info("Loading ClearNLP");
		ClearNLPInterface cnlp = new ClearNLPInterface();
		StringBuilder output = new StringBuilder();
		for (String sentence: sentences)
		{
			sentence = sentence.replace("\n", " ");

			DEPTree tree = cnlp.process(sentence);
			classifier.classify(tree);

			if (outputDep)
			{
				output.append(tree.toStringDEP()).append("\n");
			} else
			{
				for (DEPNode node: tree)
				{
					if (!node.hasHead())
						continue;
					String sense = node.getFeat(DEPLib.FEAT_VN);
					if (sense == null) sense = "";
					output.append(node.form).append("\t").append(sense).append("\n");
				}
			}
			output.append("\n");
		}
		File outputFile;
		if (outputName.length() == 0)
		{
			outputFile = new File(inputPath + "." + outputExtension);
		} else
		{
			outputFile = new File(inputFile.getParent(), outputPath);
		}
		LOG.info("Writing to {}", outputFile.getAbsolutePath());
		try
		{
			FileUtils.writeStringToFile(outputFile, output.toString());
		} catch (IOException e)
		{
			System.err.println("Error writing output to file: " + outputFile.getAbsolutePath());
			e.printStackTrace();
		}

	}

    	/**
     	* Perform VSD on a given list of sentences
     	* changes: kaustubhdhole
     	*/
    	public static Map<String, Map<String, String>> classify(String[] sentences, VerbNetClassifier classifier) {

        	LOG.info("Loading ClearNLP");
        	ClearNLPInterface cnlp = new ClearNLPInterface();

        	String sense;
        	Map<String, String> tokenToSense;
        	Map<String, Map<String, String>> sentenceToSenses = new HashMap<String, Map<String, String>>();

        	for (String sentence : sentences) 
        	{
            		tokenToSense = new HashMap<String, String>();
            		DEPTree tree = cnlp.process(sentence);
            		classifier.classify(tree);
            		for (DEPNode node : tree) 
            		{
                		if (!node.hasHead())
                			 continue;
                		sense = node.getFeat(DEPLib.FEAT_VN);
                		if (sense != null)
                    			tokenToSense.put(node.form, sense);
            		}
            		sentenceToSenses.put(sentence, tokenToSense);
        	}

        	return sentenceToSenses;
	}

}
