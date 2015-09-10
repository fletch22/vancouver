package com.fletch22.orb.query;

import static org.junit.Assert.*

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import spock.lang.Shared
import spock.lang.Specification

import com.fletch22.orb.IntegrationSystemInitializer
import com.fletch22.orb.IntegrationTests
import com.fletch22.orb.Orb
import com.fletch22.orb.OrbManager
import com.fletch22.orb.OrbTypeManager
import com.fletch22.orb.cache.local.Cache
import com.fletch22.orb.cache.local.CacheEntry
import com.fletch22.orb.query.CriteriaFactory.Criteria

@org.junit.experimental.categories.Category(IntegrationTests.class)
@ContextConfiguration(locations = "classpath:/springContext-test.xml")
class CriteriaSpec extends Specification {

	Logger logger = LoggerFactory.getLogger(CriteriaSpec)
	
	@Autowired
	Cache cache

	@Autowired
	CriteriaFactory criteriaFactory

	@Autowired
	OrbManager orbManager

	@Autowired
	OrbTypeManager orbTypeManager

	@Shared
	static final String ATTRIBUTE_COLOR = 'color'

	@Shared
	static final String COLOR_TO_FIND = 'green'

	@Autowired
	IntegrationSystemInitializer integrationSystemInitializer;

	def setup() {
		integrationSystemInitializer.nukeAndPaveAllIntegratedSystems();
	}

	def cleanup() {
		integrationSystemInitializer.nukeAndPaveAllIntegratedSystems();
	}

	def 'test criteria search for green'() {

		given:
		def orbTypeInternalId = loadTestData()

		Criteria criteria = criteriaFactory.getInstance(orbTypeInternalId)

		criteria.add(LogicalConstraint.and(Constraint.eq(ATTRIBUTE_COLOR, COLOR_TO_FIND)))

		when:
		StopWatch stopWatch = new StopWatch()
		stopWatch.start()
		List<CacheEntry> results = cache.orbCollection.executeQuery(criteria);
		stopWatch.stop()
		
		def elapsed = new BigDecimal(stopWatch.nanoTime).divide(new BigDecimal(1000000))
		logger.info("elapsed time: {}", elapsed)
		
		then:
		notThrown Exception
		results
		results.size > 0
	}
	
	def 'test criteria search or'() {
		
		given:
		def orbTypeInternalId = loadTestData()

		Criteria criteria = criteriaFactory.getInstance(orbTypeInternalId)

		criteria.add(LogicalConstraint.or(Constraint.eq(ATTRIBUTE_COLOR, "red"), LogicalConstraint.or(Constraint.eq(ATTRIBUTE_COLOR, COLOR_TO_FIND), Constraint.eq(ATTRIBUTE_COLOR, "orange"))))

		when:
		StopWatch stopWatch = new StopWatch()
		stopWatch.start()
		List<CacheEntry> results = cache.orbCollection.executeQuery(criteria);
		stopWatch.stop()
		
		def elapsed = new BigDecimal(stopWatch.nanoTime).divide(new BigDecimal(1000000))
		logger.info("elapsed time: {}", elapsed)
		
		then:
		notThrown Exception
		results
		results.size == 110
	}
	
	def 'test criteria search collection'() {
		
		given:
		def orbTypeInternalId = loadTestData()

		Criteria criteria = criteriaFactory.getInstance(orbTypeInternalId)
		
		ConstraintCollection constraintCollection = new ConstraintCollection()
		constraintCollection.constraintArray = new Constraint[3]
		constraintCollection.constraintArray[0] = Constraint.eq(ATTRIBUTE_COLOR, "red")
		constraintCollection.constraintArray[1] = Constraint.eq(ATTRIBUTE_COLOR, COLOR_TO_FIND)
		constraintCollection.constraintArray[2] = Constraint.eq(ATTRIBUTE_COLOR, "orange")
		
		criteria.add(LogicalConstraint.or(constraintCollection))

		when:
		StopWatch stopWatch = new StopWatch()
		stopWatch.start()
		List<CacheEntry> results = cache.orbCollection.executeQuery(criteria);
		stopWatch.stop()
		
		def elapsed = new BigDecimal(stopWatch.nanoTime).divide(new BigDecimal(1000000))
		logger.info("elapsed time: {}", elapsed)
		
		then:
		notThrown Exception
		results
		results.size == 110
	}

	public long loadTestData() {

		LinkedHashSet<String> customFields = new LinkedHashSet<String>()

		customFields.add(ATTRIBUTE_COLOR)
		customFields.add("size")
		customFields.add("speed")

		def orbTypeInternalId = orbTypeManager.createOrbType('foo', customFields)

		def color = 'red'
		
		def numInstances = 60
		setNumberInstancesToColor(60, orbTypeInternalId, "red")
		
		setNumberInstancesToColor(10, orbTypeInternalId, "orange")

		setNumberInstancesToColor(40, orbTypeInternalId, COLOR_TO_FIND)
		
		return orbTypeInternalId
	}

	private setNumberInstancesToColor(int numInstances, long orbTypeInternalId, color) {
		numInstances.times {
			Orb orb = orbManager.createOrb(orbTypeInternalId)
			orbManager.setAttribute(orb.orbInternalId, ATTRIBUTE_COLOR, color);
		}
	}
}
