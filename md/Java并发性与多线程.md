# Java并发性与多线程
@(深入理解Java)[并发,多线程,concurrenct,lock,锁]

## 了解多线程

单CPU在同一时间只能执行一个线程上的任务，但是在单CPU上计算机也能在同一时间点执行多任务或多进程。虽然并不是严格意义上的“同一时间点”，而是多个任务共享一个CPU，CPU通过运行切换，使每个任务都能获得时间片运行，看起来就像是多个任务在同时进行。

后来，随着多CPU（多处理器）多核的出现，线程可以在不同的**CPU核**上得到真正意义上的并行执行。

优点：
- 资源利用率好，能充分利用CPU
- Server-Client模式中，专门有一个线程去监听客户端请求，相应更快

缺点：
- 设计会更复杂，主要是数据一致性问题，也就是同步问题
- 上下文切换的开销。
 CPU从执行一个线程切换到执行另一个线程的时候，需要先存储当前线程的本地数据，程序指针等，然后载入另一个线程的本地数据，线程指针等，最后才能开始执行。
- 增加资源消耗。线程运行也是需要内存消耗的，占用内存保存本地的堆栈信息，也会占用操作系统的一些信息来管理线程。

**多线程怎么交流通信的？怎么同步的？ java中是通过内存。那么有什么问题。**

<br>

--------

## 并发编程模型

<br>

### 并行工作者模型

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-1.png)

**并行工作者模型中，委派者（Delegator）将传入的作业分配给不同的工作者。每个工作者完成整个任务。工作者们并行运作在不同的线程上，甚至可能在不同的CPU上。**

这种模型容易理解，简单，直接增加新的线程就可以增加整体的并行度。分布式任务用的就是这种模型。

但是如果涉及到共享状态，就会变得复杂：

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-2.png)

共享状态会被多个线程操作，涉及到线程同步的问题。在Java中就可能会涉及竟态、死锁，也可能发生阻塞，影响程序执行的效率。

上面的模型中，可以将共享状态存放在Delegator中，Worker通过消息机制将消息发送给Delegatar，然后Delegatar操作共享状态，序列化处理所有的消息。

<br>

### 流水线模型

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-3.png)

将上面模式中的worker串联起来，变成一个个流水线，而每一个流水线是一个线程。每一个加工数据的工厂是不保存数据的，他们就像是加工数据的管道一样，线程中的数据经过一次次加工，最后变成了想要的数据。比如说Java中的`Servlet`。

由于大多数系统可以执行多个作业，作业从一个工作者流向另一个工作者取决于作业需要做的工作。在实际中可能会有多个不同的虚拟流水线同时运行。

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-5.png)

作业甚至也有可能被转发到超过一个工作者上并发处理。比如说，作业有可能被同时转发到作业执行器和作业日志器。比如下图：

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-6.png)

当简单的流水线模型复杂到这个程度的时候，就有必要请出流水线模型的升级版模型--`反应器、事件驱动模型`


<br>

### 反应器，事件驱动模型

系统内的工作者对系统内出现的事件做出反应，这些事件也有可能来自于外部世界或者发自其他工作者。事件可以是传入的HTTP请求，也可以是某个文件成功加载到内存中等。(比如说Tomcat处理请求的过程，虽然还是和流水线有点像)

如果说流水线模型的实现是线性的或者是链式的（责任链模式？比如Spring处理httpRequest的filter-chain），那么反应器、事件驱动模型就是基于监听者模式的，每一个工作者需要向自己感兴趣的工作者注册，当自己感兴趣的工作者收到消息之后，自然会通知本工作者去处理该消息。像下图一样：

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-7.png)

更复杂的，比如说工作者虽然都是一大类，但是性质也不同，在这一道工序中，有的工作者专门处理水果，有的工作者专门处理蔬菜，而在下一道工序中，水果和蔬菜又被看做一种东西，需要这道工序中的一类工作者处理就可以了。那么第二道工序中的工作者就需要去监听多个工作者。就像上图一样。


上面说的是无状态的反应器、事件驱动模型，另外一种就是工作者是一个个的线程，根据工序的复杂度不同，启动不同数量的相应工序的工作者，然后工作者之间也不会直接通信，他们在不同的通道中发布自己的消息，其他工作者只监听这些通道中的消息。（有点像复杂、解耦版的并行工作者模型）

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrency-models-8.png)


<br>

--------

## 如何处理并发编程中的共享状态

我们以Java中多线程为前提。

<br>

**`谁会有问题`**

就是说在Java多线程执行过程中，谁会出现与我们预料中不一样的结果。

