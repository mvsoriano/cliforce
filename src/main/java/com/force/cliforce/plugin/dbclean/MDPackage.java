package com.force.cliforce.plugin.dbclean;

import java.util.ArrayList;
import java.util.List;

public class MDPackage {

	private String fullName;
	private String version;
	
	public List<String> customObjects = new ArrayList<String>();
	
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void addCustomObject(String name) {
		customObjects.add(name);
	}
		
	public String toXML() {
		StringBuilder b = new StringBuilder();
		
		b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		b.append("<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");
		if(fullName!=null) {
			b.append("  <fullName>"+fullName+"</fullName>\n");
		}
		if(!customObjects.isEmpty()) {
			b.append("  <types>\n");
			for(String s : customObjects) {
				b.append("    <members>"+s+"</members>\n");
			}
			b.append("    <name>CustomObject</name>\n</types>\n");
		}
		if(version!=null) {
			b.append("  <version>"+version+"</version>\n");
		}
		b.append("</Package>\n");
		return b.toString();
	}
}
