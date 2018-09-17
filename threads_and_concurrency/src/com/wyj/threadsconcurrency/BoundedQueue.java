package com.wyj.threadsconcurrency;

import java.lang.reflect.Array;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Condition的应用
 * <br> 其实Condition和object的wait 和 notify 差不多，官方说更安全，更便捷
 * 
 * @author wuyingjie
 * @date 2018.09.17
 */

public class BoundedQueue<T>{
	final Lock lock = new ReentrantLock();
	
	final Condition notFull = lock.newCondition();
	final Condition notEmpty = lock.newCondition();
	
	final T[] data;
	int startIndex = 0, endIndex = 0, size = 0;
	
	@SuppressWarnings("unchecked")
	public BoundedQueue(Class<T> identifyClass){
		data = (T[])Array.newInstance(identifyClass, 100);
	}
	
	public T take() {
		try {
			lock.lock();
			while(size == 0)
				notEmpty.wait();
			
			size --;
			T ret = data[startIndex];
			if (++startIndex == data.length) startIndex=0;
			notFull.signal();
			return ret;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {			
			lock.unlock();
		}
		return null;
		
	}
	
	public void put(T tmp) {
		try {
			lock.lock();
			while (size == data.length)
				notFull.wait();
			data[endIndex] = tmp;
			size ++;
			if (++endIndex == data.length) endIndex = 0;
			notEmpty.signal();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}
	
	
}
