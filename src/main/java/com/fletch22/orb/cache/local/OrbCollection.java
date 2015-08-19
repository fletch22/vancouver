package com.fletch22.orb.cache.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbType;

public class OrbCollection {
	
	Logger logger = LoggerFactory.getLogger(OrbCollection.class);

	Map<Long, OrbSingleTypesInstanceCollection> allInstances = new HashMap<Long, OrbSingleTypesInstanceCollection>();
	private Map<Long, Orb> quickLookup = new HashMap<Long, Orb>();
	
	public void add(OrbType orbType, Orb orb) {
		OrbSingleTypesInstanceCollection orbSingleTypesInstanceCollection = allInstances.get(orb.getOrbTypeInternalId());
		
		if (orbSingleTypesInstanceCollection == null) {
			logger.info("Type not found in single type instance collection. Creating new single type instance collection.");
			orbSingleTypesInstanceCollection = new OrbSingleTypesInstanceCollection(orbType.id);
			allInstances.put(orbType.id, orbSingleTypesInstanceCollection);
		}
		
		ArrayList<String> fields = getPropertyValuesInOrder(orbType, orb);
		orbSingleTypesInstanceCollection.addInstance(orb.getOrbInternalId(), null, orb.getTranDate(), fields);
		
		quickLookup.put(orb.getOrbInternalId(), orb);
	}
	
	private ArrayList<String> getPropertyValuesInOrder(OrbType orbType, Orb orb) {
		Map<String, String> properties = orb.getUserDefinedProperties();
		ArrayList<String> fieldValues = new ArrayList<String>();
		for (String field : orbType.customFields) {
			fieldValues.add(properties.get(field));
		}
		
		return fieldValues;
	}

	public void deleteAll() {
		allInstances.clear();
		quickLookup.clear();
	}

	public Orb get(long orbInternalId) {
		Orb orb = quickLookup.get(orbInternalId);
		
		if (orb == null) throw new RuntimeException("Encountered problem getting orb. Couldn't find orb with id '" + orbInternalId + "'.");
		
		return orb;
	}
	
	public Orb delete(long orbInternalId) {
		Orb orb = quickLookup.get(orbInternalId);
		
		OrbSingleTypesInstanceCollection orbSingleTypesInstanceCollection = allInstances.get(orb.getOrbTypeInternalId());
		orbSingleTypesInstanceCollection.removeInstance(orb.getOrbInternalId());
		
		quickLookup.remove(orbInternalId);
		
		return orb;
	}
	
	public void addAttribute(long orbTypeInternalId, String name) {
		
		if (allInstances.containsKey(orbTypeInternalId)) {
			OrbSingleTypesInstanceCollection orbSingleTypesInstanceCollection = allInstances.get(orbTypeInternalId);
			for (CacheEntry cacheEntry:  orbSingleTypesInstanceCollection.instances) {
				Orb orb = quickLookup.get(cacheEntry.id);
				orb.getUserDefinedProperties().put(name, null);
				
				cacheEntry.attributes.add(null);
			}
		}
	}
	
	public void removeAttribute(long orbTypeInternalId, int indexOfAttribute, String name) {
		
		OrbSingleTypesInstanceCollection orbSingleTypesInstanceCollection = allInstances.get(orbTypeInternalId);
		
		for (CacheEntry cacheEntry:  orbSingleTypesInstanceCollection.instances) {
			Orb orb = quickLookup.get(cacheEntry.id);
			orb.getUserDefinedProperties().remove(name);
			cacheEntry.attributes.remove(indexOfAttribute);
		}
	}
	
	public Map<Long, Orb> getQuickLookup() {
		return quickLookup;
	}

	public boolean doesOrbExist(long orbInternalId) {
		return quickLookup.containsKey(orbInternalId);
	}

	public int getCount() {
		return quickLookup.size();
	}
}
