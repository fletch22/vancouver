package com.fletch22.orb.query;

import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.googlecode.cqengine.query.QueryFactory.or;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fletch22.Fletch22ApplicationContext;
import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbType;
import com.fletch22.orb.OrbTypeManager;
import com.fletch22.orb.cache.local.Cache;
import com.fletch22.orb.cache.local.CacheEntry;
import com.fletch22.orb.cache.local.OrbCollection;
import com.fletch22.orb.query.CriteriaFactory.Criteria;
import com.fletch22.orb.query.sort.CriteriaSortInfo;
import com.fletch22.orb.query.sort.GrinderSortInfo;
import com.fletch22.orb.query.sort.OrbComparator;
import com.fletch22.orb.search.GeneratedCacheEntryClassFactory;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.SimpleNullableAttribute;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;

public class ConstraintGrinder {
	
	Logger logger = LoggerFactory.getLogger(ConstraintGrinder.class);
	
	Criteria criteria;
	IndexedCollection<CacheEntry> indexedCollection;
	long orbTypeInternalId;
		
	Query<CacheEntry> query = null;
	ConstraintKitchen constraintKitchen;

	private OrbTypeManager orbTypeManager;
	
	private OrbCollection orbCollection;
	
	public ConstraintGrinder(Criteria criteria, IndexedCollection<CacheEntry> indexedCollection) {
		this.criteria = criteria;
		this.indexedCollection = indexedCollection;
		this.orbTypeInternalId = criteria.getOrbTypeInternalId();
		this.constraintKitchen = (ConstraintKitchen) Fletch22ApplicationContext.getApplicationContext().getBean(ConstraintKitchen.class);
		this.orbTypeManager = (OrbTypeManager) Fletch22ApplicationContext.getApplicationContext().getBean(OrbTypeManager.class);
		this.orbCollection = (OrbCollection) Fletch22ApplicationContext.getApplicationContext().getBean(Cache.class).orbCollection;
		
		this.query = processConstraint(criteria.logicalConstraint);
	}
	
	public List<CacheEntry> listCacheEntries() {
		
		ResultSet<CacheEntry> resultSet = this.indexedCollection.retrieve(query);
		
		List<CacheEntry> cacheEntryList = new ArrayList<CacheEntry>();
		for (CacheEntry cacheEntry : resultSet) {
			cacheEntryList.add(cacheEntry);
		}
		
		return cacheEntryList;
	}
	
	public List<Orb> list() {
		
		ResultSet<CacheEntry> resultSetCacheEntries = this.indexedCollection.retrieve(query);
		
		List<Orb> orbList = new ArrayList<Orb>(resultSetCacheEntries.size());
		
		for (CacheEntry cacheEntry : resultSetCacheEntries) {
			Orb orb = orbCollection.get(cacheEntry.getId());
			orbList.add(orb);
		}
		
		if (criteria.hasSortCriteria()) {
			
			List<CriteriaSortInfo> criteriaSortInfoList = criteria.getSortInfoList();
			List<GrinderSortInfo> grinderSortInfoList = new ArrayList<GrinderSortInfo>();
			for (CriteriaSortInfo criteriaSortInfo : criteriaSortInfoList) {
				GrinderSortInfo grinderSortInfo = new GrinderSortInfo();
				grinderSortInfo.sortDirection = criteriaSortInfo.sortDirection;
				grinderSortInfo.sortType = criteriaSortInfo.sortType;
				grinderSortInfo.sortIndex = orbTypeManager.getIndexOfAttribute(criteria.getOrbType(), criteriaSortInfo.sortAttributeName);
				grinderSortInfoList.add(grinderSortInfo);
			}
			
			OrbType orbType = orbTypeManager.getOrbType(this.orbTypeInternalId);
			OrbComparator rowComparator = new OrbComparator(grinderSortInfoList, orbType);
			Collections.sort(orbList, rowComparator);
		}
		
		return orbList;
	}
	
