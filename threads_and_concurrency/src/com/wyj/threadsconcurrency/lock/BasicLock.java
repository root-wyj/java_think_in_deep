package com.wyj.threadsconcurrency.lock;

/**
 * 
 * @author wuyingjie
 * @date 2018年9月7日
 */
public class BasicLock{
	private boolean wasSignalled = false;
	
	synchronized void doWait() throws InterruptedException {
		while(!wasSignalled) {
			wait();
		}
		wasSignalled = false;
	}
	
	synchronized void doNotify() {
		wasSignalled = true;
		notify();
	}
}
