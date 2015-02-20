package edu.colorado.clear.wsd.verbnet;

import com.clearnlp.dependency.DEPLib;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.morphology.MPLibEn;

/* VerbNet instance */
public class VNInstance
{

	protected String path;
	protected int sentence;
	protected int token;
	protected String verb;
	protected String vnclass;
	protected String instance;

	protected DEPTree tree;
	protected DEPNode node;

	public VNInstance(DEPTree tree, DEPNode node)
	{
		this.tree = tree;
		this.node = node;
		this.verb = node.lemma;
	}

	public VNInstance(String line)
	{
		this.instance = line;
		try
		{
			String[] fields = line.split("\\s");
			this.path = fields[0];
			this.sentence = Integer.parseInt(fields[1]);
			this.token = Integer.parseInt(fields[2]);
			this.verb = fields[3];
			if (verb.contains("-"))
				verb = verb.split("-")[0];
			String field = fields[4];
			
			if (!field.contains("-"))
				this.vnclass = this.verb + "-other";
			else
				this.vnclass = fields[4].substring(fields[4].indexOf("-")+1);
		} catch (Exception e)
		{
			System.err.println("Error parsing VN instance: " + instance);
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static boolean addToDEPTree(DEPTree tree, VNInstance instance)
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

	public static boolean addToDEPTree(DEPNode node, VNInstance instance)
	{
		if (node!=null)
		{
			node.addFeat(DEPLib.FEAT_VN, instance.getVnclass());
			return true;
		}
		else
			return false;
	}

	public static DEPNode findDEPNode(DEPTree tree, VNInstance instance)
	{
		DEPNode candidate = null;
		for (int i = 0; i < tree.size(); ++i)
		{
			DEPNode node = tree.get(i);
			if (MPLibEn.isVerb(node.pos))
			{
				if (node.lemma.startsWith(instance.verb))
					candidate = node;
			}
			else if (node.lemma.startsWith(instance.verb) && instance.getToken() == i)
			{
				candidate = node;
			}
		}
		return candidate;
	}

	public VNInstance(String verb, String vnClass)
	{
		this.verb = verb;
		this.vnclass = vnClass;
	}

	public VNInstance(DEPNode node, DEPTree tree)
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

	public String getVerb(){
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}

	public String getVnclass() {
		return vnclass;
	}

	public void setVnclass(String vnclass) {
		this.vnclass = vnclass;
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
