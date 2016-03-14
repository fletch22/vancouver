package com.fletch22.app.state;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbManager;
import com.fletch22.orb.OrbType;
import com.fletch22.orb.OrbTypeManager;
import com.fletch22.orb.command.transaction.TransactionService;

@Component
public class FrontEndStateDao {
	
	Logger logger = LoggerFactory.getLogger(FrontEndStateDao.class);

	@Autowired
	OrbTypeManager orbTypeManager;
	
	@Autowired
	OrbManager orbManager;
	
	@Autowired
	TransactionService transactionService;
	
	public void save(String state) {
		OrbType orbType = this.orbTypeManager.getOrbType(FrontEndState.TYPE_LABEL);
		
		Orb orb = orbManager.createUnsavedInitializedOrb(orbType);
		
		orb.getUserDefinedProperties().put(FrontEndState.ATTR_STATE, state);
		
		BigDecimal currentTransactionId = transactionService.getCurrentTransactionId();
		
		logger.debug("Current tranid: {} for type: {}", currentTransactionId, orbType.id);
		
		if (currentTransactionId == TransactionService.NO_TRANSACTION_IN_FLIGHT) {
			throw new RuntimeException("Attempted to save fron end state without a transaction number. This is not allowed. Wrap the call in a transaction.");
		}
		
		orb.getUserDefinedProperties().put(FrontEndState.ATTR_ASSOCIATED_TRANSACTION_ID, String.valueOf(currentTransactionId));
		
		orbManager.createOrb(orb);
	}
}
