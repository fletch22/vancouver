package com.fletch22.app.designer.page.head;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.fletch22.app.designer.Parent;
import com.fletch22.app.designer.page.PageChild;

public class Head extends Parent implements PageChild {

	public static final String TYPE_LABEL = "Head";
	public static final String ATTR_LABEL = "label";
	public static final String ATTR_TITLE = "title";
	public static LinkedHashSet<String> ATTRIBUTE_LIST = new LinkedHashSet<String>(Arrays.asList(Head.ATTR_PARENT, ATTR_LABEL, ATTR_TITLE, ATTR_CHILDREN, ATTR_ORDINAL));
	
	@Override
	public String getTypeLabel() {
		return TYPE_LABEL;
	}
}
