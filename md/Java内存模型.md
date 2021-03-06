# Java内存模型
@(深入理解Java)[内存模型,Java,as-if-serial,happens-before,final]


**`Java内存模型(JMM)`规范了Java虚拟机与计算机内存是如何协同工作的。Java虚拟机是一个完整的计算机的一个模型，因此这个模型自然也包含一个内存模型——又称为`Java内存模型`。**

> 注意和`Java运行时内存`区分开。

想设计表现良好的并发程序，理解Java内存模型是非常重要的。Java内存模型**规定了如何和何时可以看到由其他线程修改过后的共享变量的值，以及在必须时如何同步的访问共享变量。**

<br>

-------

## Java内存模型内部原理

![|left](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-1.png)![|right](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-2.png)

第一张图演示了Java内存模型吧Java虚拟机内部划分为线程栈和堆。（具体就不细讲了，可以看`Java运行时内存`）

第二张图演示了调用栈的本地变量放在线程栈上，对象放在堆上。

我们从`Java运行时内存`知道，线程栈上只能存储`boolean,byte,char,short,int,float,reference,returnAddress`这些类型。

总结一下：
- 一个本地变量可能是基本类型，在这种情况下，它总是“呆在”线程栈上。

- 一个本地变量也可能是指向一个对象的一个引用`reference`。在这种情况下，引用（这个本地变量）存放在线程栈上，但是对象本身存放在堆上。

- 一个对象可能包含方法(这个方法又可以看做是一个新的线程栈帧)，这些方法可能包含本地变量。这些本地变量任然存放在线程栈上，即使这些方法所属的对象存放在堆上。

- 一个对象的成员变量可能随着这个对象自身存放在堆上。不管这个成员变量是原始类型还是引用类型。

- 静态成员变量跟随着类定义一起也存放在堆上。

- 存放在堆上的对象可以被所有持有对这个对象引用的线程访问。当一个线程可以访问一个对象时，它也可以访问这个对象的成员变量。如果两个线程同时使用同一个对象上的同一个成员变量，它们将会都拥有这个成员变量的私有拷贝。

<br>

> **上文说的本地变量就存放在Java虚拟机栈线程帧栈中的局部变量表中**


如下图所示：

![|center](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-3.png)

两个线程拥有一些的本地变量。其中一个本地变量（Local Variable 2）指向堆上的一个共享对象（Object 3）。这两个线程分别拥有同一个对象的不同引用。这些引用都是本地变量，因此存放在各自线程的方法帧栈上的局部变量表中。这两个不同的引用指向堆上同一个对象。

