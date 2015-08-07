package com.fletch22.orb.cache.external;

import java.math.BigDecimal;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.fletch22.orb.InternalIdGenerator;
import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbManager;
import com.fletch22.orb.command.orb.DeleteOrbCommand;
import com.fletch22.orb.command.orbType.dto.AddOrbDto;
import com.fletch22.orb.rollback.UndoActionBundle;

//@Component(value = "OrbManagerExternalCache")
public class OrbManagerExternalCache implements OrbManager {
	
	@Autowired
	InternalIdGenerator internalIdGenerator;
	
	@Autowired
	ObjectInstanceExternalCacheService objectInstanceCacheService;
	
	@Autowired
	NakedToClothedOrbTransformer nakedToClothedOrbTransformer;
	
	@Autowired
	DeleteOrbCommand deleteOrbCommand;

	public Orb createOrbInstance(AddOrbDto addOrbDto, BigDecimal tranDate, UndoActionBundle undoActionBundle) {
		long orbInternalId = this.internalIdGenerator.getNewId();
		
		NakedOrb nakedOrb = new NakedOrb(orbInternalId, addOrbDto.orbTypeInternalId, tranDate);
		
		this.objectInstanceCacheService.createInstance(nakedOrb);
		
		// Add delete to rollback action
		undoActionBundle.addUndoAction(this.deleteOrbCommand.toJson(orbInternalId, false), tranDate);
		
		return nakedToClothedOrbTransformer.convertNakedToClothed(nakedOrb);
	}

	@Override
	public void deleteAllOrbInstances() {
		throw new NotImplementedException("deleteAllOrbInstances");
	}
}