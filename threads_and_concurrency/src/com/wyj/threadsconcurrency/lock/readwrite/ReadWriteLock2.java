package com.wyj.threadsconcurrency.lock.readwrite;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>可重入读写锁</b>
 * <br>
 * <br>问题：
 * <br>简单读写锁中，是不可重入的，下面的例子：
 * <ol>1. Thread 1 获得了读锁</ol>
 * <ol>2. Thread 2 请求写锁，但因为Thread 1 持有了读锁，所以写锁请求被阻塞。</ol>
 * <ol>3. Thread 1 再想请求一次读锁，但因为Thread 2处于请求写锁的状态，所以想再次获取读锁也会被阻塞。</ol>
 * 此时，陷入死锁。
 * <br>规则：
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * @author wuyingjie
 * @date 2018年9月7日
 */

public class ReadWriteLock2 {
	
	Map<Thread, Integer> mReadCountMap = new HashMap<>();
	int writeAccesses = 0;
	int writeRequestCount = 0;
	Thread mWritingThread = null;
	
	public static void main(String[] args) {
		Map<String, Integer> map = new HashMap<>();
		map.put("haha", 2);
		map.merge("haha", 1, (old, value) -> {
			System.out.println("old:"+old);
			System.out.println("value:"+value);
			return old;
		});
	}
	
	public synchronized void readLock() throws InterruptedException {
		Thread thread = Thread.currentThread();
		
		while (!canRead(thread)) {
			wait();
		}
		
		mReadCountMap.merge(thread, 1, (arg1, arg2) -> {return arg1 + arg2;});
	}
	
	private boolean canRead(Thread t) {
//		if (mReadCountMap.containsKey(t)) return true;
//		if (writeAccesses > 0 || writeRequestCount > 0) return false;
//		return true;
		
		//将写锁降低到读锁，有写锁，当然让读了
		if (t == mWritingThread) return true;
		//注意这里只有没有写锁的时候 才允许重入
		if (writeAccesses > 0) return false;
		if (mReadCountMap.containsKey(t)) return true;
		if (writeRequestCount > 0) return false;
		return true;
	}
	
	public synchronized void readUnLock() {
		Thread thread = Thread.currentThread();
		int value = mReadCountMap.get(thread);
		if (value == 1) {
			mReadCountMap.remove(thread);
		} else {
			mReadCountMap.put(thread, value-1);
		}
		notifyAll();
	}
	
	public synchronized void writeLock() throws InterruptedException {
		Thread thread = Thread.currentThread();
		
		writeRequestCount ++;
		
		while (!canWrite(thread)) {
			wait();
		}
		
		writeRequestCount --;
		writeAccesses ++;
		
		mWritingThread = thread;
	}
	
	private boolean canWrite(Thread t) {
		// 将读锁升级到写锁
		if (isOnlyReader(t)) return true;
		
		if (t == mWritingThread) return true;
		if (writeAccesses > 0) return false;
		if (mReadCountMap.size() > 0) return false;
		return true;
	}
	
	private boolean isOnlyReader(Thread t) {
		return mReadCountMap.size() == 1 && mReadCountMap.containsKey(t);
	}
	
	public synchronized void writeUnLock() {
		writeAccesses --;
		if (writeAccesses == 0) {
			mWritingThread = null;
		}
		notifyAll();
	}
}
