package com.wyj.threadsconcurrency.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

/**
 * 基于AbstractQueuedSynchronizer实现的
 * <br>在同一时刻，只能最多有两个线程并行访问，超过限制进入阻塞。
 * 
 * @author wuyingjie
 * @date 2018.09.17
 */

public class TwinsLock {

	private final Sync sync = new Sync(2);
	
	private static final class Sync extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;

		public Sync(int count) {
			
			if (count < 0) {
				throw new IllegalArgumentException("count must large than zero!!");
			}
			
			setState(count);
		}
		
		@Override
		protected int tryAcquireShared(int reduceCount) {
			for(;;) {
				int current = getState();
				int newCount = current - reduceCount;
				if (newCount < 0 || compareAndSetState(current, newCount)) {
					return newCount;
				}
			}
		}
		
		@Override
		protected boolean tryReleaseShared(int returnCount) {
			for(;;) {
				int current = getState();
				int newCount = current + returnCount;
				if (compareAndSetState(current, newCount)) {
					return true;
				}
			}
		}
		
	}
	
	public void lock() {
		sync.acquireShared(1);
	}
	
	public boolean tryLock() {
		return sync.tryAcquireShared(1) >= 0;
	}
	
	public void unlock() {
		sync.releaseShared(1);
	}
	
	public Condition newCondition() {
		return null;
	}
	
}
