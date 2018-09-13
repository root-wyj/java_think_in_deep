# ConcurrentHashMap

## 基本原理

在Java1.8之后，`ConcurrentHashMap`迎来了一次更新。使用了更加高效的同步策略。

**摒弃了之前Segment使用的分段锁的技术，启用了CAS+synchronized技术保证多线程并发更新的安全。**

<br>

----------

### 1.7 版本

结构图：

![|center](https://ws4.sinaimg.cn/large/006tNc79gy1ftj0evlsrgj30dw073gm2.jpg)

**由Segment数组，HashEntry组成。和老版本的HashMap一样，都是数组加链表。**

```java
/**
 * Segment 数组，存放数据时首先需要定位到具体的 Segment 中。
 */
final Segment<K,V>[] segments;

/**
 * Segment 内部类
 */
static final class Segment<K,V> extends ReentrantLock implements Serializable {
    // 和 HashMap 中的 HashEntry 作用一样，真正存放数据的桶
    transient volatile HashEntry<K,V>[] table;

    transient int count;

    transient int modCount;

    transient int threshold;

    final float loadFactor;
}

static class HashEntry<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile HashEntry<K,V> next;

    HashEntry(int hash, K key, V val, HashEntry<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    ...
}
```

`原理上来说：ConcurrentHashMap 采用了分段锁技术，其中 Segment 继承于 ReentrantLock。不会像 HashTable 那样不管是 put 还是 get 操作都需要做同步处理，理论上 ConcurrentHashMap 支持 CurrencyLevel (Segment 数组数量)的线程并发。每当一个线程占用锁访问一个 Segment 时，不会影响到其他的 Segment。`


<br>
put方法分析：

首先根据一定的hash算法找到对应的segment，调用segment的对象的put方法，在Segment.put方法中会加锁，加锁的时候如果发现有人在使用了，首先使用的自旋锁，如果自旋一会儿还没有申请到，就会变成阻塞锁。
再然后，在Segment的HashEntry table中通过hashcode定位到HashEntry，如果为空，直接写入，相等，覆盖；否则新建一个HashEntry，加入到Segment中，还要判断扩容等问题，最后操作完成，释放锁。

set方法分析：

由于 HashEntry 中的 value 属性是用 volatile 关键词修饰的，保证了内存可见性，所以每次获取时都是最新值。
ConcurrentHashMap 的 get 方法是非常高效的，因为整个过程都不需要加锁。



<br>

----------

### 1.8 版本

组成结构：

![|center](https://ws3.sinaimg.cn/large/006tNc79gy1fthpv4odbsj30lp0drmxr.jpg)

**首先结构上与1.8HashMap有一些类似，都是使用了数组+链表+红黑树的实现。另外抛弃了Segment分段锁，采用了`CAS+synchronized`来保证并发安全性**


<br>

#### 重要属性

```java

    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     * 盛装Node元素的数组 它的大小是2的整数次幂 懒初始化，在第一次put的时候才初始化。
     */
    transient volatile Node<K,V>[] table;


	/**
     * Table initialization and resizing control.  When negative, the
     * table is being initialized or resized: -1 for initialization,
     * else -(1 + the number of active resizing threads).  Otherwise,
     * when table is null, holds the initial table size to use upon
     * creation, or 0 for default. After initialization, holds the
     * next element count value upon which to resize the table.
     * hash表初始化或扩容时的一个控制位标识量。（非常重要的一个变量，控制整个流程）
     * 
     * 负数代表正在进行初始化或扩容操作
     *  -1代表正在初始化
     *  -N 表示有N-1个线程正在进行扩容操作
     * 正数或0代表hash表还没有被初始化，这个数值表示初始化或下一次进行扩容的大小
     
     */
    private transient volatile int sizeCtl; 
    // 以下两个是用来控制扩容的时候 单线程进入的变量
     /**
     * The number of bits used for generation stamp in sizeCtl.
     * Must be at least 6 for 32bit arrays.
     */
    private static int RESIZE_STAMP_BITS = 16;
		/**
     * The bit shift for recording size stamp in sizeCtl.
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;


    static final int MOVED     = -1; // hash值是-1，表示这是一个forwardNode节点
    static final int TREEBIN   = -2; // hash值是-2  表示这时一个TreeBin节点

    /**
     * The next table to use; non-null only while resizing.
     * 扩容时 使用
     */
    private transient volatile Node<K,V>[] nextTable;

    /**
     * Table of counter cells. When non-null, size is a power of 2.
     * 用来计算 size的，无论怎样都是弱一致性，都是个估算值
     */
    private transient volatile CounterCell[] counterCells;
```

<br>

#### 重要内部类

`Node`:

包装了key-value键值对，所有的数据都包装在这里面。这个Node内部类与HashMap中定义的Node类很相似，但是有一些差别。
- `final hash; final key, volatile value; volatile next`。 key和hash都是用了final修饰 不可变，value 和 next使用了volatile修饰。
- 不允许调用setValue方法直接改变Node的value域
- 增加了find方法辅助map.get()方法


```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;//带有同步锁的value
    volatile Node<K,V> next;//带有同步锁的next指针

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    public final K getKey()       { return key; }
    public final V getValue()     { return val; }
    public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
    public final String toString(){ return key + "=" + val; }
    //不允许直接改变value的值
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public final boolean equals(Object o) {
        Object k, v, u; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
                (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                (v = e.getValue()) != null &&
                (k == key || k.equals(key)) &&
                (v == (u = val) || v.equals(u)));
    }

    /**
     * Virtualized support for map.get(); overridden in subclasses.
     */
    Node<K,V> find(int h, Object k) {
        Node<K,V> e = this;
        if (k != null) {
            do {
                K ek;
                if (e.hash == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
            } while ((e = e.next) != null);
        }
        return null;
    }
}

```

<br>

`TreeNode`

标准的红黑树节点。当链表长度过长的时候，会转换成TreeNode，链表会变成红黑树。

但并不是直接转换为红黑树，而是把这些结点包装成TreeNode放在TreeBin对象中，由TreeBin完成对红黑树的包装。而且TreeNode在ConcurrentHashMap继承自Node类，而并非HashMap中的继承自LinkedHashMap.Entry<K,V>类，也就是说TreeNode带有next指针，这样做的目的是方便基于TreeBin的访问。

<br>

`TreeBin`

这个类并不负责包装用户的key、value信息，而是包装的很多TreeNode节点。它代替了TreeNode的根节点，也就是说在实际的ConcurrentHashMap“数组”中，存放的是TreeBin对象，而不是TreeNode对象，这是与HashMap的区别。另外这个类还带有了读写锁

而且数组中该TreeBin对象的hashcode为`TREEBIN = -2`。

```java

```

<br>

`ForwardingNode`

这个节点比较特殊，hash值为`MOVED = -1`，存储nextTable的引用。主要再扩容时使用。他提供的find方法也是在新表中nextTable中查找。

<br>

---------------

#### 重要方法

`put方法：`

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        //1 第一次put，去初始化table
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        //2 table的这个地方没有值，直接放入
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null,
                            new Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        //3 在扩容，加入一起扩容
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            //4 往里面放，注意这里终于使用了同步 synchronized table的Node元素
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                    (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                            value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                        value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

put方法可以大致分为4种情况：
1. 第一次put，去初始化
2. table[]数组中为null，直接放进去
3. 正在扩容，那么就和其他线程一起扩容
4. 往里面放呗。

---------

`初始化`

```java
/**
    * Initializes table, using the size recorded in sizeCtl.
    */
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

初始化方法主要应用了关键属性`sizeCtl`的值。小于0说明其他线程正在初始化，则本线程放弃此次操作。

这里明显是会发生多线程的竞争的，这里通过CAS的方法设置volatile类型的变量`sizeCtl`的值，让后来的线程不能进入初始化代码执行的块，保证初始化只有一个线程进行。

`volatile`类型在只有一个线程有修改操作的时候，是线程安全的，而且他的修改也能马上被其他线程看见，在多线程修改操作的时候是不安全的，因为这种操作是`读-改-写`三个操作，而非一个原子操作，这期间，读出来的值可能就已经被修改了。但是这里的修改操作是原子操作，通过原子修改操作+volatile，保证其他线程不存在误读操作，最后也保证了进入初始化块的线程只有一个，也就是线程安全。

**`volatile+CAS`实现了线程安全。** 

其实这里只是一个例子，但其实在`concurrent`包中，这是经常使用的方法。

下面是初始化Unsafe和其他变量的静态代码块：
```java
// Unsafe mechanics
private static final sun.misc.Unsafe U;
private static final long SIZECTL;
private static final long TRANSFERINDEX;
private static final long BASECOUNT;
private static final long CELLSBUSY;
private static final long CELLVALUE;
private static final long ABASE;
private static final int ASHIFT;

static {
    try {
        U = sun.misc.Unsafe.getUnsafe();
        Class<?> k = ConcurrentHashMap.class;
        SIZECTL = U.objectFieldOffset
            (k.getDeclaredField("sizeCtl"));
        TRANSFERINDEX = U.objectFieldOffset
            (k.getDeclaredField("transferIndex"));
        BASECOUNT = U.objectFieldOffset
            (k.getDeclaredField("baseCount"));
        CELLSBUSY = U.objectFieldOffset
            (k.getDeclaredField("cellsBusy"));
        Class<?> ck = CounterCell.class;
        CELLVALUE = U.objectFieldOffset
            (ck.getDeclaredField("value"));
        Class<?> ak = Node[].class;
        ABASE = U.arrayBaseOffset(ak);
        int scale = U.arrayIndexScale(ak);
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
    } catch (Exception e) {
        throw new Error(e);
    }
}
```

<br>

`为null，直接放入table数组`

再仔细去上面看看这块代码，调用了两个静态方法：`tabAt、casTabAt`

```java

 @SuppressWarnings("unchecked")
//获得在i位置上的Node节点
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}
    //利用CAS算法设置i位置上的Node节点。之所以能实现并发是因为他指定了原来这个节点的值是多少
    //在CAS算法中，会比较内存中的值与你指定的这个值是否相等，如果相等才接受你的修改，否则拒绝你的修改
    //因此当前线程中的值并不是最新的值，这种修改可能会覆盖掉其他线程的修改结果  有点类似于SVN
static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                    Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}
    //利用volatile方法设置节点位置的值
static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
    U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);

