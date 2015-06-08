package com.fletch22.orb.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.dao.LogActionService;
import com.fletch22.orb.cache.local.CacheService;
import com.fletch22.orb.command.processor.OperationResult;
import com.fletch22.orb.command.transaction.TransactionService;

@Component
public class Initialization {
	
	Logger logger = LoggerFactory.getLogger(Initialization.class);

	@Autowired
	LogActionService logActionService;
	
	@Autowired
	TransactionService transactionService;
	
	@Autowired
	CacheService cacheService;
	
	public OperationResult initializeSystem() {
		OperationResult operationResult = OperationResult.FAILURE;
		
		nukePaveAndRepopulate();
		if (transactionService.doesDatabaseHaveAnExpiredTransaction()) {
			logger.info("Database has an expired transaction. Will rollback and repopulate cache.");
			transactionService.rollbackCurrentTransaction();
		} else {
			operationResult = OperationResult.SUCCESS;
		}
		
		return operationResult;
	}

	private void nukePaveAndRepopulate() {
		
		cacheService.clearAllItemsFromCache();
		
		logActionService.loadCacheFromDb();
	}
}
