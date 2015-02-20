package edu.colorado.clear.wsd.ontonotes;

import com.clearnlp.dependency.DEPLib;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.morphology.MPLibEn;

/* OntoNotes sense instance */
public class ONInstance
{

	protected String path;
	protected int sentence;
	protected int token;
	protected String lemma;
	protected String sense;
	protected String instance;

	protected DEPTree tree;
	protected DEPNode node;

	public ONInstance(DEPTree tree, DEPNode node)
	{
		this.tree = tree;
		this.node = node;
		this.lemma = node.lemma;
	}

	public ONInstance(String line)
	{
		this.instance = line;
		try
		{
			String[] fields = line.split("\\s");
			this.path = fields[0];
			this.sentence = Integer.parseInt(fields[1]);
			this.token = Integer.parseInt(fields[2]);
			this.lemma = fields[3];
			if (lemma.contains("-"))
				lemma = lemma.split("-")[0];

			if (fields.length > 5) /* adjudicated sense */
				this.sense = fields[5];
			else
				this.sense = fields[4];

		} catch (Exception e)
		{
			System.err.println("Error parsing OntoNotes instance: " + instance);
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static boolean addToDEPTree(DEPTree tree, ONInstance instance)
	{
		DEPNode node = findDEPNode(tree, instance);
		if (addToDEPTree(node, instance))
		{
			instance.setTree(tree);
			instance.setNode(node);
			return true;
		}
		else
			return false;
	}

	public static boolean addToDEPTree(DEPNode node, ONInstance instance)
	{
		if (node!=null)
		{
			node.addFeat(DEPLib.FEAT_WS, instance.getSense());
			return true;
		}
		else
			return false;
	}

	public static DEPNode findDEPNode(DEPTree tree, ONInstance instance)
	{
		DEPNode candidate = null;
		for (int i = 0; i < tree.size(); ++i)
		{
			DEPNode node = tree.get(i);
			if (MPLibEn.isVerb(node.pos))
			{
				if (node.lemma.startsWith(instance.lemma))
					candidate = node;
			}
			else if (node.lemma.startsWith(instance.lemma) && instance.getToken() == i)
			{
				candidate = node;
			}
		}
		return candidate;
	}

	public ONInstance(String lemma, String sense)
	{
		this.lemma = lemma;
		this.sense = sense;
	}

	public ONInstance(DEPNode node, DEPTree tree)
	{
		this.tree = tree;
		this.node = node;
	}

	public String toString()
	{
		return instance;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getSentence() {
		return sentence;
	}

	public void setSentence(int sentence) {
		this.sentence = sentence;
	}

	public int getToken() {
		return token;
	}

	public void setToken(int token) {
		this.token = token;
	}

	public String getlemma(){
		return lemma;
	}

	public void setlemma(String lemma) {
		this.lemma = lemma;
	}

	public String getSense() {
		return sense;
	}

	public void setSense(String sense) {
		this.sense = sense;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public DEPNode getNode()
	{
		return node;
	}

	public DEPTree getTree()
	{
		return tree;
	}

	public void setTree(DEPTree tree)
	{
		this.tree = tree;
	}

	public void setNode(DEPNode node)
	{
		this.node = node;
	}
}
