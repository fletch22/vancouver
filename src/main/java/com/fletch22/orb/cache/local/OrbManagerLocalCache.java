package com.fletch22.orb.cache.local;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.aop.Log4EventAspect;
import com.fletch22.aop.Loggable4Event;
import com.fletch22.orb.InternalIdGenerator;
import com.fletch22.orb.Orb;
import com.fletch22.orb.OrbCloner;
import com.fletch22.orb.OrbManager;
import com.fletch22.orb.OrbType;
import com.fletch22.orb.OrbTypeManager;
import com.fletch22.orb.TranDateGenerator;
import com.fletch22.orb.command.orb.DeleteOrbCommand;
import com.fletch22.orb.command.orbType.dto.AddOrbDto;
import com.fletch22.orb.dependency.DependencyHandler;
import com.fletch22.orb.dependency.DependencyHandlerEngine;
import com.fletch22.orb.dependency.DependencyHandlerFactory;
import com.fletch22.orb.query.QueryManager;
import com.fletch22.orb.rollback.UndoActionBundle;
import com.fletch22.util.json.MapLongString;

@Component(value = "OrbManagerLocalCache")
public class OrbManagerLocalCache implements OrbManager {

	Logger logger = LoggerFactory.getLogger(OrbManagerLocalCache.class);

	@Autowired
	InternalIdGenerator internalIdGenerator;

	@Autowired
	Cache cache;

	@Autowired
	DeleteOrbCommand deleteOrbCommand;

	@Autowired
	OrbTypeManager orbTypeManager;
	
	@Autowired
	OrbCloner orbCloner;
	
	@Autowired
	TranDateGenerator tranDateGenerator;
	
	@Autowired
	QueryManager queryManager;
	
	@Autowired
	DependencyHandlerFactory dependencyHandlerFactory;

	@Override
	@Loggable4Event
	public Orb createOrb(Orb orb) {

		if (orb.getOrbInternalId() == Orb.INTERNAL_ID_UNSET) {
			orb.setOrbInternalId(this.internalIdGenerator.getNewId());
		}

		OrbType orbType = orbTypeManager.getOrbType(orb.getOrbTypeInternalId());

		populateOrbMap(orbType, orb);

		cache.orbCollection.add(orbType, orb);

		Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
		deleteOrb(orb.getOrbInternalId(), true);
		
		return orb;
	}

	@Override
	@Loggable4Event
	public Orb createOrb(OrbType orbType, BigDecimal tranDate) {

		long orbInternalId = this.internalIdGenerator.getNewId();
		Orb orb = new Orb(orbInternalId, orbType.id, tranDate, new LinkedHashMap<String, String>());

		populateOrbMap(orbType, orb);

		cache.orbCollection.add(orbType, orb);

		Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
		deleteOrb(orb.getOrbInternalId(), true);

		return orb;
	}

	@Override
	public Orb createOrb(long orbTypeInternalId, BigDecimal tranDate) {

		long orbInternalId = this.internalIdGenerator.getNewId();
		Orb orb = new Orb(orbInternalId, orbTypeInternalId, tranDate, new LinkedHashMap<String, String>());

		createOrb(orb);

		return orb;
	}
	
	@Override
	public Orb createOrb(long orbTypeInternalId) {

		BigDecimal tranDate = tranDateGenerator.getTranDate();

		return createOrb(orbTypeInternalId, tranDate);
	}

	@Override
	public Orb createOrb(AddOrbDto addOrbDto, BigDecimal tranDate, UndoActionBundle undoActionBundle) {

		long orbInternalId = this.internalIdGenerator.getNewId();
		Orb orb = new Orb(orbInternalId, addOrbDto.orbTypeInternalId, tranDate, new LinkedHashMap<String, String>());
		OrbType orbType = orbTypeManager.getOrbType(orb.getOrbTypeInternalId());

		populateOrbMap(orbType, orb);

		cache.orbCollection.add(orbType, orb);

		// Add delete to rollback action
		undoActionBundle.addUndoAction(this.deleteOrbCommand.toJson(orbInternalId, false), tranDate);

		return orb;
	}

	private void populateOrbMap(OrbType orbType, Orb orb) {

		LinkedHashMap<String, String> propertyMap = orb.getUserDefinedProperties();
		LinkedHashSet<String> customFields = orbType.customFields;
		
		for (String field : customFields) {
			if (!propertyMap.containsKey(field)) {
				propertyMap.put(field, null);
			}
		}
	}

