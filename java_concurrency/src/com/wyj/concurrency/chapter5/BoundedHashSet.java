package com.wyj.concurrency.chapter5;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * 使用信号量Semaphore来约束容器
 * @author wuyingjie
 * @date 2018年2月23日
 */

public class BoundedHashSet<T> {
	
	private final Set<T> set;
	private final Semaphore sem;
	
	public BoundedHashSet(int bound) {
		set = Collections.synchronizedSet(new HashSet<>());
		sem = new Semaphore(bound);
	}
	
	public boolean add(T t) throws InterruptedException {
		sem.acquire();
//		boolean b = set.add(t);
//		if (!b) {
//			sem.release();
//		}
//		return b;
		
		boolean b = false;
		try {
			b = set.add(t);
			return b;
		} finally {
			if (!b)
				sem.release();
		}
	}
	
	public boolean remove(Object o) {
		boolean b = set.remove(o);
		if (b)
			sem.release();
		return b;
	}
}
