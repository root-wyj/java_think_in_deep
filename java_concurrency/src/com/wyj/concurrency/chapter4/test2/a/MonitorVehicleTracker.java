package com.wyj.concurrency.chapter4.test2.a;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于java监视器模式的 线程安全
 * @author wuyingjie
 * @date 2018年2月12日
 */

public class MonitorVehicleTracker {
	private final Map<String, MutablePoint> locations;
	
	public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
		this.locations = locations;
	}
	
	public synchronized Map<String, MutablePoint> getLocations() {
		return deepCopy(locations);
	}
	
	public synchronized MutablePoint getLocation(String id) {
		return locations.get(id);
	}
	
	//这里的参数是x,y 而不是MutablePoint
	public synchronized void setLocation(String id, int x, int y) {
		locations.put(id, new MutablePoint(x, y));
	}
	
	private static Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> locations) {
		Map<String, MutablePoint> result = new HashMap<>();
		for (String id : locations.keySet()) {
			result.put(id, locations.get(id));
		}
		
		return Collections.unmodifiableMap(result);
	}
}
