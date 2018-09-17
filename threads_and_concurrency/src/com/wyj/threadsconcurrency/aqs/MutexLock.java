package com.wyj.threadsconcurrency.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 基于AbstractQueuedSynchronizer实现的 排它锁
 * 
 * @author wuyingjie
 * @date 2018.09.17
 */

public class MutexLock implements Lock, java.io.Serializable{

	private static final long serialVersionUID = 1L;
	
	private final Sync sync = new Sync();

	private static class Sync extends AbstractQueuedSynchronizer {
		
		private static final long serialVersionUID = 1L;

		// 是否处于占用状态，对象刚初始化出来的时候 默认state=0
		@Override
		protected boolean isHeldExclusively() {
			return getState() == 1;
		}
		
		//状态为0的时候 获取锁
		@Override
		protected boolean tryAcquire(int acquires) {
			assert acquires == 1;
			if (compareAndSetState(0, 1)) {
				setExclusiveOwnerThread(Thread.currentThread());
				return true;
			}
			return false;
		}
		
		// 状态为1 的时候释放锁
		@Override
		protected boolean tryRelease(int releases) {
			assert releases == 1;
			
			if (compareAndSetState(1, 0)) {
				setExclusiveOwnerThread(null);
				return true;
			} else {
				throw new IllegalMonitorStateException();
			}
		}
		
		Condition newCondition() {
			return new ConditionObject();
		}
	}
	
	
	@Override
	public void lock() {
		sync.acquire(1);
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		sync.acquireInterruptibly(1);
	}

	@Override
	public boolean tryLock() {
		return sync.tryAcquire(1);
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return sync.tryAcquireNanos(1, unit.toNanos(time));
	}

	@Override
	public void unlock() {
		sync.release(1);
	}

	public boolean isLocked() {
		return sync.isHeldExclusively();
	}
	
	public boolean hasQueuedThreads() {
		return sync.hasQueuedThreads();
	}
	
	@Override
	public Condition newCondition() {
		return sync.newCondition();
	}

}