	private Query<CacheEntry> processConstraint(LogicalConstraint logicalConstraint) {
		List<Constraint> constraintList = logicalConstraint.constraintList;
		
		List<Query<CacheEntry>> queries = new ArrayList<Query<CacheEntry>>();
		for (Constraint constraintInner: constraintList) {
			
			Query<CacheEntry> queryLocal = null;
			if (constraintInner instanceof ConstraintDetailsSingleValue) {
				queryLocal = processConstraintDetailsSingleValue((ConstraintDetailsSingleValue) constraintInner);
			} else if (constraintInner instanceof ConstraintDetailsList) {
				queryLocal = processConstraintDetailsList((ConstraintDetailsList) constraintInner);
			} else if (constraintInner instanceof LogicalConstraint) {
				queryLocal = processConstraint((LogicalConstraint) constraintInner);
			}
			queries.add(queryLocal);
		}
		
		Query<CacheEntry> query = null;
		
		if (queries.size() == 1) {
			query = queries.get(0);
		} else {
			if (logicalConstraint.logicalOperator.equals(LogicalOperator.AND)) {
				query = and(queries);
			} else if (logicalConstraint.logicalOperator.equals(LogicalOperator.OR)) {
				query = or(queries);
			}
		}
		
		return query;
	}
	
	private Query<CacheEntry> processConstraintDetailsList(ConstraintDetailsList constraintDetailsList) {
		Query<CacheEntry> queryLocal = null;	
		
		SimpleNullableAttribute<CacheEntry, String> simpleNullableAttribute = createSimpleNullableAtttribute(constraintDetailsList);

		if (constraintDetailsList.getRelationshipOperator() == RelationshipOperator.IN) {
			queryLocal = in(simpleNullableAttribute, constraintDetailsList.operativeValueList.toArray(new String[constraintDetailsList.operativeValueList.size()]));
		}
		
		return queryLocal;
	}

	private OrbTypeManager getOrbTypeManager() {
		return Fletch22ApplicationContext.getApplicationContext().getBean(OrbTypeManager.class);
	}

	private Query<CacheEntry> processConstraintDetailsSingleValue(ConstraintDetailsSingleValue constraintDetailsSingleValue) {

		Query<CacheEntry> queryLocal = null;	
		
		SimpleNullableAttribute<CacheEntry, String> simpleNullableAttribute = createSimpleNullableAtttribute(constraintDetailsSingleValue);

		if (constraintDetailsSingleValue.getRelationshipOperator() == RelationshipOperator.EQUALS) {
			queryLocal = equal(simpleNullableAttribute, constraintDetailsSingleValue.operativeValue);
		}
		
		return queryLocal;
	}

	private SimpleNullableAttribute<CacheEntry, String> createSimpleNullableAtttribute(ConstraintDetails constraintDetails) {
		Class<? extends SimpleNullableAttribute<CacheEntry, String>> clazz = getClazzFactory(constraintDetails.getAttributeName());

		SimpleNullableAttribute<CacheEntry, String> simpleNullableAttribute = null;
		try {
			simpleNullableAttribute = (SimpleNullableAttribute<CacheEntry, String>) clazz.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return simpleNullableAttribute;
	}
	
	private Class<? extends SimpleNullableAttribute<CacheEntry, String>> getClazzFactory(String attributeName) {
		
		Class<? extends SimpleNullableAttribute<CacheEntry, String>> clazz = null;
		
		try {
			int indexOfAttribute = getOrbTypeManager().getIndexOfAttribute(orbTypeInternalId, attributeName);
			
			String compositeKey = SimpleNullableAttribute.class.getName() + "_" + String.valueOf(orbTypeInternalId) + "_" + String.valueOf(indexOfAttribute) + "_" + attributeName;
			
			if (hasBespokeAttributeClassBeenRegistered(compositeKey)) {
				clazz = (Class<? extends SimpleNullableAttribute<CacheEntry, String>>) getBespokeAttributeClass(compositeKey);
			} else {
				clazz = GeneratedCacheEntryClassFactory.getInstance(indexOfAttribute, compositeKey);
				ensureNameRegistered(compositeKey, clazz);
			}
			
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			for (StackTraceElement stackTraceElement: trace) {
				logger.info("CN: {}", stackTraceElement.getClassName());
			}
			throw new RuntimeException(e);
		}
		return clazz;
	}
	
	private boolean hasBespokeAttributeClassBeenRegistered(String key) {
		Map<String, Class<?>> map = this.constraintKitchen.previouslyBespokeAttributeClassMap;
		return map.containsKey(key);
	}
	
	private Class<?> getBespokeAttributeClass(String key) {
		Map<String, Class<?>> map = this.constraintKitchen.previouslyBespokeAttributeClassMap;
		return map.get(key);
	}
	
	private void ensureNameRegistered(String key, Class<?> clazz) {
		Map<String, Class<?>> map = this.constraintKitchen.previouslyBespokeAttributeClassMap;
		map.put(key, clazz);
	}
}