根据JVM运行时内存，我们知道是**存在堆上的数据。**

从Java类的角度来讲，是**成员变量，而不是方法**


<br>

**`为什么会出现问题`**

详细请看[Java内存模型]

<br>

**`怎么解决问题`**

Java提供了两种基本的解决方法：
`volatile`和`synchronize`同步块。

<br>

---------

上文，主要介绍了Java中如何应对多线程，保证共享变量的数据一致性。下面介绍一下通用的方法，就是应该从那几个方面考虑。

<br>

**`线程之间如何通信及线程之间如何同步`**

**通信是指线程之间以何种机制来交换信息。线程之间的通信机制有两种：`共享内存`和`消息传递`。**

**同步是指程序用于控制不同线程之间操作发生相对顺序的机制**

- 共享内存并发模型：线程之间共享程序的公共状态，线程之间通过写-读内存中的公共状态来隐式进行通信。
 同步是显式进行的。程序员必须显式指定某个方法或某段代码需要在线程之间互斥执行。
- 消息传递并发模型：线程之间没有公共状态，线程之间必须通过明确的发送消息来显式进行通信。
 由于消息的发送必须在消息的接收之前，因此同步是隐式进行的。

Java中，线程间通信是通过共享内存的方式实现的。

<br>

**`处理共享状态的几种思路`**

撇开Java，通常来讲，防止并发导致的数据不一致的方法有哪些？
- `变量的都存储在线程中`，就是说所有的变量都不会跨线程，线程中的变量只有本线程可以操作，那么自然也就不涉及同步这些变量的状态了。例如Elixir中的进程。
 那么由于其他线程而需要改变本线程的变量该怎么实现？通过线程之间发消息。而且传递的参数也是从这个线程到那个线程的一份拷贝。
- `所有的变量都不可变，任何的修改都是Copy On Write。`只有常量，没有变量。就不用担心在这个线程使用的时候变量发生了改变等。
- `对可能会被多个线程操作的数据加锁`。通过锁的方式，保证变量在一个时间只能被一个线程访问到。（锁的更底层原理是原子操作，或者说是因为线程间修改数据，线程中数据对其他线程的不可见性导致的数据不一致情况。）

<br>

---------

## 了解Java线程




<br>

--------

## 竞态条件与临界区

**在同一程序中运行多个线程本身不会导致问题，问题在于多个线程访问了相同的资源。如，同一内存区（变量，数组，或对象）、系统（数据库，web services等）或文件。**

实际上，这些问题只有在一或多个线程向这些资源做了写操作时才有可能发生，只要资源没有发生变化,多个线程读取相同的资源就是安全的。

**当两个线程竞争同一资源时，如果对资源的访问顺序敏感**，就称`存在竞态条件`。**导致竞态条件发生的代码区**称作`临界区`。在临界区中使用适当的同步就可以避免竞态条件。





<br>

-----------

## Java中线程间基本同步方式

<br>

**`volatile` 关键字**

