package com.wyj.threadsconcurrency.lock;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author wuyingjie
 * @date 2018年9月7日
 */

public class BlockingQueue<T> {
	
	private List<T> list = new ArrayList<>();
	
	private int limit = 0;
	
	public BlockingQueue(int limit) {
		this.limit = limit;
	}
	
	public synchronized void push(T data) throws InterruptedException {
		while (list.size() == limit) {
			wait();
		}
		
		if (list.size() == 0) {
			// 只有这种情况才存在 阻塞，所以只有这种情况才需要notifyAll
			notifyAll();
		}
		
		list.add(data);
	}
	
	public synchronized T pop() throws InterruptedException {
		while (list.size() == 0) {
			wait();
		}
		
		if (list.size() == limit) {
			// 这种情况是 唤醒push，因为只有这种情况，puhs才会阻塞
			list.notifyAll();
		}
		return list.remove(0);
	}
	
	
}
