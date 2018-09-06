# Building Block

该章主要讲构建同步容器、并发容器所遇到的问题。

## 并发容器

我们都知道Vector是线程安全的，继承了List接口，拥有List的所有方法。但是，如果我们用到了一些复合操作，比如说线程池中用来存放线程，但是线程池是有大小的，add操作的时候需要先通过size方法获取长度，如果没有超过最大长度，则添加，这时候，添加操作就变成了复合操作，在多线程的时候就会出现竞争，导致数据不一致。

大致的操作可以有，迭代、导航（根据一定的顺序寻找下一个元素）、条件运算（如，缺少就加入，检查再运行等）。

### 自己构建并发容器

简单点，可以通过下列方式加锁：
```java
public static Object getLast(Vector list) {
    synchronized(list) {
        int lastIndex = list.size() - 1;
        return list.get(lastIndex);
    }
}

public static void traverse(Vector list, Hock h) {
    synchronized(list) {
        for (int i=0; i<list.size(); i++){
            h.doSomething(list.get(i));
        }
    }
}
```

> 注意：这里的加的同步锁一定要是`list`，而不能是类似`this`这样的类，因为`Vector`内部通过的时候，使用的锁就是本身，如果这里加的锁和`Vector`中使用的不是一个锁，那么还是不能保证线程安全。

#### 迭代器 与 ConcurrentModificationException

对Collection的标准迭代方式是`Iterator`，不管是显示的用`collection.iterator()`还是隐式的用`foreach`，都是使用的迭代器方式来遍历的。

下面这种方式，并没有使用迭代器，所以也不会抛出异常：
```java
for (int i=0; i<list.size(); i++){
    if (i % 2 == 0) {
        list.remove(i);
    }
}
```


> 也要注意方法中隐藏的使用了迭代器，如toString、hasCode、equals、containsAll、removeAll、retainAll方法，或者把容器作为构造函数的参数，都会对容器进行迭代。

当容器正在使用迭代器的时候，容器被修改，那么就会抛出`ConcurrentModificationException`异常。容器被修改包括单线程的在**迭代过程中修改容器**（如Test1.test1）；也包括其他线程对容器的修改。Java中，在设计同步容器返回的迭代器时，并没有考虑到并发修改的问题，也是会抛出该异常的。

怎么解决这个问题呢，简单点，就是在迭代期间对容器加锁，但是，当容器比较大的时候，又比较影响性能。替代方法是复制容器（复制期间，也需要加锁），但是这样也有内存上的开销。

### java中的并发容器

java提供了同步容器，如`Collections.synchronizedList(list)`返回的`SynchronizedList`(该类是内部包类)等。

另外，java还提供了复合操作的并发容器，如`ConcurrentHashMap`, `CopyOnWriteArrayList`, 传统的FIFO队列`ConcurrentLinkedQueue`, 用来代替SortedSet 和 SortedMao 的`ConcurrentSkipListSet`和`ConcurrentSkipListMap`等等。

#### ConcurrentHashMap

通常的同步容器类，比如`Collection.synchronizedMap`返回的类，每个操作的执行期间都会持有一个锁，有一些耗时操作，比如contains，equals等，如果该map中的元素的hashcode并没有很好的分布，最极端的就是这个map其实是一个链表，遍历所有的元素并调用equals方法，会花费很长的时间，而在这期间，其他线程都不能访问这个容器。

`ConcurrentHashMap`一样是一个哈希表，但是使用了完全不同的锁策略，可以通过更好的并发性和可伸缩性。他使用了更加细化的锁机制，叫**`分离锁`**。这个机制允许更深层次的共享访问。**任意数量的读线程可以并发访问map，读者和写者也可以并发访问map，并且有限数量的写线程还可以并发修改map**。

而且，**提供了不会抛出ConcurrentModificationException的迭代器，不需要再容器迭代中加锁。ConcurrentHashMap返回的迭代器具有`弱一致性`。**
> **弱一致性的迭代器**，容许并发修改，当迭代器被创建时(xxx.iterator())，会遍历以后的元素，并且可以（但是不保证）感应到在迭代器被创建后，对容器的修改。

