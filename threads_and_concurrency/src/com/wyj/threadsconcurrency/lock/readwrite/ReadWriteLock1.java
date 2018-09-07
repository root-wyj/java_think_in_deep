package com.wyj.threadsconcurrency.lock.readwrite;

/**
 * <b>简单读写锁</b>
 * <br>
 * <br>规则：
 * <li>write时 不能read，不能write</li>
 * <li>read时可以read，不能write</li>
 * <li>read时，如果出现了writeRequest，就不让新线程read了，因为write的优先级要高于read，否则read频繁可能会导致write永远不执行</li>
 * @author wuyingjie
 * @date 2018年9月7日
 */

public class ReadWriteLock1 {
	
	int readCount = 0;
	int writeCount = 0;
	int writeRequestCount = 0;
	
	public synchronized void readLock() throws InterruptedException {
		while (writeCount > 0 || writeRequestCount > 0) {
			wait();
		}
		readCount ++;
	}
	
	public synchronized void readUnLock() {
		readCount --;
//		if (readCount == 0) notifyAll();
		// 因为这时候已经释放锁了，改叫醒那些等待的线程 重新去争抢锁
		notifyAll();
	}
	
	public synchronized void writeLock() throws InterruptedException {
		writeRequestCount ++;
		
		while (readCount > 0 || writeCount > 0) {
			wait();
		}
		
		writeRequestCount --;
		writeCount ++;
	}
	
	public synchronized void writeUnLock() {
		writeCount --;
//		if (writeAccesses == 0) notifyAll();
		notifyAll();
	}

}
