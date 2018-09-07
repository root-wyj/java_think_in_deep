package com.wyj.threadsconcurrency.lock;

import java.util.ArrayList;

/**
 * 
 * @author wuyingjie
 * @date 2018年9月7日
 */

// 正常的思路是
// 唉算了，发现自己 怎么都想不到合适的解决方案。。。
// 还是直接看源码的实现方式。。。
public class FairLock {
	
	private ArrayList<BasicLock> mLockList = new ArrayList<>();
	
	private boolean mIsLocked = false;
	
	private Thread mLockedThread = null;
	
	public void lock() {
		
		boolean isTargetThread = false;
		BasicLock lock = new BasicLock();
		
		//第一步 先把所有需要加锁的线程 生成的对应锁对象 放到锁队列中
		synchronized (mLockList) {
			mLockList.add(lock);
		}
		
		//第二步 找到队列中的第一个线程 对应的锁对象，放开
		// 其他的人 全部wait 等待notify
		//第四步 被唤醒的线程和其他所有的后来请求锁的线程 共同争抢 FairLock的锁使用权。
		// 只有被唤醒的线程，也就是队列中第一个元素对应的线程能够拿到锁，其他的线程全部wait，等待notify
		while(!isTargetThread) {
			synchronized (this) {
				// 仅仅当没锁 并且 是队列中第一个锁对象 对应的线程进入了同步块 才允许通过，并设置锁状态
				// 其他的情况，跳过if块，全部等待
				isTargetThread = !mIsLocked && mLockList.get(0) == lock;
				if (isTargetThread) {
					mLockList.remove(lock);
					mIsLocked = true;
					mLockedThread = Thread.currentThread();
					return;
				}
			}
			
			try {
				// 所有非队列第一个元素 都会释放掉FairLock，而是持有队列元素的锁，等待被唤醒
				lock.doWait();
				//第三步，上一个线程执行完毕，调用了unlock，唤醒了当前队列的第一个元素对象所对应的线程。
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	public synchronized void unlock() {
		if(this.mLockedThread != Thread.currentThread()){
			throw new IllegalMonitorStateException(
			        "Calling thread has not locked this lock");
	    }

		
		mIsLocked = false;
		
		if (mLockList.size() > 0) {
			mLockList.get(0).doNotify();
		}
	}
	
	
}
