package com.fletch22.orb.cache.local;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.orb.InternalIdGenerator;
import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbManager;
import com.fletch22.orb.command.orb.DeleteOrbCommand;
import com.fletch22.orb.command.orbType.dto.AddOrbDto;
import com.fletch22.orb.rollback.UndoActionBundle;

@Component(value = "OrbManagerLocalCache")
public class OrbManagerLocalCache implements OrbManager {
	
	Logger logger = LoggerFactory.getLogger(OrbManagerLocalCache.class);
	
	@Autowired
	InternalIdGenerator internalIdGenerator;
	
	@Autowired
	Cache cache;
	
	@Autowired
	DeleteOrbCommand deleteOrbCommand;
	
	public Orb createOrbInstance(AddOrbDto addOrbDto, BigDecimal tranDate, UndoActionBundle undoActionBundle) {
		
		long orbInternalId = this.internalIdGenerator.getNewId();
		
		Orb orb = cache.orbCollection.add(orbInternalId, addOrbDto.orbTypeInternalId, tranDate);
		
		// Add delete to rollback action
		undoActionBundle.addUndoAction(this.deleteOrbCommand.toJson(orbInternalId, false), tranDate);
		
		return orb;
	}

	@Override
	public void deleteAllOrbInstances() {
		cache.orbCollection.deleteAll();
	}
}