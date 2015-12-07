package com.fletch22.orb.query;

import com.fletch22.orb.cache.query.CriteriaCollection;
import com.fletch22.orb.query.CriteriaImpl;

public interface QueryManager extends CriteriaManager {
	
	public OrbResultSet executeQuery(long orbTypeInternalId, String queryLabel);

	public OrbResultSet executeQuery(CriteriaImpl criteria);
	
	public OrbResultSet findByAttribute(long orbTypeInternalId, String attributeName, String attributeValueToFind);
	
	public CriteriaCollection getCriteriaCollection();
}
