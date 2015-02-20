package edu.colorado.clear.wsd.verbnet;

import com.clearnlp.dependency.DEPLib;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.srl.SRLArc;
import com.clearnlp.dependency.srl.SRLLib;
import com.clearnlp.propbank.verbnet.PVMap;
import com.clearnlp.propbank.verbnet.PVRole;
import com.clearnlp.propbank.verbnet.PVRoles;
import com.clearnlp.propbank.verbnet.PVRoleset;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class VnPbMapping
{

	public static final String VN_ROLE = "vnrole";
	protected PVMap pbvnMap;
	protected Map<String, List<String>> verbClassMap;

	/* for monosemous verbs, use this to get appropriate class
	* returns null if no vn-Listclasses found*/
	public String getFirstClass(String lemma)
	{
		List<String> entry = verbClassMap.get(lemma);
		if (entry == null)
			return null;
		return verbClassMap.get(lemma).get(0);
	}

	public void addVNRoles(DEPNode node)
	{
		String vncls = node.getFeat(DEPLib.FEAT_VN);
		String pb = node.getFeat(DEPLib.FEAT_PB);
		if (vncls != null && pb != null)
		{
			PVRoles roles = getPVRoles(pb, vncls);
			if (roles != null)
				addVNRoles(node, roles);
		}
	}

	public PVRoles getPVRoles(String roleset, String vnClass)
	{
		PVRoleset rs = pbvnMap.getRoleset(roleset);
		if (rs == null)
			return null;
		return rs.getSubVNRoles(vnClass);
	}

	public void addVNRoles(DEPNode node, PVRoles roles)
	{
		for (DEPNode dep: node.getDependentNodeList())
		{
			SRLArc role = dep.getSHead(node);
			if (role == null)
				continue;
			String n = SRLLib.getBaseLabel(role.getLabel()).replace("A", "");
			PVRole pvRole = roles.getRole(n);
			if (pvRole != null)
				dep.addFeat(VN_ROLE, pvRole.vntheta);
		}
	}

	public void setSubclass(DEPNode node)
	{
		if (node == null)
			return;
		String vncls = node.getFeat(DEPLib.FEAT_VN);
		for (String cls: verbClassMap.get(node.lemma))
			if (cls.startsWith(vncls))
			{
				node.addFeat(DEPLib.FEAT_VN, cls);
			}
	}

	public VnPbMapping(File mappingFile, File vnFile)
	{
		try
		{
			this.pbvnMap = new PVMap(new FileInputStream(mappingFile));
			this.verbClassMap = new HashMap<String, List<String>>();
			for (String s: FileUtils.readFileToString(vnFile).split("\n"))
			{
				String verb = s.split("\t")[0];
				String[] clses = s.split("\t")[1].split(" ");
				Set<String> clsSet = new HashSet<String>();
				Collections.addAll(clsSet, clses);
				verbClassMap.put(verb, new ArrayList<String>(clsSet));
			}
		}
		catch (IOException e)
		{
			System.err.println("Problem loading mappings file");
			e.printStackTrace();
		}
	}

}