```

这三个都是原子操作，而且使用了Unsafe直接操作主内存中的数据，所以这个操作也保证了对各个线程的可见性。

`casTabAt`该操作也支持并发，使用乐观锁更新数据。如果传进来的参数值不等于从内存中取到的值就操作失败，如果等于就操作成功，而且该方法的调用包裹在`for`的无限循环中，所以就算这次操作不成功，该数据在下一个循环中还是会被处理，虽然不是这种情况了。

这里通过`CAS+无限循环`实现的乐观锁来处理了高并发的情况，保证了多线程的操作不会出错。

<br>

`你往里面放阿`

这就比较简单了，首先获取table[]数组中该元素的锁，这样其他线程（无论是来放元素的，还是扩容，甚至修改链表到树的结构的）通通进不来，如果是链表，加到最后，如果是红黑树，调用`TreeBin.putTreeVal`方法加入到红黑树。

<br>

`最后部分的逻辑`

首先，在循环结束前，加入了元素之后，需要看看是不是将table[i]下的结构链表转换为树。

最后，循环结束，计数加1，并判断是否需要扩容。

<br>

`最后的最后，来说说扩容的问题`

扩容的基本思想还是和HashMap很像的，但是由于他支持真正的并发操作，所以复杂的多。因为扩容的时候总是会涉及到从一个“数组”到另一个“数组”拷贝的操作，如果这个操作能够并发进行，那真真是极好的了。利用并发处理去减少扩容带来的时间影响。

整个扩容操作分为两个部分

1. 构建一个nextTable,它的容量是原来的两倍，这个操作是单线程完成的。这个单线程的保证是通过RESIZE_STAMP_SHIFT这个常量经过一次运算来保证的，和sizeClt类似；
2. 将原来table中的元素复制到nextTable中，这里允许多线程进行操作

第二部分的并发可以分为以下几种情况：
- 遍历table，当前节点为null，采用CAS方式在当前节点放入`ForwardingNode`
- 当前节点是`ForwardingNode`，说明已经处理完了，那么进行下一个节点。
- 剩下这种情况就是链表或者红黑树了。那么先申请锁，进来之后先判断，是不是被改成`ForwardingNode`，然后构造顺序和逆序链表放在nextTable的i和i+n上。TreeBin节点也是类似。然后通过`Unsafe.putObjectVolatile`在tab的原位置赋为为fwd, 表示当前节点已经完成扩容

最后，复制工作完成，nextTable作为新的table，设置sizeCtl为新容量的0.75倍，完成扩容。


具体的代码就不贴了，很长。下面根据这张图来看一下：

![|center](https://github.com/root-wyj/java_think_in_deep/blob/master/md/images/concurrenthashmap_%E6%89%A9%E5%AE%B9.jpg)

详细的源码分析可以看[ConcurrentHashMap源码解析（jdk1.8）](https://blog.csdn.net/programmer_at/article/details/79715177#14-table-%E6%89%A9%E5%AE%B9)


> 还有两部分**`count`的计算**，`clear、delete`删除元素，没有仔细分析。但是应该都是大同小异，和put中和扩容中使用的思想都差不多。



<br>

-----------

## 和HashTable对比

**`注意：ConcurrentHashMap提供了弱一致性。HashTable或者是Collection.synchronizedMap返回的对象，都提供了强一致性`**

就是说：

- Hashtable的任何操作都会把整个表锁住，是阻塞的。好处是总能获取最实时的更新，比如说线程A调用putAll写入大量数据，期间线程B调用get，线程B就会被阻塞，直到线程A完成putAll，因此线程B肯定能获取到线程A写入的完整数据。坏处是所有调用都要排队，效率较低。
- `ConcurrentHashMap 是设计为非阻塞的。在更新时会局部锁住某部分数据，但不会把整个表都锁住。同步读取操作则是完全非阻塞的。好处是在保证合理的同步前提下，效率很高。坏处 是严格来说读取操作不能保证反映最近的更新。例如线程A调用putAll写入大量数据，期间线程B调用get，则只能get到目前为止已经顺利插入的部分 数据`

缺点，一些对整个map的操作，如size、isEmpty，它们的语义在反应容器并发特性上并弱化了。因为size的结果相对于在计算的时刻可能已经过期了，它仅仅是个估算值。而且，像size、isEmpty这样的方法在并发环境下几乎没有什么用处，因为map一直是运动的。所以对这些操作的需求就被弱化了，并且对重要的操作进行了性能调优，包括get、put、containsKey、remove等

缺点，同步map比如`Collection.synchronizedMap`返回的类，提供的一个特性是独占访问锁（我访问的时候，其他任何人都不能访问），在`ConcurrentHashMap`中并没有实现。但是他的其他任何方面都远比同步map有巨大优势，所以只有程序需要独占访问时，`ConcurrentHashMap`才无法胜任。

也因为`ConcurrentHashMap`不能够独占访问，所以也不适合为其创建新的原子复合操作。比如之前说道的`Vector`的缺少即加入等操作，而且`ConcurrentHashMap`也提供了常见的复合操作。


<br>

-----------


> attention!!! **`看源码是为了习得其中的编程思想： 比如说 初始化时的懒加载；使用CAS + while-true实现乐观锁，保证高并发；使用CAS + volatile保证某个代码块只执行一次；使用Unsafe和静态常量和静态代码块从内存上直接操作，保证操作的原子性和可见性；使用锁表元素的思想实现真正的高并发；使用多线程扩容技术；为保证高效，计算均使用移位操作`**


参考文献：
1. [HashMap? ConcurrentHashMap? 相信看完这篇没人能难住你！](http://ifeve.com/hashmap-concurrenthashmap-%E7%9B%B8%E4%BF%A1%E7%9C%8B%E5%AE%8C%E8%BF%99%E7%AF%87%E6%B2%A1%E4%BA%BA%E8%83%BD%E9%9A%BE%E4%BD%8F%E4%BD%A0%EF%BC%81/#more-39622)
2. [ConcurrentHashMap源码解析（jdk1.8）](https://blog.csdn.net/programmer_at/article/details/79715177)
3. [ConcurrentHashMap源码分析（JDK8版本）](https://blog.csdn.net/u010723709/article/details/48007881)
4. [ConcurrentHashMap 在1.7与1.8中的不同](https://www.jianshu.com/p/e694f1e868ec)

