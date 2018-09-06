# Java线程状态及切换

### Java中线程的生命周期大体可以分为5中状态

1. `新建(NEW)` -- 新创建了一个线程对象
2. `可运行(RUNNABLE)` -- 线程对象创建后，其他线程(比如main线程）调用了该对象的start()方法。该状态的线程位于可运行线程池中，等待被线程调度选中，获取cpu 的使用权 。
3. `运行(RUNNING)` -- 可运行状态(runnable)的线程获得了cpu 时间片（timeslice） ，执行程序代码。
4. `阻塞(BLOCKED)` -- 阻塞状态是指线程因为某种原因放弃了cpu 使用权，也即让出了cpu timeslice，暂时停止运行。直到线程进入可运行(runnable)状态，才有机会再次获得cpu timeslice 转到运行(running)状态。阻塞的情况分三种： 
 - 等待阻塞：运行(running)的线程执行o.wait()方法，JVM会把该线程放入**等待队列(waitting queue)**中。
 - 同步阻塞：运行(running)的线程在获取对象的同步锁时，若该同步锁被别的线程占用，则JVM会把该线程放入**锁池(lock pool)**中。
 - 其他阻塞：运行(running)的线程执行Thread.sleep(long ms)或t.join()方法，或者发出了I/O请求时，JVM会把该线程置为阻塞状态。当sleep()状态超时、join()等待线程终止或者超时、或者I/O处理完毕时，线程重新转入可运行(runnable)状态。
5. `死亡(DEAD)` -- 线程run()、main() 方法执行结束，或者因异常退出了run()方法，则该线程结束生命周期。死亡的线程不可再次复生。

![|center](http://dl.iteye.com/upload/picture/pic/116719/7e76cc17-0ad5-3ff3-954e-1f83463519d1.jpg)

------------

上面是我们通俗理解的几种状态，在`java.lang.Thread.State`中对状态也有详细的定义和说明：

 - `NEW` 状态是指线程刚创建, 尚未启动
 - `RUNNABLE` 状态是线程正在正常运行中, 当然可能会有某种耗时计算/IO等待的操作/CPU时间片切换等, 这个状态下发生的等待一般是其他系统资源, 而不是锁, Sleep等
 - `BLOCKED` 这个状态下, 是在多个线程有同步操作的场景, 比如正在等待另一个线程的synchronized 块的执行释放, 或者可重入的 synchronized块里别人调用wait() 方法, 也就是这里是线程在等待进入临界区
 - `WAITING` 这个状态下是指线程拥有了某个锁之后, 调用了他的wait方法, 等待其他线程/锁拥有者调用 notify / notifyAll 以便该线程可以继续下一步操作, 这里要区分 BLOCKED 和 WATING 的区别, 一个是在临界点外面等待进入, 一个是在临界点里面wait等待别人notify, 线程调用了join方法 join了另外的线程的时候, 也会进入WAITING状态, 等待被他join的线程执行结束(join的示例已在Test1.test2中给出)
 - `TIMED_WAITING`  这个状态就是有限的(时间限制)的WAITING, 一般出现在调用wait(long), join(long)等情况下, 另外一个线程sleep后, 也会进入TIMED_WAITING状态

 - `TERMINATED` 这个状态下表示 该线程的run方法已经执行完毕了, 基本上就等于死亡了(当时如果线程被持久持有, 可能不会被回收)


### 等待队列与锁池
1. 调用obj的wait、notify方法前，必须获得obj锁，也就是必须在synchronized(obj)代码块内。
2. 当前线程想调用对象A的同步方法时，发现对象A的锁被别的线程占有，此时当前线程进入锁池状态。简言之，锁池里面放的都是想争夺对象锁的线程。
3. 当一个线程1被另外一个线程2唤醒时，1线程进入锁池状态，去争夺对象锁。
4. **锁池是在同步的环境下才有的概念，一个对象对应一个锁池。**

 与等待队列和锁池的相关步骤图：
![|center](http://dl.iteye.com/upload/picture/pic/116721/3f19f0fb-33ae-322f-9f6a-035f0bf3a2d5.jpg)


-------------

参考自：

- [Java线程的5种状态及切换](http://blog.csdn.net/pange1991/article/details/53860651)
- [java 线程的几种状态](https://www.cnblogs.com/xll1025/p/6415283.html)










