![|center](http://ifeve.com/wp-content/uploads/2013/01/113.png)

比如说Object4是Object3的一个基本类型的成员变量，两个线程的methodOne都要去访问这个变量，那么在两个线程的methodOne方法帧栈的局部变量表中，都会存在该成员变量的一份拷贝。而这一份存在虚拟机栈中的方法帧栈中的局部变量表中的本地变量对其他线程都是不可见的，直到刷新回主存（也就是堆内存），再被其他线程访问时，才能看到该值被修改了。所以如果这时候一个线程从堆内存上读到本地变量中，在刷新回主存之前另一个线程又去读取并修改并刷回主存，就出现了多线程操作导致数据不一致的情况。


<br>

**`如何保证线程修改对其他线程的可见性`**

说白了就是 如何保证线程修改了主内存（堆）的内容及时刷新回主内存上，在其他线程访问的时候，能读到最新的值得问题。

在Java中提供了两种方案：
- `volatile` 使用volatile声明的成员变量，对他的修改会立刻同步到主存上，就是说可以保证，这次修改之后发生的读操作，一定能读到被修改之后的值
- `synchronize` 同步块。同步块保证，同一时间只能有一个线程进入，并且在退出同步块之前，需要把数据同步回主存。


<br>

**`happens-before`**

Java语言中有一个**`先行发生（happen—before）`**的规则，**它是Java内存模型中定义的两项操作之间的偏序关系**

**如果操作A先行发生于操作B，其意思就是说，在发生操作B之前，操作A产生的影响都能被操作B观察到，“影响”包括修改了内存中共享变量的值、发送了消息、调用了方法等，它与时间上的先后发生基本没有太大关系。这个原则特别重要，它是判断数据是否存在竞争、线程是否安全的主要依据。**

`happen-before`定义了许多的规则，在并发编程中用的比较多的就是下面几条：

- 程序顺序规则：一个线程中的每个操作，happens-before于线程中的任意后续操作。
- 监视器锁规则：一个锁的解锁，happens-before于随后对这个锁的加锁。
- volatile变量规则：对一个volatile域的写，happens-before于任意后续对这个volatile域的读。
- 传递性：如果A happens-before B，且Bhappens-before C，那么Ahappens-before C。

另外还有**线程启动，线程终止**等规则，具体可以参考[Java中的happen-before规则](https://blog.csdn.net/zhaojw_420/article/details/70477874)

> 注意：**一个操作时间上先发生于另一个操作“并不代表”一个操作happen—before另一个操作;一个操作happen—before另一个操作“并不代表”一个操作时间上先发生于另一个操作**

`happen-before`指的是两个操作之间的可见性问题，所以就可以简单的理解为：`A操作happen-beforeB操作，那么A操作的结果对B可见，但是并没有规定在时间上谁先谁后`

<br>

**`as-if-serial`**

`as-if-serial`语义的意思是：**不管怎么重排序（编译器和处理器为了提高并行度），（单线程）程序的执行结果不会改变。**就是说在单线程中，不管你怎么重排，对我们来说都不能有什么影响。就是说也存在，重拍两个不相关操作的顺序



<br>

---------

## 硬件内存架构

现代硬件内存模型与Java内存模型有一些不同。理解内存模型架构以及Java内存模型如何与它协同工作也是非常重要的。这部分描述了通用的硬件内存架构，下面的部分将会描述Java内存是如何与它“联手”工作的

下面是现代计算机硬件架构的简单图示：

![|center](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-4.png)

一个现代计算机通常由两个或者多个CPU。其中一些CPU还有多核。

每个CPU都包含一系列的寄存器，它们是CPU内内存的基础。CPU在寄存器上执行操作的速度远大于在主存上执行的速度。这是因为CPU访问寄存器的速度远大于主存。

每个CPU可能还有一个CPU缓存层。实际上，绝大多数的现代CPU都有一定大小的缓存层。CPU访问缓存层的速度快于访问主存的速度，但通常比访问内部寄存器的速度还要慢一点。一些CPU还有多层缓存。

通常情况下，当一个CPU需要读取主存时，它会将主存的部分读到CPU缓存中。它甚至可能将缓存中的部分内容读到它的内部寄存器中，然后在寄存器中执行操作。当CPU需要将结果写回到主存中去时，它会将内部寄存器的值刷新到缓存中，然后在某个时间点将值刷新回主存。


<br>

---------

## Java内存模型和硬件内存架构之间的桥接

上面已经提到，Java内存模型与硬件内存架构之间存在差异。硬件内存架构没有区分线程栈和堆。对于硬件，所有的线程栈和堆都分布在主内中。部分线程栈和堆可能有时候会出现在CPU缓存中和CPU内部的寄存器中。如下图所示：

![|center](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-5.png)


<br>

-------------

## 双重校验锁与final关键字

`双重校验锁`的标准实现

```java
public class LazySingleton {
    private int                  someField;
    private static volatile LazySingleton instance;

    private LazySingleton(){
        this.someField = new Random().nextInt(200) + 1; // (1)
    }

    public static LazySingleton getInstance() {
        if (instance == null) {// (2)
            synchronized (LazySingleton.class) { // (3)
              if (instance == null) { // (4)
                instance = new LazySingleton(); // (5)
              }
            }
        }
        return instance; // (6)
    }

    public int getSomeField() {
        return this.someField;  // (7)
    }
}

```

整个流程是这样的：

1. 同时来了一万个线程，9000个阻塞在3，1000个才执行到2
2. 有一个线程获得了锁，进入了同步块，初始化了singleton对象，释放锁。
3. 又有一个线程获取到了锁，发现已经初始化完毕，直接返回，之后其他线程也都是这样。

上面的过程保证了单例。

**可是，我有问题！！，volatile 到底有个软用？！**

因为在同步块释放锁之前，已经将本地内存的数据同步到主存了，其他的线程才可见，之后的操作

经过仔细的思考，我好像找到了volatile的作用了，`防止指令重排！！`

5步骤创建对象涉及到很多条指令，并不是一个原子操作，2步骤和5步骤掺杂执行，根据JMM Java内存模型，在5步骤monitor释放class对象锁之前，这些操作都是线程内的本地操作，根据happen-before原则，在释放锁之前，还需要将本地内存数据同步到主存。也就是说，这整个同步块创建对象的操作涉及到以下指令
1. n多个创建对象的指令（下面再详细说创建对象的整个流程）
2. 将本地内存数据同步到主存
3. 释放锁

我认为，1、2步骤中涉及的指令并没有明确的顺序规定，而JVM应该可以保证3步骤释放锁的指令是在最后执行的，happen-before规则定义了获取锁的操作发生在释放锁之后，但是代码中的2步骤(顺序上)是在获取锁之前的，也就是说2步骤和5步骤的指令执行完全是掺杂着乱进行的。所以！！就存在这么一种情况，步骤5中的指令执行了分配空间指令、将指针指向该空间的指令和刷新回主存的指令，但是并没有执行初始化的指令，而这时候，代码中2步骤判断该对象已经不为null了，就返回了，而实际上，这个对象还没有初始化，其他线程使用就可能出现问题。。但是阻塞带步骤3的线程，在获取到锁之后，肯定能保证整个同步块的指令已经全部执行完毕，也当然能保证单例对象完全创建完成。而如果有volatile，整个创建对象的指令不会被重排，按照指令顺序，创建完了，然后才刷新回主存，这时候就算还没有释放锁，步骤2的线程得到了该对象，但是确实已经初始化完了，这应该就是volatile关键字的作用。

### 创建对象的流程

以下过程参考自《深入理解Java虚拟机》，JDK版本是1.7

另外整个过程讨论的是普通Java对象，不涉及Class对象和数组的创建。

1. 检查类是否加载、解析初始化过，如果没有，先加载类。
2. 检查通过后，就已经可以根据类信息为对象申请确定的内存空间。并修改指针（这两个操作并不是同步的，我也不知道JVM内部是否将这两个操作同步，同步方案有两种：一是CAS，而是内存的分配动作按照线程划分在不同的空间汇总进行，每个线程在Java堆上先分配一小块内存，叫本地线程分配缓冲（TLAB），只有TLAB用完分配新的TLAB是才需要同步。）
3. 分配的内存空间都初始化零值。就是说对象的成员变量都分配给对应类型的零值，不初始化也可以直接使用，不会报什么没有初始化的错误。（但是方法内的本地变量就需要先初始化才能用，因为虚拟机没有默认帮他初始化）
4. 对对象设置必要信息，包括类的元数据地址、对象的HashCode、GC分代年龄等，这些都放在对象头中
5. 从虚拟机的角度来讲，对象已经创建完了，但是对于程序来讲，对象差创建才刚刚开始，还没有执行\<init\>方法，开始执行init方法，也就是将变量的复制操作和构造函数整合起来的方法。



<br>

---------

参考自：
1. [Java内存模型系列文章](http://ifeve.com/java-memory-model-0/)
2. [Java内存模型](http://ifeve.com/java-memory-model-6/)（这是上面目录中少的这一篇）
3. [JVM内存结构 VS Java内存模型 VS Java对象模型](http://www.hollischuang.com/archives/2509)
4. [再有人问你Java内存模型是什么，就把这篇文章发给他。](http://www.hollischuang.com/archives/2550)
5. [Java运行时内存](https://mp.weixin.qq.com/s/VDt9sVH4l_FKKAy-uqTjYg)

