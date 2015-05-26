package com.fletch22.redis;

import org.springframework.stereotype.Component;

@Component
public class ObjectTypeKeyGenerator extends ObjectBaseKeyGenerator implements KeyGenerator {
	
	private static final String KEY_CORE_PREFIX = "orbType:";

	@Override
	public String getKeyCorePrefix() {
		return KEY_CORE_PREFIX;
	}
}
