package com.wyj.concurrency.chapter5;

import java.util.concurrent.CountDownLatch;

/**
 * 闭锁，例如确保一个计算不会执行，直到它需要的资源被初始化。
 * 但闭锁一旦到达终点，就不能再改变状态了
 * @author wuyingjie
 * @date 2018年2月23日
 */

public class TSTLatch {
	//使用CountDownLatch计算多个并发 完成某个任务需要的时间。
	public long timeTasks(int nThreads, final Runnable task) throws InterruptedException {
		
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(nThreads);
		
		for (int i=0; i < nThreads; i++) {
			Thread t = new Thread(){
				public void run() {
					try {
						System.out.println("等待大门打开。。");
						startGate.await();
						System.out.println("大门已打开，GOGOGO！！！");
						try {
							task.run();
						} finally {
							endGate.countDown();
							System.out.println("关闭大门");
						}
					} catch (InterruptedException e) {
					}
					
				};
			};
			t.start();
		}
		long startTime = System.nanoTime();
		long startTime2 = System.currentTimeMillis();
		System.out.println("t1:"+startTime+", t2:"+startTime2);
		startGate.countDown();
		System.out.println("大门打开。。");
		System.out.println("等待大门关闭。。");
		endGate.await();
		System.out.println("大门全部关闭");
		long endTime = System.nanoTime();
		return endTime-startTime;
	}
	
	public static void main(String[] args) throws InterruptedException {
		TSTLatch test = new TSTLatch();
		long time = test.timeTasks(5, new Runnable() {
			@Override
			public void run() {
				Math.pow(2.5, 3.4);
			}
		});
		System.out.println("use time:"+time);
	}
}
