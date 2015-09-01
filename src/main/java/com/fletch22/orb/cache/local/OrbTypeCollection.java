package com.fletch22.orb.cache.local;

import static com.googlecode.cqengine.query.QueryFactory.equal;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fletch22.orb.OrbType;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.unique.UniqueIndex;
import com.googlecode.cqengine.query.Query;

@Component
public class OrbTypeCollection {
	
	Logger logger = LoggerFactory.getLogger(OrbTypeCollection.class);

	private IndexedCollection<OrbType> types = new ConcurrentIndexedCollection<OrbType>();
	private Map<Long, OrbType> quickLookup = new HashMap<Long, OrbType>();

	public OrbTypeCollection() {
		types.addIndex(UniqueIndex.onAttribute(OrbType.ID));
		types.addIndex(UniqueIndex.onAttribute(OrbType.LABEL));
	}

	public void add(OrbType orbType) {
		logger.debug("Adding orb type with id: {}", orbType.id);
		this.quickLookup.put(orbType.id, orbType);
		this.types.add(orbType);
	}

	public OrbType remove(long id) {
		OrbType orbType = this.quickLookup.remove(id);
		boolean wasFound = types.remove(orbType);
		
		if (!wasFound) {
			throw new RuntimeException("Encountered problem removing type from type collection. Orb type not found.");
		}
		return orbType;
	}

	public OrbType get(long id) {
		return this.quickLookup.get(id);
	}

	public boolean doesTypeWithLabelExist(String label) {
		Query<OrbType> query = equal(OrbType.LABEL, label);
		return types.retrieve(query).isEmpty();
	}
	
	public void deleteAll() {
		this.quickLookup.clear();
		this.types.clear();
	}

	public int getCount()  {
		return quickLookup.size();
	}
	
	public Map<Long, OrbType> getQuickLookup() {
		return this.quickLookup;
	}
}