缺点，一些对整个map的操作，如size、isEmpty，它们的语义在反应容器并发特性上并弱化了。因为size的结果相对于在计算的时刻可能已经过期了，它仅仅是个估算值。而且，像size、isEmpty这样的方法在并发环境下几乎没有什么用处，因为map一直是运动的。所以对这些操作的需求就被弱化了，并且对重要的操作进行了性能调优，包括get、put、containsKey、remove等

缺点，同步map比如`Collection.synchronizedMap`返回的类，提供的一个特性是独占访问锁（我访问的时候，其他任何人都不能访问），在`ConcurrentHashMap`中并没有实现。但是他的其他任何方面都远比同步map有巨大优势，所以只有程序需要独占访问时，`ConcurrentHashMap`才无法胜任。

也因为`ConcurrentHashMap`不能够独占访问，所以也不适合为其创建新的原子复合操作。比如之前说道的`Vector`的缺少即加入等操作，而且`ConcurrentHashMap`也提供了常见的复合操作。

#### CopyOnWriteArrayList

`CopyOnWriteArrayList` 是同步List的一个并发替代品，**通常情况下提供了更好的并发性，并避免了在迭代期间对容器加锁和复制**

> `CopyOnWriteArraySet` 是同步Set的一个并发替代品。

**写入时复制（copy）**，容器的线程安全性来源于此，只有有效的不可变对象被正确发布，那么访问将不再需要更多的同步（读）。每次修改时，会创建并重新发布一个新的容器拷贝，以此来实现可变性（写，可以想想Elixir）。

写入时复制的容器，会保留一个底层基础数组的引用。这个数组作为迭代器的起点，永远不会被修改，`因此对他的同步只是为了确保数组内容的可见性（及时发布？）`。因此多个线程可以对这个容器进行迭代，而且不会受到另一个或多个想要修改的线程带啦IDE干涉。返回的迭代器也不会抛出`ConcurrentModificationException`，而且`返回的元素严格与迭代器创建时相一致，不会烤炉后续的修改`。

显而易见，每次修改容器是复制基础数组需要一定的开销，特别是容器比较大的时候。所以也需要我们权衡。

### 阻塞队列 和 生产者-消费者模式

`阻塞队列 (BlockingQueue)`-- 增加了可阻塞的插入和获取操作。如果队列是空的，一个获取操作会一直阻塞到队列中存在可用的元素，如果队列是满的（对于有界队列），插入操作会一直阻塞到队列中存在可用空间。

阻塞队列支持`生产者消费者模式`。一个生产者-消费者设计，分离了派发任务者、消费任务者和任务之间的耦合关系，当有新的任务产生是，生产者就将任务发布到消息队列中，不需要管理任务是否被处理还是怎么了，而消息队列仅仅是接收和分发任务，不关系谁给的也不关系给谁，而消费者仅仅是从任务队列中拿到并处理任务，没有任务的时候，自己就歇着。这种设计模式围绕着阻塞队列展开。

java类库中有很多`BlockingQueue`的实现。
- `LinkedBlockingQueue`和`ArrayBlockingQueue`是FIFO队列。一个是链表的实现方式 ，一个是数组的实现方式。
- `PriorityBlockQueue` 是一个按优先级排序的队列（如果不希望是FIFO的）。可以按照元素的自然顺序（如实现了`Comparable`的类），也可以使用`Comparator`进行排序。
- `SynchronousQueue` 可以说不是一个真正意义上的队列。因为它不会为队列元素维护任何存储空间。和其他的相比，就像是吧文件直接递给你的同事，还是把文件直接发送到他的邮箱期待他一会可以得到文件之间的不同。`SynchronousQueue`没有存储能力，除非有另外一个消费者线程已经准备好处理他了，否则put和take会一直阻塞。这种队列只有在消费者充足的时候比较合适。

