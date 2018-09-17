# ThreadPoolExecutor 线程池

类的继承和实现关系可以看下图：

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/ThreadPoolExecutor_class.png?raw=true)

Java也提供了四种线程池的工具构造方法：

- `Executors.newCachedThreadPool()` 可缓存线程池，若线程池长度超过处理需要，则回收空线程，否则创建新线程，线程规模可无限大。
- `Executors.newFixedThreadPool(3)` 定长线程池，可控制线程最大并发数，超出的线程会在队列中等待。
- `Executors.newScheduledThreadPool(5)` 定长线程池，支持定时及周期性任务执行，类似Timer。
- `Executors.newSingleThreadExecutor()` 单线程 的线程池，支持FIFO, LIFO, 优先级策略

<br>
构造方法：

```java
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {}
```

参数说明 
- `corePoolSize`：线程池的核心线程数。 
- `maximumPoolSize`：线程池所能容纳的最大线程数。 
- `keepAliveTime`：非核心线程闲置时的超时时长。超过该时长，非核心线程就会被回收。 
- `unit`：keepAliveTime的时间单位。 
- `workQueue`：线程池中的任务队列。 
- `threadFactory`：线程工厂，默认值DefaultThreadFactory。 
- `handler`：饱和策略，当线程池中的数量大于maximumPoolSize，对拒绝任务的处理策略，默认值ThreadPoolExecutor.AbortPolicy()。


<br>

----------

## 丢弃策略

当队列满了之后，并且线程池的线程也已经饱和，如果再来新的任务，就需要一定的策略来处理这些任务。

`ThreadPoolExecutor`提供了几个内部类实现供我们使用。

- `CallerRunsPolicy` **调用者执行策略** 不会开辟新线程，而是提交任务的线程来负责维护任务。会先判断ThreadPoolExecutor对象的状态，之后执行任务。这样处理的一个好处，是让caller线程运行任务，以推迟该线程进一步提交新任务有效的缓解了线程池对象饱和的情况。
- `AbortPolicy` **废弃终止** 不处理，而是抛出java.util.concurrent.RejectedExecutionException异常。
 注意，处理这个异常的线程是执行execute()的调用者线程。
- `DiscardPolicy` **直接丢弃** 
- `DiscardOldestPolicy` **丢弃最老** 会丢弃掉一个任务，但是是队列中最早的。注意，会先判断ThreadPoolExecutor对象是否已经进入SHUTDOWN以后的状态。之后取出队列头的任务并不做任何处理，即丢弃，再重新调用execute()方法提交新任务。



<br>

---------

## 阻塞队列

阻塞队列`BlockingQueue`需要在初始化线程池的时候传递给线程池存放任务的一个队列。

Java默认提供了7个阻塞队列：

- `ArrayBlockingQueue`：一个由数组结构组成的有界阻塞队列。
- `LinkedBlockingQueue`：一个由链表结构组成的有界阻塞队列。默认Integer.MAX_VALUE
- `PriorityBlockingQueue`：一个支持优先级排序的无界阻塞队列。
- `DelayQueue`：一个使用优先级队列实现的无界阻塞队列。
- `SynchronousQueue`：一个不存储元素的阻塞队列。
- `LinkedTransferQueue`：一个由链表结构组成的无界阻塞队列。
- `LinkedBlockingDeque`：一个由链表结构组成的双向阻塞队列。

不同的阻塞队列放到线程池中会有不同的效果，需要注意。

更多关于阻塞队列[聊聊并发（七）——Java中的阻塞队列](http://ifeve.com/java-blocking-queue/)

<br>

----------

## 源码分析

看这里 -> [【JUC】JDK1.8源码分析之ThreadPoolExecutor（一）](https://www.cnblogs.com/leesf456/p/5585627.html)

-------------

## 流程？

还是这篇文章（在最后） -> [【JUC】JDK1.8源码分析之ThreadPoolExecutor（一）](https://www.cnblogs.com/leesf456/p/5585627.html)

--------------

## 生命周期

- `RUNNING`: 接受新任务并且处理已经进入阻塞队列的任务
- `SHUTDOWN`: 不接受新任务，但是处理已经进入阻塞队列的任务
- `STOP`: 不接受新任务，不处理已经进入阻塞队列的任务并且中断正在运行的任务
- `TIDYING`: 所有的任务都已经终止，workerCount为0， 线程转化为TIDYING状态并且调用terminated钩子函数
- `TERMINATED`: terminated钩子函数已经运行完成
