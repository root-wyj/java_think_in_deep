package com.wyj.concurrency.chapter4;

/**
 *
 * Created
 * Author: wyj
 * Email: 18346668711@163.com
 * Date: 2018/2/7
 */
public class Test1 {

    public static void main(String[] args) {
        test1();
    }

    /**
     * 为了验证存储在静态公共区域里面的状态 也是线程不安全的
     * test1_a++操作其实分为三步 -- 读 +1 写。
     *
     * 测试结果：
     * 一开始 只跑了30个、100个线程，发现结果总是300，1000
     * 当把线程数增加到3000的时候，最后test1_a=29969
     *
     * 第一次出错的位置是：2070 2040 2082 2089
     */
    public static int test1_a = 0;
    public static void test1() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                for (int i=0; i<10; i++) {
                    test1_a++;
                }
                System.out.println(test1_a);
            }
        };

        for (int i=0; i<3000; i++) {
            new Thread(run).start();
        }
    }
}
