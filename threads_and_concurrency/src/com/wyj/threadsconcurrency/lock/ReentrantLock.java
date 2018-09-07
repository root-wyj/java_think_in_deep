package com.wyj.threadsconcurrency.lock;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author wuyingjie
 * @date 2018年9月7日
 */

public class ReentrantLock {
	
	Map<Thread, Integer> mThreadMap = new HashMap<>();
	
	BasicLock mLock = new BasicLock();
	
	boolean mIsLock = false;
	
	public void lock() throws InterruptedException {
		
		synchronized (this) {
			
		}
		
		mLock.doWait();
	}
	
	public void unlock() {
		
	}
}
