package com.fletch22.app.designer.page;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class Page {
	
	public static final String TYPE_LABEL = "Page";
	public static final String ATTR_LABEL = "label";
	public static final String ATTR_PAGE = "page";
	public static LinkedHashSet<String> ATTRIBUTE_LIST = new LinkedHashSet<String>(Arrays.asList(ATTR_LABEL, ATTR_PAGE)); 
}
