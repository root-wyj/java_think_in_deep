package com.wyj.threadsconcurrency.lock;

/**
 * 
 * @author wuyingjie
 * @date 2018年9月7日
 */

public class Semaphore {

	int signals = 0;
	int bound = 0;
	
	public Semaphore(int bound) {
		this.bound = bound;
	}
	
	public synchronized void take() throws InterruptedException {
		while (signals == bound) {
			wait();
		}
		signals++;
		notify();
	}
	
	public synchronized void release() throws InterruptedException {
		while (signals == 0) {
			wait();
		}
		signals--;
		notify();
	}
	
}
