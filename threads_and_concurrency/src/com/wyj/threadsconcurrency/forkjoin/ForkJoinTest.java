package com.wyj.threadsconcurrency.forkjoin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * <li> 把大任务分成小任务 </li>
 * <li> 为了减少线程间的竞争，每一个线程对应一个任务队列，</li>
 * <li> 工作窃取：如果某个线程的任务队列中已没有任务，将会去其他线程的任务队列中取任务去执行。</li>
 * <br>
 * <br>工作队列是一个双端队列，也是为了减少线程之间的资源竞争
 * <br>
 * <br>任务子类：
 * <li>RecursiveAction:用于没有返回结果的任务。</li>
 * <li>RecursiveTask:用于有返回结果的任务。</li>
 * <br>可以参考一下网站：
 * <li><a href="https://www.cnblogs.com/wenbronk/p/7228455.html">java-forkjoin框架的使用</a></li>
 * <li><a href="http://ifeve.com/talk-concurrency-forkjoin/">聊聊并发（八）——Fork/Join框架介绍</a></li>
 * @author wuyingjie
 * @date 2018.09.14
 */

public class ForkJoinTest extends RecursiveTask<Long>{

	private static final long serialVersionUID = 1L;

	static final int THRESHOLD = 100000;
	
	final long start;
	final long end;
	
	public ForkJoinTest(long start, long end) {
		this.start = start;
		this.end = end;
	}
	
	@Override
	protected Long compute() {
		long sum = 0;
		
		if (end - start < THRESHOLD) {
			for (long i = start; i <= end; i++) {
				sum += i;
			}
		} else {
			long mid = (end - start) / 2 + start; //这么算，因为怕越界
			// 分割任务
			ForkJoinTest task1 = new ForkJoinTest(start, mid);
			ForkJoinTest task2 = new ForkJoinTest(mid+1, end);
			
			//执行子任务
			// task1.fork();
			// task2.fork();
			
			// 调用该方法和我们想的是一致的
			// 他只调用了task2.fork方法，将task2放入了队列，而继续使用该任务所在的线程完成task1的计算
			// 就是说，把一个任务分成两个任务之后，原本任务所在的线程应该继续执行其中的一个任务，而不是将两个
			//  子任务都入队列，而晾着本线程傻傻等待。
			invokeAll(task1, task2);
			
			//等待子任务执行完 并合并任务结果
			sum = task1.join() + task2.join();
		}
		
		return sum;
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		long low = 0;
		long high = Integer.MAX_VALUE ;
		
		long t1 = System.currentTimeMillis();
		long sum = 0;
		for (long i=low; i<=high; i++) {
			sum += i;
		}
		long t2 = System.currentTimeMillis();
		System.out.println("sum="+sum+", use time:"+(t2-t1)*1.0/1000);
		
		sum = 0;
		long t3 = System.currentTimeMillis();
		ForkJoinPool pool = new ForkJoinPool();
		ForkJoinTest task = new ForkJoinTest(low, high);
		ForkJoinTask<Long> result = pool.submit(task);
		sum = result.get();
		
		long t4 = System.currentTimeMillis();
		System.out.println("sum="+sum+", use time:"+(t4-t3)*1.0/1000);
	}

}
