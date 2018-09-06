package com.wyj.concurrency.chapter5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created
 * Author: wyj
 * Email: 18346668711@163.com
 * Date: 2018/2/25
 */
public class Test1 {

    public static void main(String[] args) {
//        test1_1();
//        test2();
    	test3();
    }

    private static void test1_pre(Vector<Integer> list) {
        for (int i=0; i<100; i++) {
            list.add(i);
        }
    }

    public static void test1_1() {
        Vector<Integer> list = new Vector<>();
        test1_pre(list);



        for (int i : list) {
            System.out.println(i);
            list.add(i);
        }

        System.out.println(list);
    }
    
    public static void test2() {
    	Thread t1, t2;
    	t1 = new Thread(new Runnable() {
			public void run() {
				for (int i=0; i<2; i++) {
					try {
						Thread.sleep(300);
						System.out.println(Thread.currentThread().getName());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

    	t2 = new Thread(new Runnable() {
			public void run() {
				for (int i=0; i<2; i++) {
					try {
						Thread.sleep(500);
						System.out.println(Thread.currentThread().getName());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
    	
    	t1.start();
    	t2.start();
    	
    	try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	System.out.println("main end");
    }
    
    public static void test3() {
    	Thread t1, t2;
    	t1 = new Thread(new Runnable() {
			public void run() {
				String threadName = Thread.currentThread().getName();
				System.out.println(threadName+"开始");
				try {
					System.out.println("中断前");
					Thread.sleep(5000);
					System.out.println("中断后");
				} catch (InterruptedException e) {
					System.out.println("捕获到 被中断异常");
					Thread.currentThread().interrupt();
				}
				System.out.println(threadName+"结束");
			}
		});

    	t2 = new Thread(new Runnable() {
			public void run() {
				String threadName = Thread.currentThread().getName();
				System.out.println(threadName+"开始");
				try {
					Thread.sleep(300);
					System.out.println("2中断1");
					t1.interrupt();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println(threadName+"结束");
			}
		});
    	
    	t1.start();
    	t2.start();
    }

}
