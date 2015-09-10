package com.fletch22.orb.cache.local;

import static org.junit.Assert.*

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.StopWatch
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

import com.fletch22.orb.IntegrationSystemInitializer
import com.fletch22.orb.IntegrationTests
import com.fletch22.orb.Orb
import com.fletch22.orb.OrbManager
import com.fletch22.orb.OrbType
import com.fletch22.orb.OrbTypeManager
import com.fletch22.orb.TranDateGenerator
import com.fletch22.orb.client.service.BeginTransactionService
import com.fletch22.orb.client.service.RollbackTransactionService
import com.fletch22.util.RandomUtil

@org.junit.experimental.categories.Category(IntegrationTests.class)
@ContextConfiguration(locations = ['classpath:/springContext-test.xml'])
class OrbManagerLocalCacheSpec extends Specification {

	Logger logger = LoggerFactory.getLogger(OrbManagerLocalCacheSpec)

	@Autowired
	OrbTypeManager orbTypeManager

	@Autowired
	OrbManager orbManager

	@Autowired
	IntegrationSystemInitializer integrationSystemInitializer

	@Autowired
	RandomUtil randomUtil

	@Autowired
	OrbReference orbReference
	
	@Autowired
	TranDateGenerator tranDateGenerator
	
	@Autowired
	Cache cache
	
	@Autowired
	CacheCloner cacheCloner
	
	@Autowired
	BeginTransactionService beginTransactionService
	
	@Autowired
	RollbackTransactionService rollbackTransactionService
	
	@Autowired
	CacheComponentComparator cacheComponentComparator

	def setup()  {
		integrationSystemInitializer.nukeAndPaveAllIntegratedSystems()
	}

	def tearDown() {
		integrationSystemInitializer.nukeAndPaveAllIntegratedSystems()
	}

	@Test
	def 'testAddIllegalAttributeValue'() {

		given:
		long orbTypeInternalId = orbTypeManager.createOrbType("foop", new LinkedHashSet<String>())

		String attributeName = "foo"
		orbTypeManager.addAttribute(orbTypeInternalId, attributeName)

		OrbType orbType = orbTypeManager.getOrbType(orbTypeInternalId)

		String tranDateString = orbType.tranDate.toString()
		BigDecimal tranDate = new BigDecimal(tranDateString)

		Orb orb = orbManager.createOrb(orbTypeInternalId, tranDate)

		when:
		orbManager.setAttribute(orb.getOrbInternalId(), attributeName, ReferenceCollection.REFERENCE_KEY_PREFIX)

		then:
		final Exception exception = thrown()
	}

	@Test
	def 'test delete instance no references'() {

		given:
		long orbTypeInternalId = orbTypeManager.createOrbType("foop", new LinkedHashSet<String>())

		String attributeName = "foo"
		orbTypeManager.addAttribute(orbTypeInternalId, attributeName)

		OrbType orbType = orbTypeManager.getOrbType(orbTypeInternalId)

		String tranDateString = orbType.tranDate.toString()
		BigDecimal tranDate = new BigDecimal(tranDateString)

		Orb orb = orbManager.createOrb(orbTypeInternalId, tranDate)

		orbManager.setAttribute(orb.getOrbInternalId(), attributeName, "bar")

		when:
		orbManager.deleteOrb(orb.orbInternalId)

		then:
		!orbManager.doesOrbExist(orb.orbInternalId);
	}
	
	@Test
	def 'test delete instance with references'() {

		given:
		def tranDate = beginTransactionService.beginTransaction()
		
		def original = cacheCloner.clone(cache.cacheComponentsDto)
		
		long orbTypeInternalId = orbTypeManager.createOrbType("foop", new LinkedHashSet<String>())

		String attributeName = "foo"
		orbTypeManager.addAttribute(orbTypeInternalId, attributeName)

		OrbType orbType = orbTypeManager.getOrbType(orbTypeInternalId)

		String referenceValue = createReferences(orbType, 100)

		Orb orb = orbManager.createOrb(orbTypeInternalId, tranDateGenerator.getTranDate())
		orbManager.setAttribute(orb.getOrbInternalId(), attributeName, referenceValue)
		
		orbManager.deleteOrb(orb.orbInternalId)
		
		rollbackTransactionService.rollbackToSpecificTransaction(tranDate)
		
		when:
		def rolledBack = cacheCloner.clone(cache.cacheComponentsDto)

		then:
		cacheComponentComparator.areSame(original, rolledBack)
	}
	
	def createReferences(OrbType orbType, int numberOfReference) {
		
		List<String> referenceList = []
		numberOfReference.times {
		Orb orb = orbManager.createOrb(orbType.id, tranDateGenerator.getTranDate())
		orbManager.setAttribute(orb.orbInternalId, "foo", "bar")
			referenceList << orbReference.composeReference(orb.orbInternalId, "foo")
		}
		
		return referenceList
	}

	def testSetReferenceAttribute() {

		given:
		long orbTypeInternalId = orbTypeManager.createOrbType("foop", new LinkedHashSet<String>())

		String attributeName = "foo"
		orbTypeManager.addAttribute(orbTypeInternalId, attributeName)

		OrbType orbType = orbTypeManager.getOrbType(orbTypeInternalId)

		String tranDateString = orbType.tranDate.toString()
		BigDecimal tranDate = new BigDecimal(tranDateString)
		
		Orb orb = orbManager.createOrb(orbTypeInternalId, tranDate)

		StopWatch stopWatch = new StopWatch()
 
		stopWatch.start()
		def numberOfReferences = 100
		def set1 = getSet(numberOfReferences)
		def set2 = getSet(numberOfReferences)
		stopWatch.stop()
		
		def elapsedMillis = new BigDecimal(stopWatch.getNanoTime()).divide(1000000)
		
		logger.info("Time to get 2 * 2 * {} orbs/orb types: {}", numberOfReferences, elapsedMillis)
		
		stopWatch.reset()

		def numberSetActions = 100

		stopWatch.start()
		def index = 0
		numberSetActions.times {
			index++
			def setToUse = index % 2 > 0 ? set1: set2 
			
			orbManager.setAttribute(orb.getOrbInternalId(), attributeName, setToUse)
		}
		stopWatch.stop()

		def elapsedMillisPerSetAction = new BigDecimal(stopWatch.getNanoTime()).divide(1000000).divide(numberSetActions)

		when:
		logger.info("Each set action averaged {} millis", elapsedMillisPerSetAction)
		def fetchedValue = orbManager.getAttribute(orb.orbInternalId, attributeName)

		then:
		fetchedValue == set1 ? true: fetchedValue == set2 ? true: false 
	}
	
	def getSet(int numberOfReferences) {
		Set<String> set = new HashSet<String>()
		numberOfReferences.times {
			def orbInternalId = randomUtil.getRandom(1, 1000)
			def attributeName = randomUtil.getRandomString(10)
			
			LinkedHashSet<String> fieldSet = new LinkedHashSet<String>()
			fieldSet.add(attributeName)
			
			def orbTypeInternalId = orbTypeManager.createOrbType(attributeName, fieldSet)
			
			def tranDate = tranDateGenerator.getTranDate()
			Orb orb = orbManager.createOrb(orbTypeInternalId, tranDate)
			
			def composedKey = orbReference.composeReference(orb.getOrbInternalId(), attributeName)
			set.add(composedKey)
		}

		return StringUtils.join(set, ',');
	}
}