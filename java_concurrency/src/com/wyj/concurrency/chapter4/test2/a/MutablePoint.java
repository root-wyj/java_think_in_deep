package com.wyj.concurrency.chapter4.test2.a;

/**
 * 线程不安全
 * @author wuyingjie
 * @date 2018年2月12日
 */

public class MutablePoint {
	private int x,y;
	
	public MutablePoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
}
