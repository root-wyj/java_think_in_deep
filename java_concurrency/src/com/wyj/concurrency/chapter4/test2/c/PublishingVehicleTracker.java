package com.wyj.concurrency.chapter4.test2.c;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author wuyingjie
 * @date 2018年2月23日
 */

public class PublishingVehicleTracker {
	private final Map<String, SafePoint> locations;
	private final Map<String, SafePoint> unmodifiableMap;
	
	public PublishingVehicleTracker(Map<String, SafePoint> locations) {
		this.locations = new ConcurrentHashMap<>(locations);
		this.unmodifiableMap = Collections.unmodifiableMap(locations);
	}
	
	public Map<String, SafePoint> getLocations() {
		return unmodifiableMap;
	}
	
	public SafePoint getLocation(String key) {
		return locations.get(key);
	}
	
	public void setLocation(String key, int x, int y) {
		if (locations.containsKey(key)) {
			locations.get(key).set(x, y);
		}
		throw new IllegalArgumentException("invalid vehicle name:"+key);
	}
}
