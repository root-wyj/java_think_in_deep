package com.wyj.nio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 
 * @author wuyingjie
 * @date 2018年8月28日
 */

public class TestNio {
	
	public static void main(String[] args) {
		Runnable r = () -> System.out.println("xixi");
		System.out.println(r);
		
		Runnable r2 = new Runnable() {
			@Override
			public void run() {
				System.out.println("haha");
			}
		};
		System.out.println(r2);
		
		Function<String, Integer> fun = Integer::parseInt;
		System.out.println(fun);
		System.out.println(fun.apply("1"));
		Function<String, Integer> fun2 = new Function<String, Integer>() {
			public Integer apply(String t) {
				return Integer.parseInt(t);
			};
		};
		
		Consumer<Object> consumer = System.out::println;
		System.out.println(consumer);
		
		Supplier<List<String>> supplier = ArrayList::new;
		System.out.println(supplier);
		
		int i = 1;
		Integer i2 = new Integer(1);
		System.out.println(i == i2);
		
		System.out.println(new Integer(1) == new Integer(1));
		
		int i3 = 256;
		Integer i4 = new Integer(256);
		System.out.println(i3 == i4);
		
		String a = "23";
		String b = "23";
		System.out.println(Integer.parseInt(a) == Integer.parseInt(b));
		
		String a1 = "ab";
        String b1 = "a";
        b1 +="b";
        System.out.println(a1 == b1);

        String c = "ab";
        String d = "a" +"b";
        System.out.println(c == d);

        String e = new String("ab");
        System.out.println(c == e);
        
        String f1 = "a";
        String f2 = "b";
        String f = f1 + f2;
        System.out.println(c == f);
        
        String e1 = "a2";
        String e2 = "a";
        final String e3 = "a";
        String e4 = e2 + 2;
        //因为在编译器就可以确定e3的值，所以做了编译期优化， 其实和e5 = "a" + 2 没什么区别
        // 但是如果e3=getA() 通过方法返回的，编译期不能确定，还是返回false
        String e5 = e3 + 2;
        System.out.println(e1 == e4);
        System.out.println(e1 == e5);
        
        String string = Test2.FINAL_STR;
        System.out.println(string);
	}
}