	@Override
	@Loggable4Event
	public Orb deleteOrb(long orbInternalId, boolean isDeleteDependencies) {
		
		OrbCollection orbCollection = cache.orbCollection;
		
		Orb orb = orbCollection.get(orbInternalId);
		
		Orb orbCopy = orbCloner.cloneOrb(orb);
		
		OrbType orbType = orbTypeManager.getOrbType(orb.getOrbTypeInternalId());
		
		handleDependenciesForOrbDeletion(orb, isDeleteDependencies);
		
		// Process references inside of orb here.
		cache.orbCollection.delete(orbType, orbInternalId);

		Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
		createOrb(orbCopy);
		
		return orbCopy;
	}
	
	@Override
	@Loggable4Event
	public Orb deleteOrbIgnoreQueryDependencies(long orbInternalId, boolean isDeleteDependencies) {
		
		OrbCollection orbCollection = cache.orbCollection;
		
		Orb orb = orbCollection.get(orbInternalId);
		
		Orb orbCopy = orbCloner.cloneOrb(orb);
		
		OrbType orbType = orbTypeManager.getOrbType(orb.getOrbTypeInternalId());
		
		handleOrbReferenceDependenciesForOrbDeletion(orb, isDeleteDependencies);
		
		// Process references inside of orb here.
		cache.orbCollection.delete(orbType, orbInternalId);

		Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
		createOrb(orbCopy);
		
		return orbCopy;
	}
	
	private void handleDependenciesForOrbDeletion(Orb orb, boolean isDeleteDependencies) {
		handleQueryDependenciesForOrbDeletion(orb, isDeleteDependencies);
		handleOrbReferenceDependenciesForOrbDeletion(orb, isDeleteDependencies);
	}
	
	private void handleDependenciesForOrbDeletion2(Orb orb, boolean isDeleteDependencies) {
		DependencyHandler handler1 = dependencyHandlerFactory.getOrbDeletionForQueryInstance(orb, isDeleteDependencies);
		DependencyHandler handler2 = dependencyHandlerFactory.getOrbDeltionForReferencesInstance(orb, isDeleteDependencies);
		
		DependencyHandlerEngine dependencyHandlerEngine = new DependencyHandlerEngine();
		dependencyHandlerEngine.addHandler(handler1);
		dependencyHandlerEngine.addHandler(handler2);
		
		dependencyHandlerEngine.check();
	}

	private void handleQueryDependenciesForOrbDeletion(Orb orb, boolean isDeleteDependencies) {
		long orbInternalId = orb.getOrbInternalId();
		if (isDeleteDependencies) {
			queryManager.removeQueryFromCollection(orbInternalId);
		} else {
			boolean doesExist = queryManager.doesQueryExist(orbInternalId);
			if (doesExist) {
				String message = String.format("Encountered problem deleting orb '%s'. Orb has at least one dependency. A query exists that depends on the orb.", orbInternalId);
				throw new RuntimeException(message);
			}
		}
	}
	
	public void handleOrbReferenceDependenciesForOrbDeletion(Orb orb, boolean isDeleteDependencies) {
		
		if (isDeleteDependencies) {
			resetAllReferencesPointingToOrb(orb);
		} else {
			long orbInternalId = orb.getOrbInternalId();
			boolean doesExist = this.cache.orbCollection.doesReferenceToOrbExist(orb);
			if (doesExist) {
				String message = String.format("Encountered problem deleting orb '%s'. Orb has at least one dependency. Specify that dependencies should be deleted automatically by passing 'true' for 'isDeleteDependencies'.", orbInternalId);
				throw new RuntimeException(message);
			}
		}
	}

	@Override
	public void resetAllReferencesPointingToOrb(Orb orb) {
		
		OrbCollection orbCollection = cache.orbCollection;
		Map<Long, List<String>> attributeReferenceMap = orbCollection.getReferencesToOrb(orb);

		Set<Long> orbInternalIdSet = attributeReferenceMap.keySet();
		for (long orbInternalId : orbInternalIdSet) {
			List<String> attributeNameList = attributeReferenceMap.get(orbInternalId);
			for (String attributeArrow: attributeNameList) {
				setAttribute(orbInternalId, attributeArrow, null);
			}
		}
	}

