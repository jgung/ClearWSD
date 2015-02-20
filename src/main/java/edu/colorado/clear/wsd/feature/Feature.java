package edu.colorado.clear.wsd.feature;

public class Feature
{

	public static final String separator = "_";
	public static final String ngramStr = "-gram";
	public static final String valSeparator = " ";

	protected String name;
	protected String value;

	public Feature(String name, String value)
	{
		this.name = name;
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name) { this.name = name; }
	public String toString()
	{
		return name + separator + value;
	}

}
