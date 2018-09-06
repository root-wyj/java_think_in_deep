# chapter4 组合对象

本章主要讲了构造线程安全类的技术。

[TOC]

### 测试例子

##### Test1

`为了验证存储在静态公共区域里面的状态 也是线程不安全的`

对静态变量a开启3000线程，每个线程里面循环10次 执行a++操作，如果**线程安全，结果是 30000**，**线程不安全，结果 小于 30000**
最后的结果是 `29969`。出现了31次线程冲突。

<br>

##### Test2 -- 机动车追踪器

`问题描述：`
每一辆机动车都有一个String标识，并有一个对应的位置(x,y)。对象VehicleTracker封装了这些信息。**视图线程**和**多个更新线程**可能会共享数据模型。视图线程会获取机动车的名称和位置，将他们显示在显示器上。

1. 包`test2.a`中，演示了基于Java监视器模式(对象封装了所有状态，并由对象自己的内部锁保护)的同步方式。该例子用这种方式实现有这么几个问题，`getLocations`方法也是同步的，这时候会阻塞很多的update操作，更重要的是，`getLocation`方法也被阻塞了，会有很严重的用户体验上的问题。而且类`MutablePoint`是非线程安全的（其实仔细想想这里并没有什么问题）.

2. 包`test2.b`中，演示了基于Java委托的线程安全的同步方式。`DelegatingVehicleTracker`将更新操作的线程安全委托给`ConcurrentHashMap`，内部数据Point是不可变的，所以也是线程安全的。`Tracker`类中，提供了两种获取location的方式，一种是镜像，一种是实时可变的。具体可以根据需求。

3. 包`test2.c`中，演示了底层的可变状态安全同步的方式。它的线程安全依然委托与底层的`ConcurrentHashMap`，不过这次Map的内容是线程安全可变的Point。

##### Test3 向已有的线程安全的类中添加功能

1. 客户端加锁

错误示范:
```java
public class ListHelper<E> {
	public List<E> list = Collection.synchronizedList(new ArrayList<E>());
	...
	public synchronized boolean putIfAbsent(E x) {
		boolean absent = !list.contains(x);
		if (absent) list.add(x);
		return absent;
	}
}
```
上例中，list通过`Collections.synchronizedList`封装，已经实现了线程安全，但是对于`putIfAbsent`这种需要两次访问list的函数的操作，就不能保证原子性了。虽然这里做了`synchronized` 通过，但是锁是`ListHelper`，就是说在同步方法`putIfAbsent`中，依然可以对list有更新，这显然并没有实现真正的同步。因为同步行为发生在**错误的锁**上。下面才是正确的方式：

```java
public class ListHelper<E> {
	public List<E> list = Collection.synchronizedList(new ArrayList<E>());
	...
	public boolean putIfAbsent(E x) {
		synchronized (list) {
			boolean absent = !list.contains(x);
			if (absent) list.add(x);
			return absent;
		}
	}
}
```
通过看`Collection.synchronizedList`的源码会发现，得到的同步对象使用的锁就是他本身。所以这里也是用list作为锁，完成真正的同步。


2. 组合
与`SynchronizedList`中的同步方式一样。继承List并使用线程安全的方式重写所有方法。

> `tips`--`Collections.synchronizedList(list:List)` 通过该静态方法，重新返回一个经过`SynchronizedList`装饰的List，这样所有List的操作，都会被`SynchronizedList`中的同步方法同步，做到线程安全。

> `tips` -- `Collections.unmodifiableMap(locations)` 通过该静态方法，重新返回一个经过 `UnmodifiableMap`装饰的Map，内部只是创建了一个新的指向原空间的指针，并且隔离了map的增删操作，所以通过该方法返回的map是绝对不会被修改的。（但是，如果这个map还有其他指针，那么该map的修改也会体现在unmodify方法返回的map指针中）下面给出示例：

```java
public static void main(String[] args) {
	Map<String, String> map1 = new HashMap<>();
	map1.put("1", "haha");
	System.out.println("map1:"+map1);
	// Map<String, String> map2 = Collections.unmodifiableMap(map1); //注释1
	// Map<String, String> map2 = Collections.unmodifiableMap(new HashMap(map1)); //注释2
	System.out.println("map2:"+map2);
	map1.put("2", "xixi");
	System.out.println("map1:"+map1);
	System.out.println("map2:"+map2);
}

//输出 打开注释1
map1:{1=haha}
map2:{1=haha}
map1:{1=haha, 2=xixi}
map2:{1=haha, 2=xixi}
//打开注释2
map1:{1=haha}
map2:{1=haha}
map1:{1=haha, 2=xixi}
map2:{1=haha}
```


为什么List、Map这么复杂的结构，但是这么简单就做到了从线程不安全到线程安全的转变？ 因为系统对List系列的良好封装，只暴露了List接口，扩展修改都非常方便，不会影响到内部的其他类。
