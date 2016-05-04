package com.fletch22.orb.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbType;
import com.fletch22.orb.cache.local.Cache;
import com.fletch22.orb.cache.query.CriteriaCollection;
import com.fletch22.orb.command.transaction.RollbackTransactionService;
import com.fletch22.orb.query.Criteria;
import com.fletch22.orb.query.constraint.Constraint;
import com.fletch22.orb.query.event.QueryAttributeDeleteHandler;

@Component
public class QueryManagerImpl extends AbstractCriteriaManager implements QueryManager {

	Logger logger = LoggerFactory.getLogger(QueryManagerImpl.class);

	@Autowired
	Cache cache;

	@Autowired
	RollbackTransactionService rollbackTransactionService;
	
	@Autowired
	QueryAttributeDeleteHandler queryAttributeDeleteHandler;
	
	@Override
	public CriteriaCollection getCriteriaCollection() {
		return cache.queryCollection;
	}

	@Override
	public OrbResultSet executeQuery(String queryLabel) {
		Criteria criteria = this.getCriteriaCollection().findByLabel(queryLabel);
		return cache.orbCollection.executeQuery(criteria);
	}

	@Override
	public OrbResultSet executeQuery(Criteria criteria) {
		return cache.orbCollection.executeQuery(criteria);
	}

//	protected Criteria findQuery(long criteriaOrbTypeInternalId, String queryLabel) {
//
//		boolean isCriteriaFound = false;
//		Criteria criteriaFound = null;
//		
//		OrbType orbType = getParentOrbType();
//		List<Orb> orbList = orbManager.getOrbsOfType(orbType.id);
//		for (Orb orb : orbList) {
//			criteriaFound = get(orb.getOrbInternalId());
//			
//			for (String prop : orb.getUserDefinedProperties().keySet()) {
//				logger.info("Prop: {}", prop);
//			}
//			logger.info("oid: {}", orb.getOrbInternalId());
//			logger.info("typeOid: {}", orb.getOrbTypeInternalId());
//			logger.info("Is criteria found null? {}", (null == criteriaFound));
//			
//			if (criteriaFound.getLabel().equals(queryLabel)
//			&& criteriaFound.getOrbTypeInternalId() == criteriaOrbTypeInternalId) {
//				isCriteriaFound = true;
//				break;
//			}
//		}
//
//		if (!isCriteriaFound) {
//			throw new RuntimeException(String.format("Encountered problem trying to find query. Couldn't find query '%s'.", queryLabel));
//		}
//
//		return criteriaFound;
//	}

	@Override
	public OrbResultSet findByAttribute(long orbTypeInternalId, String attributeName, String attributeValueToFind) {

		OrbType orbType = orbTypeManager.getOrbType(orbTypeInternalId);
		Criteria criteria = new CriteriaStandard(orbType.id, "findByAttribute");
		criteria.addAnd(Constraint.eq(attributeName, attributeValueToFind));

		return executeQuery(criteria);
	}

	@Override
	public CriteriaAttributeDeleteHandler getCriteriaAttributeDeleteHandler() {
		return this.queryAttributeDeleteHandler;
	}
}
