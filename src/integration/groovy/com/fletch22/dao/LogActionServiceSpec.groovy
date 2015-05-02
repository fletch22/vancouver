package com.fletch22.dao;

import static org.junit.Assert.*

import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import com.fletch22.orb.IntegrationSystemInitializer
import com.fletch22.orb.IntegrationTests
import com.fletch22.orb.TranDateGenerator
import com.fletch22.orb.command.ActionSniffer
import com.fletch22.orb.command.CommandBundle
import com.fletch22.orb.command.orbType.AddOrbTypeCommand
import com.fletch22.orb.command.processor.CommandProcessActionPackageFactory
import com.fletch22.orb.command.processor.CommandProcessor
import com.fletch22.orb.command.processor.OperationResult
import com.fletch22.orb.command.processor.CommandProcessActionPackageFactory.CommandProcessActionPackage
import com.fletch22.orb.command.transaction.TransactionService
import com.fletch22.orb.rollback.UndoAction
import com.fletch22.orb.rollback.UndoActionBundle
import com.fletch22.orb.service.OrbTypeService

@org.junit.experimental.categories.Category(IntegrationTests.class)
@ContextConfiguration(locations = ['classpath:/springContext-test.xml'])
class LogActionServiceSpec extends Specification {
	
	@Shared Logger logger = LoggerFactory.getLogger(LogActionServiceSpec)
	
	@Autowired
	IntegrationSystemInitializer initializer
	
	@Autowired
	LogActionService logActionService
	
	@Autowired
	AddOrbTypeCommand addOrbTypeCommand
	
	@Autowired
	CommandProcessor commandProcessor
	
	@Autowired
	CommandProcessActionPackageFactory commandProcessActionPackageFactory;
	
	@Autowired
	ActionSniffer actionSniffer;
	
	@Autowired
	TransactionService transactionService;
	
	@Autowired
	TranDateGenerator tranDateGenerator;
	
	@Autowired
	LogActionDao logActionDao;
	
	@Autowired
	OrbTypeService orbTypeService;
	
	def setup() {
		initializer.nukeAndPaveAllIntegratedSystems()
	}

	@Unroll
	@Test
	def 'test get result set from CommandBundle'() {
		
		given:
		setup()
		
		CommandBundle commandBundle = new CommandBundle();
		
		CommandProcessActionPackage commandProcessActionPackage = null
		for (i in 0..numberOfAdds.intValue()) {
			def json = addOrbTypeCommand.toJson('foo')
			commandBundle.addCommand(json);
		}
		
		commandProcessActionPackage = commandProcessActionPackageFactory.getInstance(commandBundle.toJson())
		OperationResult operationResult = this.commandProcessor.processAction(commandProcessActionPackage)
		
		when:
		List<UndoActionBundle> undoActionBundleList = logActionService.getUndoActions(commandProcessActionPackage.getTranId().longValue())
		
		then:
		undoActionBundleList
		undoActionBundleList.size() == 1
		UndoActionBundle undoActionBundle = undoActionBundleList.get(0)
		
		logger.info(undoActionBundle.toJson().toString());
		
		where:
		numberOfAdds << [1, 5000]
	}
	
	@Unroll
	@Test
	def 'test get result set from multiple actions same tran date'() {
		
		given:
		setup()
		
		BigDecimal tranId;
		
		CommandProcessActionPackage commandProcessActionPackage = null
		commandProcessActionPackage = insertTypes(numberOfAdds, commandProcessActionPackage)
		
		when:
		List<UndoActionBundle> undoActionBundleList = logActionService.getUndoActions(commandProcessActionPackage.getTranId().longValue())
		
		then:
		undoActionBundleList
		UndoActionBundle undoActionBundle = undoActionBundleList.get(0)
		
		long lastTranDate = 0
		def isFirst = true
		while (!undoActionBundle.getActions().empty()) {
			UndoAction undoAction = undoActionBundle.getActions().pop()
			if (!isFirst) {
				assertTrue(lastTranDate > undoAction.tranDate.longValue())
			}
			isFirst = false
			lastTranDate = undoAction.tranDate.longValue()
			
			UndoActionBundle.fromJson(new StringBuilder(undoAction.action));
			
			logger.info(undoAction.action.toString());
		}
		
		where:
		numberOfAdds << [1, 5, 10]
	}
	
	@Test
	def 'testThrowException if new transaction started'() {
		
		given:
		setup()
		
		this.logActionDao.recordTransactionStart(this.transactionService.generateTranId());
		
		when:
		this.transactionService.beginTransaction(123);
		
		then:
		thrown Exception
	}
		
	private CommandProcessActionPackage insertTypes(Integer numberOfAdds, CommandProcessActionPackage commandProcessActionPackage) {
		BigDecimal tranId;
		for (i in 1..numberOfAdds.intValue()) {
			def json = addOrbTypeCommand.toJson('foo' + i)
			
			CommandBundle commandBundle = new CommandBundle()
			
			commandBundle.addCommand(json);
			commandProcessActionPackage = commandProcessActionPackageFactory.getInstance(commandBundle.toJson())
			if (null != tranId) {
				commandProcessActionPackage.setTranId(tranId)
			}
			
			OperationResult operationResult = this.commandProcessor.processAction(commandProcessActionPackage)
			
			if (null == tranId) {
				tranId = commandProcessActionPackage.getTranId()
			}
		}
		
		return commandProcessActionPackage
	}
}