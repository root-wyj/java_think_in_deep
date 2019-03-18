# JUC Thread

<br>

- [JUC Thread](#juc-thread)
  - [ThreadPoolExecutor 线程池](#threadpoolexecutor-线程池)
    - [源码分析](#源码分析)
    - [流程？](#流程)
    - [生命周期](#生命周期)
  - [Future & Callable](#future--callable)
    - [Callable](#callable)
    - [Future](#future)


----------

<br>

## ThreadPoolExecutor 线程池

类的继承和实现关系可以看下图：

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/ThreadPoolExecutor_class.png?raw=true)

<br>

**`Core and maximum pool sizes`**

当调用了`execute`方法，新的任务被提交时，

- 如果运行的线程数小于`corePoolSize`，那么都会创建一个新的线程，尽管其他的线程时空闲的。
- 如果运行的线程数大于`corePoolSize`， 小于`maxPoolSize`，直到线程队列满了，才会创建新的线程。
- 如果运行的线程数等于`maxPoolSize`，并且队列满了，那么会按照舍弃策略舍弃某些线程。

通常设置`corePoolSize=maxPoolSize`，创建一个固定线程大小的线程池(`fixed-size thread pool`)。

这两个参数一般在构造方法中设置，但是可以调用相应方法动态设置，也可以调用相应方法查看。

> 默认的，`core threads`在任务到来时才会创建，可以通过重写`prestartCoreThread` 或 `prestartAllCoreThreads`在创建线程池的时候就初始化创建核心线程数的线程。

<br>

> **`keep alive times`**：如果`ThreadPoolExecutor`线程数超过了核心线程池，并且闲置时间超过了`keepAliveTime`，那么这些线程就会被终止。一般的，该参数仅仅在`runningPoolSize>corePoolSize`时生效，但是 **`allowCoreThreadTimeOut(boolean)`方法可以应用该失效策略在核心线程上，当该参数不为0**


<br>

**`ThreadFactory`**

新线程的创建使用的`TreadFactory`。默认的会调用`TreadFactory.defaultThreadFactory`返回的线程工厂，它创建的所有线程会在**相同的ThreadGroup**，**相同的优先级`NORM_PRIORITY`**和**非守护线程(non-daemon)的状态**。

我们可以自己提供`ThreadFactory`，可以修改 **thread's name, thread group, priority, daemon status, etc**。

如果线程池创建线程`newThread`失败了，返回了null，`ThreadPoolExecutor`还是会继续，但是并不会执行任何任务。

**Threads should possess the "modifyThread" {@code RuntimePermission}. If worker threads or other threads using the pool do not possess this permission, service may be degraded: configuration changes may not take effect in a timely manner, and a shutdown pool may remain in a state in which termination is possible but not completed.**（这段担心自己翻译的不是特别准确，所以直接看原文）


<br>

**`Queuing`**

任何`BlockingQueue`都可以用作存储和传递被提交的任务。

当：

- `runningThreads < corePoolSize` 会创建新的线程
- `runningThreads >= corePoolSize` 会将任务入队列
- 队列满了，`runningThreads < maxPoolSize` 创建新的线程，`runningThreads >= corePoolSize`拒绝。

通常有三种策略：

- `SynchronousQueue`: 只传递任务，不存储任务的队列。如果任务进入队列却没有线程立刻消费，则会失败。
- `LinkedBlockingQueue`: 能无限存储任务的队列。
- `ArrayBlockingQueue`: 有限长度的队列。通常这种更难控制。


<br>
<br>

**`阻塞队列`**

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


**`丢弃策略`**

当队列满了之后，并且线程池的线程也已经饱和，如果再来新的任务，就需要一定的策略来处理这些任务。

`ThreadPoolExecutor`提供了几个内部类实现供我们使用。

- `CallerRunsPolicy` **调用者执行策略** 不会开辟新线程，而是提交任务的线程来负责维护任务。会先判断ThreadPoolExecutor对象的状态，之后执行任务。这样处理的一个好处，是让caller线程运行任务，以推迟该线程进一步提交新任务有效的缓解了线程池对象饱和的情况。
- `AbortPolicy` **废弃终止** 不处理，而是抛出java.util.concurrent.RejectedExecutionException异常。
 注意，处理这个异常的线程是执行execute()的调用者线程。
- `DiscardPolicy` **直接丢弃** 
- `DiscardOldestPolicy` **丢弃最老** 会丢弃掉一个任务，但是是队列中最早的。注意，会先判断ThreadPoolExecutor对象是否已经进入SHUTDOWN以后的状态。之后取出队列头的任务并不做任何处理，即丢弃，再重新调用execute()方法提交新任务。



<br>


Java也提供了四种线程池的工具构造方法：

- `Executors.newCachedThreadPool()` 可缓存线程池，若线程池长度超过处理需要，则回收空线程，否则创建新线程，线程规模可无限大。
- `Executors.newFixedThreadPool(3)` 定长线程池，可控制线程最大并发数，超出的线程会在队列中等待。
- `Executors.newScheduledThreadPool(5)` 定长线程池，支持定时及周期性任务执行，类似Timer。
- `Executors.newSingleThreadExecutor()` 单线程 的线程池，支持FIFO, LIFO, 优先级策略, 执行过程中，如果线程异常退出, 那么线程池也会重新启动一个运行下面的任务。

<br>

------------

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

简单使用：

```java
public class TemplateMessageExecutorPool {

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = new ThreadPoolExecutor(
                10,
                10,
                120L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
    }

    public void addTask(Runnable task) {
        executor.submit(task);
    }

}

```

----------

<br>

### 源码分析

看这里 -> [【JUC】JDK1.8源码分析之ThreadPoolExecutor（一）](https://www.cnblogs.com/leesf456/p/5585627.html)

-------------

### 流程？

还是这篇文章（在最后） -> [【JUC】JDK1.8源码分析之ThreadPoolExecutor（一）](https://www.cnblogs.com/leesf456/p/5585627.html)

--------------

### 生命周期

- `RUNNING`: 接受新任务并且处理已经进入阻塞队列的任务
- `SHUTDOWN`: 不接受新任务，但是处理已经进入阻塞队列的任务
- `STOP`: 不接受新任务，不处理已经进入阻塞队列的任务并且中断正在运行的任务
- `TIDYING`: 所有的任务都已经终止，workerCount为0， 线程转化为TIDYING状态并且调用terminated钩子函数
- `TERMINATED`: terminated钩子函数已经运行完成


-----------

<br>

## Future & Callable

### Callable

首先，看Callable接口的申明

```java
package java.util.concurrent;

@FunctionalInterface
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;
}

```

他与`Runnable`接口的主要区别就是，**`Callable<V>`可以返回值**。而范型就是指的返回值的类型。

<br>

一般情况下，会用下面的方式使用Callable接口。主要是 1 和 3.

```java
<T> Future<T> submit(Callable<T> task);
<T> Future<T> submit(Runnable task, T result);
Future<?> submit(Runnable task);
```

### Future

`Future`就是 对于具体的`Runnable`或者`Callable`的执行结果进行取消、查询是否完成，获取结果。通过`get`阻塞方法获取结果，会阻塞到直到有结果返回。

接口申明：

```java
package java.util.concurrent;

public interface Future<V> {
    /**
     * 尝试取消正在执行的任务。
     * 如果已经完成，或者已经取消，或者因为其他原因不能取消的，将返回false
     * 如果没有start，那么将返回true，并且也不会启动了。
     * 如果已经start，就看 mayInterruptIfRunning 参数。
     */
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    V get() throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}

```

也就是说Future提供了三种功能：

- 判断任务是否完成；
- 能够中断任务；
- 能够获取任务执行结果

<br>

`Future` 是一个接口，他的唯一实现类就是`FutureTask`，`FutureTask` 有以下两个构造器:

```java
public FutureTask(Callable<V> callable) {
}

public FutureTask(Runnable runnable, V result) {
}
```

通常使用`ExecutorService`来操作这些接口。




