package com.fletch22.app.state;

import static org.junit.Assert.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

import com.fletch22.app.designer.AppDesignerModule
import com.fletch22.orb.IntegrationSystemInitializer
import com.fletch22.orb.Orb
import com.fletch22.orb.OrbManager
import com.fletch22.orb.OrbType
import com.fletch22.orb.OrbTypeManager
import com.fletch22.orb.command.transaction.TransactionService
import com.fletch22.orb.query.QueryManager

@ContextConfiguration(locations = 'classpath:/springContext-test.xml')
class FrontEndStateServiceSpec extends Specification {
	
	Logger logger = LoggerFactory.getLogger(FrontEndStateServiceSpec);

	@Autowired
	FrontEndStateService frontEndStateService;
	
	@Autowired
	QueryManager queryManager;
	
	@Autowired
	OrbTypeManager orbTypeManager;
	
	@Autowired
	OrbManager orbManager;
	
	@Autowired
	IntegrationSystemInitializer initializer
	
	@Autowired
	AppDesignerModule appDesignerModule
	
	@Autowired
	TransactionService transactionService;
	
	def setup() {
		initializer.addOrbSystemModule(appDesignerModule)
		initializer.nukeAndPaveAllIntegratedSystems()
	}
	
	def cleanup() {
		initializer.removeOrbSystemModules()
		initializer.nukeAndPaveAllIntegratedSystems()
	}
	
	def 'test save'() {
		
		given:
		String state = "this is a test";
				
		when:
		frontEndStateService.save(state);
		
		OrbType orbType = orbTypeManager.getOrbType(FrontEndState.TYPE_LABEL);
		
		logger.debug("OrbType ID: {}", orbType.id);
		
		List<Orb> orbs = orbManager.getOrbsOfType(orbType.id);
		
		then:
		orbs.size() == 1;
	}

}