[深入理解Java中的volatile关键字](http://www.hollischuang.com/archives/2648)

[再有人问你volatile是什么，把这篇文章也发给他。](http://www.hollischuang.com/archives/2673)

<br>

**`synchronized` 关键字**

[再有人问你synchronized是什么，就把这篇文章发给他。](http://www.hollischuang.com/archives/2637)


<br>

**`CAS` 原子操作**

`CAS(Compare And Swap)`现在操作系统已经提供了这样操作的原子指令。

这也是一种乐观锁的实现，将修改前的值与现在的值比较，相同则改成目标值，不相同再重新读取并修改，然后再将修改前的值与现在的值比较，相同则改成目标值，如此循环，直到该成功为止。这也是乐观锁的实现方式。

<br>

**`wait 与 notify`**

`wait notify notifyAll`都必须要在同步代码块`synchronized`中执行。

JVM在调用wait等方法的时候，首先会检查当前线程是否是锁的拥有者，否则抛出IllegalMonitorStateException。

调用`wait`之后，当前线程会释放监视器对象上的锁。一旦一个`wait`的线程被唤醒，并不能立刻退出`wait`方法的调用，需要等到`notify`的线程退出自己的同步代码块，释放监视器对象上的锁，然后被唤醒的`wait`线程才开始抢占锁，如果抢到，则继续执行`wait`后面的代码，直到退出同步代码块，释放监视器上的锁。

下面这张图，形象的描述了整个过程：

![|center](http://dl.iteye.com/upload/picture/pic/116721/3f19f0fb-33ae-322f-9f6a-035f0bf3a2d5.jpg)

> 注意，不要使用String字符串或者是全局对象作为Monitor监视器对象，最好使用唯一对应的对象作为监视器。以免代码没有按照预期的运行，出现意想不到的意外。

<br>

---------

## ThreadLocal

`ThreadLocal`是Java中一个比较特殊的类，虽然不同的线程执行同一段代码，访问同一个ThreadLocal对象，但是每个线程只能看到对应当前线程的保存在ThreadLocal中的实例。

基本使用：

```Java
private ThreadLocal myThreadLocal1 = new ThreadLocal<String>();
myThreadLocal1.set("Hello ThreadLocal");
String threadLocalValues = myThreadLocal.get();

//另外初始化的时候还可以通过重写`initialValue`为ThreadLocal指定初始值
//调用set方法前，调用get方法所有线程看到的都是此初始值。
private ThreadLocal myThreadLocal = new ThreadLocal<String>() {
   @Override protected String initialValue() {
       return "This is the initial value";
   }
};

```

实现思路：

`Thread`类有一个类型为`ThreadLocal.ThreadLocalMap`的实例变量`threadLocals`，也就是说**每个线程有一个自己的`ThreadLocalMap`**。`ThreadLocalMap`有自己的独立实现，可以简单地将它的**key视作ThreadLocal，value为代码中放入的值**（实际上key并不是ThreadLocal本身，而*是它的一个弱引用*）。**每个线程在往某个ThreadLocal里塞值的时候，都会往自己的ThreadLocalMap里存，读也是以某个ThreadLocal作为引用，在自己的map里找对应的key，从而实现了线程隔离。**

其实整个ThreadLocal实现的重点就是`ThreadLocal.ThreadLocalMap`。

另外一个需要注意**Thread中的ThreadLocal.ThreadLocalMap类型的成员变量threadLocals**里面有一个table数组，table数组中存储了`Entry extend WeakReference<ThreadLocal<?>>`对象，key 就是弱引用的ThreadLocal，value就是需要存储的对象。

也就是说，`ThreadLocalMap`存储的ThreadLocal的弱引用。如果使用传统的key-value来存储，就会造成ThreadLocal对象和名义上存在ThreadLocal对象中实际存在ThreadLocalMap中的value对象与线程强绑定，就是，如果线程不销毁，引用就会一直存在，而实际上是如果ThreadLocal对象已经不可达，那么就没办法再获取到ThreadLocal中存储的对象，其实这时候就已经该销毁ThreadLocal对象已经value对象了，所以使用了弱引用。而且一般的线程都会是在线程池中复用的，并不会使用了之后马上销毁该线程，所以，使用这种弱引用来保存ThreadLocal对象。而且，ThreadLocal对象失效，也仅仅是Entry中获取到的key为null，value对象并没有释放，所以，ThreadLocalMap还有一大堆机制和方法在ThreadLocal失效之后，来清除value对象的引用，从而让value对象也能释放。所以在不用的时候，记着调用`remove`方法

了解了整个`ThreadLocal`和`ThreadLocal.ThreadLocalMap`的思路，首先看下outline：

![|left](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/threadlocal_class_outline.jpg)
![|right](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/threadlocalmap_class_outline.jpg)

<br>

下面看`ThreadLocal`源码：

```java
//保存在Thread类中的ThreadLocal.ThreadLocalMap 成员变量
public class Thread implements Runnable {
    /* ThreadLocal values pertaining to this thread. This map is maintained
     * by the ThreadLocal class. */
    ThreadLocal.ThreadLocalMap threadLocals = null;

}

//ThreadLocal 类
public class ThreadLocal<T> {

    //先看set方法
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            //调用的ThreadLocalMap的set方法
            map.set(this, value);
        else
            createMap(t, value);
    }

    //getMap方法就是直接返回Thread中的ThreadLocalMap成员变量
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    //get方法
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            //首先先得到Entry也就是以ThreadLocal为key，value为value的保存数据的对象
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                //说明弱引用还没有被回收
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }

        //返回 之前实例化ThreadLoca对象时， 重写`protected String initialValue()`设置的默认值
        return setInitialValue();
    }

}

```

<br>

下面是`ThreadLocal.ThreadLocalMap`的源码：

```java
// 先看看保存ThreadLocal弱引用和value对象的Entry
static class Entry extends WeakReference<java.lang.ThreadLocal<?>> {
    // 往ThreadLocal里实际塞入的值
    Object value;

    Entry(java.lang.ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}

static class ThreadLocalMap {
    /**
        * The initial capacity -- MUST be a power of two.
        */
    private static final int INITIAL_CAPACITY = 16;

    /**
        * The table, resized as necessary.
        * table.length MUST always be a power of two.
        */
    private Entry[] table;

    /**
        * The number of entries in the table.
        */
    private int size = 0;

    /**
        * The next size value at which to resize.
        */
    private int threshold; // Default to 0

    /**
        * Set the resize threshold to maintain at worst a 2/3 load factor.
        */
    private void setThreshold(int len) {
        threshold = len * 2 / 3;
    }

    private static int nextIndex(int i, int len) {
        return ((i + 1 < len) ? i + 1 : 0);
    }

    /**
    * 构造一个包含firstKey和firstValue的map。
    * ThreadLocalMap是惰性构造的，所以只有当至少要往里面放一个元素的时候才会构建它。
    */

    ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
        // 初始化table数组
        table = new Entry[INITIAL_CAPACITY];
        // 用firstKey的threadLocalHashCode与初始大小16取模得到哈希值
        int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
        table[i] = new Entry(firstKey, firstValue);
        // 设置节点表大小为1
        size = 1;
        // 设定扩容阈值
        setThreshold(INITIAL_CAPACITY);
    }

    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     * 
     */
    private final int threadLocalHashCode = nextHashCode();
}

```

从上面可以看到，`ThreadLocalMap`是通过一个环形的数组保存Entry对象的。Entry对象中保存了ThreadLocal的弱引用对象，和value。并且这个数组的负载因子的2/3，扩容是*2。table结构看起来如下：

![|center](https://images2015.cnblogs.com/blog/584724/201705/584724-20170501020337211-761293878.png)

另外关于`threadLocalHashCode`: 每个ThreadLocal创建的时候，都会生成一个threadLocalHashCode与这个ThreadLocal对应，当在ThreadLocalMap中存储的ThreadLocal对象的时候，会根据`key.threadLocalHashCode & (len-1)`（就是根据此hashcode对table长度求余）来计算该ThreadLocal对象在tabel中存储的位置。如果碰撞了，就往后找，找到第一个剩余的空间，放进去。我们将这种解决hash冲突的方法叫做`线性探测`

<br>

下面看如何存值、取值的：

```java
private void set(ThreadLocal<?> key, Object value) {

    // We don't use a fast path as with get() because it is at
    // least as common to use set() to create new entries as
    // it is to replace existing ones, in which case, a fast
    // path would fail more often than not.

    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);

    //因为是根据哈希算法算出index 然后再线性探测
    for (Entry e = tab[i];
            e != null;
            e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();

        //找到了
        if (k == key) {
            e.value = value;
            return;
        }

        //替换失效的entry
        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    tab[i] = new Entry(key, value);
    int sz = ++size;
    //是否需要清理，以及是否需要rehash
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}

private Entry getEntry(ThreadLocal<?> key) {
    int i = key.threadLocalHashCode & (table.length - 1);
    Entry e = table[i];
    // 对应的entry存在且未失效且弱引用指向的ThreadLocal就是key，则命中返回
    if (e != null && e.get() == key)
        return e;
    else
        // 没有找到，但是很可能存到了后面，所以去找找看
        return getEntryAfterMiss(key, i, e);
}
```

<br>

最后，new 一个 ThreadLocal对象的时候`nextHashCode()`到底怎么实现怎么来的，对于整个Hash是怎么优化的，怎么清理ThreadLocal弱引用失效后Entry中存储的value的，hash冲突之后，具体怎么线性探测的，怎么rehash的，这里就不细细研究了，可以参考[ThreadLocal源码解读](https://www.cnblogs.com/micrari/p/6790229.html)


----------

## 锁

[深入理解多线程（五）—— Java虚拟机的锁优化技术](http://www.hollischuang.com/archives/2344)

<br>

-------

### 剖析同步器



<br>

--------

## 非阻塞算法

CAS?


<br>

--------

## 阿姆达尔定律

---------

参考文献：
1. [Java并发性和多线程介绍目录](http://ifeve.com/java-concurrency-thread-directory/)
2. [深入理解java内存模型系列文章](http://ifeve.com/java-memory-model-0/) 和 [《成神之路-基础篇》JVM——Java内存模型(已完结)](http://www.hollischuang.com/archives/1003) 结合起来看
3. [为什么能有上百万个Goroutines，却只能有上千个Java线程？](http://www.infoq.com/cn/articles/a-million-go-routines-but-only-1000-java-threads?utm_campaign=rightbar_v2&utm_source=infoq&utm_medium=articles_link&utm_content=link_text)