package com.fletch22.orb.query;

import static org.junit.Assert.*

import java.util.Set;

import spock.lang.Specification

import com.fletch22.orb.OrbType
import com.fletch22.orb.cache.local.Cache
import com.fletch22.orb.cache.query.QueryCollection
import com.fletch22.orb.query.CriteriaFactory.Criteria

class QueryAttributeDeleteHandlerSpec extends Specification {

	static final String ORIGINAL_ATTRIBUTE_NAME = "foo"
	static final String ORIGINAL_QUERY_NAME = "TheGreatestQueryNameEvah"

	def 'test attribute delete in constraint'() {
		
		given:
		QueryAttributeDeleteHandler queryAttributeDeleteHandler = new QueryAttributeDeleteHandler()
		
		OrbType orbType = Mock(OrbType)
		orbType.id = 123
		
		CriteriaFactory criteriaFactory = new CriteriaFactory()
		Criteria criteria = criteriaFactory.createInstance(orbType, ORIGINAL_QUERY_NAME)
		
		ConstraintDetailsSingleValue constraintDetailSingleValue = (ConstraintDetailsSingleValue) Constraint.eq(ORIGINAL_ATTRIBUTE_NAME, "someValue")
		
		criteria.addAnd(constraintDetailSingleValue)
		
		Cache cache = Mock(Cache)
		QueryCollection queryCollection = new QueryCollection()
		cache.queryCollection = queryCollection
		
		queryCollection.add(234, criteria)
		
		queryAttributeDeleteHandler.cache = cache
		
		QueryManager queryManager = Mock(QueryManager)
		queryAttributeDeleteHandler.queryManager = queryManager
		
		when:
		queryAttributeDeleteHandler.handleAttributeDeletion(orbType.id, ORIGINAL_ATTRIBUTE_NAME, true)
		
		then:
		queryAttributeDeleteHandler
		1 * queryManager.delete(_, _)
	}

}