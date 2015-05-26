package com.fletch22.orb;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class NakedToClothedOrbTransformer {

	public Orb convertNakedToClothed(NakedOrb nakedOrb) {
		
		long internalId = new Long(nakedOrb.getOrbInternalId()).longValue();
		long internalTypeId = new Long(nakedOrb.getOrbTypeInternalId()).longValue();
		
		BigDecimal tranDate = new BigDecimal(nakedOrb.getTranDate());
		
		return new Orb(internalId, internalTypeId, tranDate, nakedOrb.getUserDefinedProperties());
	}
}
