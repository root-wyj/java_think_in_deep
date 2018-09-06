package com.wyj.concurrency.chapter3;

/**
 * 
 * @author wuyingjie
 * @date 2018年2月7日
 */

public class NoVisibility {
	private static boolean canRead = false;
	private static int number = 0;
	
	public static void main(String[] args) {
		new ReaderThread().start();
		
		canRead = true;
		number = 42;
		System.out.println("the end");
	}
	
	public static class ReaderThread extends Thread{
		@Override
		public void run() {
			while(!canRead) {
				Thread.yield();
			}
			System.out.println("read number:"+number);
		}
	}
}
