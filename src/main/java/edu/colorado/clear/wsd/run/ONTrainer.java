package edu.colorado.clear.wsd.run;

import edu.colorado.clear.wsd.classifier.ONClassifier;

import java.io.File;
import java.io.IOException;

/**
 * Train an OntoNotes Verb Sense Classifier
 */
public class ONTrainer
{
	protected String corpusPath;
	protected String annPath ;
	protected String dataPath;

	public boolean handleArgs(String... args)
	{
		String corpusPath = "";
		String annPath = "";
		String dataPath = "";
		try
		{
			if (args.length < 2)
				throw new IllegalArgumentException();
			for (int i = 0; i < args.length - 1; ++i)
			{
				String arg = args[i];
				String val = args[i + 1];
				if (arg.length() != 2 || arg.charAt(0) != '-')
				{
					System.err.println("Invalid argument: " + arg);
					throw new IllegalArgumentException();
				}
				switch (arg.charAt(1))
				{
					case 'a':
						annPath = val;
						++i;
						break;
					case 'c':
						corpusPath = val;
						++i;
						break;
					case 'd':
						dataPath = val;
						++i;
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
			if (annPath.length() == 0 || corpusPath.length() == 0 || dataPath.length() == 0)
			{
				System.err.println("Missing required parameter.");
				throw new IllegalArgumentException();
			} else
			{
				this.annPath = annPath;
				this.corpusPath = corpusPath;
				this.dataPath = dataPath;
				return true;
			}
		} catch (IllegalArgumentException e)
		{
			System.err.println("Usage:\n" +
					" -a <annPath> : path to file containing sense annotations\n" +
					" -c <corpusPath> : path to file containing sense annotations\n" +
					" -d <dataPath> : path to directory containing ddnIndex, clusters, and WordNet-3.0 (also output directory for models)\n");
		}
		return false;
	}

	public ONTrainer(String... args)
	{
		if (handleArgs(args))
		{
			try
			{
				train(new File(corpusPath), new File(annPath), new File(dataPath));
			} catch (IOException e)
			{
				System.err.println("Error during training: ");
				e.printStackTrace();
			}
		}
	}

	public static void main(String... args)
	{
		new ONTrainer(args);
	}

	public void train(File corpusDir, File annFile, File outputDir) throws IOException
	{
		ONClassifier classifier = new ONClassifier(outputDir, true);
		classifier.train(corpusDir, annFile);
	}
}
