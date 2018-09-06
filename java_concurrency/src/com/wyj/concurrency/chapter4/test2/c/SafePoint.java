package com.wyj.concurrency.chapter4.test2.c;

/**
 * 
 * @author wuyingjie
 * @date 2018年2月23日
 */

public class SafePoint {
	private int x,y;
	
	public SafePoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public SafePoint(SafePoint p) {
		this(p.get());
	}
	
	/* 私有构造函数捕获模式
	 * 
	 * 私有构造函数的存在可以避免一些竞争条件，这些竞争条件会发生在复制构造
	 * 函数实现为this(p.x, p.y)的时候
	 * 
	 * 文章 http://atbug.com/private-constructor-capture-idiom/ 解释的比较清楚
	 */
	private SafePoint(int[] a) {
		this(a[0], a[1]);
	}
	
	public synchronized int[] get() {
		return new int[]{x, y};
	}
	
	public synchronized void set(int x, int y) {
		this.x = x;
		this.y = y;
	}

}