`连续的线程限制`：`java.util.concurrent`包中的阻塞队列，全部都包含充分的内部同步，从而能安全的将对象从生产者线程发布至消费者线程。生产者-消费者模式和阻塞队列一起，为生产者和消费者之间移交的对象所有权提供了**连续的线程限制**。在整个生产消费过程当中，对象完全由单一线程所拥有，这个线程可以任意修改，因为它具有独占访问权。

对象池扩展了连续的线程限制。只要保证了对象池的充分同步，对象的所有权就可以在线程间安全地传递。

#### 双端队列和窃取工作

在Java6中增加了两个容器类型，`Deque`和`BlockingDeque`。他们分别扩展了`Queue`和`BlockingQueue`。他是一个双端队列，`ArrayDeque`和`LinkedBlockingDeque`实现了他们。

`窃取工作模式` -- 双端队列的特性与一种叫做**窃取工作模式**的模式相关联。每一个消费者都有一个自己的双端队列。如果一个消费者完成了自己的双端队列的全部工作，它可以偷取其他消费者的双端队列中的**末尾**任务。

------------

## 阻塞与可中断

[线程的状态](https://github.com/root-wyj/java_concurrency/blob/master/thread-state.md)

关于中断，还是理解的不太深，所以此次先不讨论，以后再说。中断的两篇文章https://www.cnblogs.com/hapjin/p/5450779.html，http://blog.csdn.net/canot/article/details/51087772。

线程可能会因为几种原因被阻塞或暂停，等待IO操作结束，等待获得一个锁，等待从Thread.sleep中唤醒，或是等待另一个线程的计算结果。当一个线程阻塞时，通常会被挂起，并设置成线程阻塞的某个状态（BLOCKED、WAITING或TIMED_WAITING）。

BlockingQueue的put和take是阻塞的方法，会抛出InterruptedException。如果它被中断，将可以提前结束阻塞状态。线程也提供了interrupt方法，来中断一个线程。

> 线程在阻塞的状态下调用interrupt方法会抛出InterruptedException异常，而在正常运行的情况下是不会抛出异常的。interrupt方法会将线程的中断标志位设置为true，但是如果调用interrupt方法的时候，线程被阻塞或挂起，线程无法响应，无法正确设置标志位，所以抛出异常。

中断是一种协作机制，一个线程不能迫使其他线程停止正在做的事情，或者去做其他事情。当线程A中断B时，A仅仅要求B在达到某个方便停止的关键点时，停止正在做的事情。从时间角度看，这样有一定的延迟，相应中断的阻塞方法（抛出的InterruptedException异常？暂时是这么理解的），可以更容易地取消耗时活动。如何相应，有两种基本选择：

 - **传递InterruptedException** 把异常传递给调用者。可能不捕获；也可能捕获，做完处理，再抛出
 - **恢复中断** 有时候不能抛出该异常，比如代码是Runnable的一部分的时候。这时候必须捕获异常，并在当前线程调用interrupt恢复中断状态。就是再次设置线程的状态为中断状态。如示例Test1.test3中，t2中断了t1，t1捕获了异常之后，又重新设置了中断状态。

### 怎么理解中断

其实我自己有点理解不了中断，我不知道为什么会需要中断，何时才会用到中断。

中断线程的使用场景：
在某个子线程中为了等待一些特定条件的到来, 你调用了Thread.sleep(10000), 预期线程睡10秒之后自己醒来, 但是如果这个特定条件提前到来的话, 来通知一个处于Sleep的线程。又比如说.线程通过调用子线程的join方法阻塞自己以等待子线程结束, 但是子线程运行过程中发现自己没办法在短时间内结束, 于是它需要想办法告诉主线程别等我了. 这些情况下, 就需要中断。

**没有任何语言方面的需求要求一个被中断的程序应该终止。中断一个线程只是为了引起该线程的注意，被中断线程可以决定如何应对中断 ** -- 来自 Core Java。

中断的经典使用代码：

```java
 //Interrupted的经典使用代码    
    public void run(){    
            try{    
                 ....    
                 while(!Thread.currentThread().isInterrupted()&& more work to do){    
                        // do more work;    
                 }    
            }catch(InterruptedException e){    
                        // thread was interrupted during sleep or wait    
            }    
            finally{    
                       // cleanup, if required    
            }    
    }

```

> 不是所有的阻塞方法收到中断后都可以取消阻塞状态, 输入和输出流类会阻塞等待 I/O 完成，但是它们不抛出 InterruptedException，而且在被中断的情况下也不会退出阻塞状态. 
尝试获取一个内部锁的操作（进入一个 synchronized 块）是不能被中断的，但是 ReentrantLock 支持可中断的获取模式即 tryLock(long time, TimeUnit unit)。

----------

## Synchronizer -- 同步器

`Synchronizer`指的是一个对象，它根据本身的状态调节线程的控制流。比如说阻塞队列就可以扮演一个Synchronizer的角色。其他类型的`Synchronizer`有`闭锁(latch)`、`信号量(semaphore)`、`关卡(barrier)`。


### 闭锁 -- latch

**`闭锁`可以用来确保特定活动直到其他活动完成后才发生。**一个闭锁就像是一道大门，直到闭锁到达终点之前，门一直都是关闭的，没有线程能通过。在终点状态到来的时候，门开了，允许所有的线程通过。一旦到达终点状态就不能再改变状态了，会永远保持敞开的状态。

Java中，`CountDownLatch`是一个灵活的闭锁实现。

`CountDownLatch`的状态只有一个计数器，初始化时需要一个正数，用来表示需要等待的线程数。`countDown`方法对计数器做减操作，表示一个事件已经发生。`await`会一直阻塞直到计数器为零，或者等待线程中断及超时。

`TSTLatch.java` 演示了它的使用方法。

### FutureTask

`FutureTask`的实现描述了一个抽象的可携带结果的计算。等价于一个可以携带结果的Runnable，并且有三个状态：等待、运行和完成。完成包括所以计算以任意方式结束，正常的取消的异常的。一旦`FutureTask`进入完成状态，会永远停止在这个状态上。

`Future.get`依赖于任务的状态，如果已完成，get可以立刻返回结果，否则会阻塞到完成状态，然后返回结果或者抛出异常。`FutureTask`将计算结果从计算的线程传送到需要这个结果的线程。它的规约保证了这种传递建立在结果的安全发布之上。

`TSTFutureTask` 演示了它的使用方法。

### 信号量 -- Semaphore

**`计数信号量（Counting Semaphore）`用来控制能够同时访问某特定资源的活动的数量，或者同时执行某一给定操作的数量。 计数信号量可以用来实现资源池或给一个容器限定边界。**

使用`Semaphore`可以把任何容器转化为有界的阻塞容器。`BoundedHashSet`演示了它的使用方法

### 关卡 -- CyclicBarrier

`CyclicBarrier` 的字面意思是可循环使用（Cyclic）的屏障（Barrier）。它要做的事情是，让一组线程到达一个屏障（也可以叫同步点）时被阻塞，直到最后一个线程到达屏障时，屏障才会开门，所有被屏障拦截的线程才会继续干活。

当线程到达关卡点时，调用await方法，await会被阻塞，直到所有的线程都到达关卡点。如果所有的线程都到达了关卡点，关卡就会被突破，这样所有的线程都被释放，关卡会重置以备下一次使用。如果对await的方法调用超时，或者阻塞中的线程被中断，那么关卡就被认为是失败的，所有对await未完成的调用都通过`BrokenBarrierException`终止。

`CyclicBarrier` 默认的构造方法是 `CyclicBarrier(int parties)`，其参数表示屏障拦截的线程数量，每个线程调用await方法告诉 `CyclicBarrier `我已经到达了屏障，然后当前线程被阻塞。

更详细的可以参考：[【Java并发编程四】关卡](https://www.cnblogs.com/xujian2014/p/5363759.html)

`CyclicBarrier`还支持更加复杂的操作和情况。

































