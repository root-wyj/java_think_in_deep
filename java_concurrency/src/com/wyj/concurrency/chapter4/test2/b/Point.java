package com.wyj.concurrency.chapter4.test2.b;

/**
 * 
 * @author wuyingjie
 * @date 2018年2月12日
 */

public class Point {
	private final int x,y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
}
