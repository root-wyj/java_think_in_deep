package com.wyj.concurrency.chapter4.test2.b;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 使用委托的线程安全
 * @author wuyingjie
 * @date 2018年2月12日
 */

public class DelegatingVehicleTracker {
	private final ConcurrentMap<String, Point> locations;
	
	public DelegatingVehicleTracker(Map<String, Point> locations) {
		this.locations = new ConcurrentHashMap<>(locations);
	}
	
	public Map<String, Point> getLocations() {
		return Collections.unmodifiableMap(new HashMap<>(locations));
	}
	
//	与上面方法的区别 就是 上面是某一时刻的镜像  下面是实时数据
//	public Map<String, Point> getLocations() {
//		return Collections.unmodifiableMap(locations);
//	}
	
	public Point getLocation(String id) {
		return locations.get(id);
	}
	
	public void setLocation(String id, int x, int y) {
		locations.put(id, new Point(x, y));
	}
	
}
