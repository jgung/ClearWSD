package edu.colorado.clear.wsd.verbnet;

import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPFeat;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.reader.AbstractColumnReader;
import com.clearnlp.reader.DEPReader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* reads annotations from the VerbNet corpus into ClearNLP format */
public class VerbNetReader
{
	// indices of ClearNLP fields
	public static final int i_id = 0;
	public static final int i_form = 1;
	public static final int i_lemma = 2;
	public static final int i_pos = 3;
	public static final int i_feats = 4;
	public static final int i_headId = 5;
	public static final int i_deprel = 6;

	public static Set<String> getVerbs(File vnFile) throws IOException
	{
		Set<String> verbs = new HashSet<String>();
		for (String line: FileUtils.readFileToString(vnFile).split("\n"))
		{
			VNInstance instance = new VNInstance(line);
			verbs.add(instance.getVerb());
		}
		return verbs;
	}

	/* Read annotations of this format: wb_eng/00/eng_0012.v01000.dep 98 3 read learn-14-2-1 */
	public static List<VNInstance> readAnnotations(File vnFile, File corpusDirectory, String lemma) throws IOException
	{
		List<VNInstance> instances = new ArrayList<VNInstance>();
		for (String line: FileUtils.readFileToString(vnFile).split("\n"))
		{
			VNInstance instance = new VNInstance(line);
			if (instance.getVerb().equals(lemma))
			{
				instances.add(instance);
			}
		}

		List<VNInstance> remove = new ArrayList<VNInstance>();
		for (VNInstance i: instances)
		{
			String path = corpusDirectory.getPath() + File.separator + i.getPath();
			File depFile = new File(path);
			if (depFile.exists())
			{
				List<DEPTree> trees = readClearNLPFile(depFile);
				DEPTree tree = trees.get(i.getSentence());
				if (!VNInstance.addToDEPTree(tree, i)) /* add verbnet class to parse */
					remove.add(i);
			} else
				remove.add(i);
		}

		instances.removeAll(remove);
		return instances;

	}

	// Read a ClearNLP dependency file
	public static List<DEPTree> readClearNLPFile(File cnlpFile) throws IOException
	{
		List<DEPTree> depTrees = new ArrayList<DEPTree>();
		// individual DEPTrees are separated by a blank line
		for (String tree : FileUtils.readFileToString(cnlpFile).split("\n\n"))
		{
			List<String[]> depTree = new ArrayList<String[]>();
			for (String node: tree.split("\n"))
				depTree.add(node.split(DEPReader.DELIM_COLUMN));
			depTrees.add(getDEPTree(depTree));
		}
		return depTrees;
	}

	// Create a ClearNLP DEPTree
	protected static DEPTree getDEPTree(List<String[]> lines)
	{
		int id, headId, i, size = lines.size();
		String form, lemma, pos, deprel;
		DEPFeat feats;
		String[] tmp;
		DEPNode node;

		DEPTree tree = new DEPTree();
		tree.get(0).initDependents();
		// initialize place holders
		for (i = 0; i < size; i++)
		{
			DEPNode newNode = new DEPNode();
			newNode.initDependents();
			tree.add(newNode);
		}

		for (i = 0; i < size; i++)
		{
			tmp = lines.get(i);
			id = Integer.parseInt(tmp[i_id]);
			form = tmp[i_form];
			lemma = tmp[i_lemma];
			pos = tmp[i_pos];
			feats = new DEPFeat(tmp[i_feats]);

			node = tree.get(id);
			node.init(id, form, lemma, pos, feats);

			if (node.getSHeads() == null)
				node.initSHeads();
			if (!tmp[i_headId].equals(AbstractColumnReader.BLANK_COLUMN))
			{
				headId = Integer.parseInt(tmp[i_headId]);
				deprel = tmp[i_deprel];

				node.setHead(tree.get(headId), deprel);
				tree.get(headId).addDependent(new DEPArc(node, deprel)); // ?
			}
		}
		return tree;
	}
}