	@Override
	public String getAttribute(long orbInternalId, String attributeName) {

		Orb orb = cache.orbCollection.get(orbInternalId);

		return orb.getUserDefinedProperties().get(attributeName);
	}

	@Override
	@Loggable4Event
	public void addAttributeAndValueToInstances(MapLongString longStringMap, long orbTypeInternalId, int indexAttribute, String attributeName) {
		
		cache.orbCollection.addAttributeValues(longStringMap.map, orbTypeInternalId, indexAttribute, attributeName);

		Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
		deleteOrbAttributeFromAllInstances(orbTypeInternalId, attributeName, indexAttribute);
	}

	@Override
	@Loggable4Event
	public void deleteOrbAttributeFromAllInstances(long orbTypeInternalId, String attributeName, int attributeIndex) {

		Map<Long, String> mapDeleted = cache.orbCollection.removeAttribute(orbTypeInternalId, attributeIndex, attributeName);
		
		Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
		addAttributeAndValueToInstances(new MapLongString(mapDeleted), orbTypeInternalId, attributeIndex, attributeName);
	}

	@Override
	@Loggable4Event
	public void setAttribute(long orbInternalId, String attributeName, String value) {
		
		String oldValue = cache.orbCollection.setAttribute(orbInternalId, attributeName, value);
		
		if (!areEqualAttributes(oldValue, value)) {
			Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
			setAttribute(orbInternalId, attributeName, oldValue);
		}
	}

//	public void addReference(long orbInternalIdArrow, String attributeNameArrow, long orbInternalIdTarget, String attributeNameTarget) {
//
//		Orb orb = cache.orbCollection.get(orbInternalIdArrow);
//
//		String oldValue = orb.getUserDefinedProperties().get(attributeNameArrow);
//
//		if (StringUtils.isEmpty(oldValue) || cache.orbCollection.orbReference.isValueAReference(oldValue)) {
//			String newValue = cache.orbCollection.orbReference.addReference(orbInternalIdArrow, attributeNameArrow, oldValue, orbInternalIdTarget, attributeNameTarget);
//
//			if (!oldValue.equals(newValue)) {
//				orb.getUserDefinedProperties().put(attributeNameArrow, newValue);
//
//				Log4EventAspect.preventNextLineFromExecutingAndLogTheUndoAction();
//				setAttribute(orbInternalIdArrow, attributeNameArrow, oldValue);
//			}
//		} else {
//			throw new RuntimeException(String.valueOf(orbInternalIdArrow) + "'s original value '" + oldValue + "' is not a reference.");
//		}
//	}

	private boolean areEqualAttributes(String value1, String value2) {
		return (value1 == null ? value2 == null : value1.equals(value2));
	}

	@Override
	public Orb getOrb(long orbInternalId) {
		Orb orb = cache.orbCollection.get(orbInternalId);

		if (orb == null) {
			throw new RuntimeException("Encountered problem getting orb. Couldn't find orb with id '" + orbInternalId + "'.");
		}

		return orb;
	}
	
	@Override
	public List<Orb> getOrbsOfType(long orbTypeInternalId) {
		return cache.orbCollection.getOrbsWithType(orbTypeInternalId);
	}

	@Override
	public void nukeAllOrbs() {
		cache.orbCollection.deleteAll();
	}

	@Override
	public boolean doesOrbExist(long orbInternalId) {
		return cache.orbCollection.doesOrbExist(orbInternalId);
	}

	@Override
	public void deleteOrbsWithType(long orbTypeInternalId, boolean isDeleteDependencies) {
		List<Orb> orbsWithType = cache.orbCollection.getOrbsWithType(orbTypeInternalId);
		
		for (Orb orb : orbsWithType) {
			deleteOrb(orb.getOrbInternalId(), isDeleteDependencies);
		}
	}

	@Override
	public void renameAttribute(long orbTypeInternalId, String attributeNameOld, String attributeNameNew) {
		cache.orbCollection.renameAttribute(orbTypeInternalId, attributeNameOld, attributeNameNew);
	}

	@Override
	public long countOrbsOfType(long orbTypeInternalId) {
		return cache.orbCollection.getCountOrbsOfType(orbTypeInternalId);
	}

	@Override
	public boolean doesOrbWithTypeExist(long orbTypeInternalId) {
		return countOrbsOfType(orbTypeInternalId) > 0;
	}
}
