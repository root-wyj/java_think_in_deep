package com.wyj.threadsconcurrency;

/**
 * 全面分析 wait、notify、notifyAll的使用
 * @author wuyingjie
 * @date 2018年9月6号
 */

public class C1_WaitNotify {

    /**
     * 这是一个同步类
     */
    public class MonitorObject{}


    /**
     * wait notify 基本使用
     *
     */
    public class WaitNotify1{
        MonitorObject myMonitorObject = new MonitorObject();

        public void doWait() {
            synchronized (myMonitorObject) {
                try {
                    myMonitorObject.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void doNotify() {
            synchronized (myMonitorObject) {
                myMonitorObject.notify();
            }
        }
    }

    /**
     * <b>信号丢失</b>
     * <br>调用notify 和 notifyAll方法时，可能线程并没有处于等待状态，也就是wait 后与 notify到来。这时候就出现了信号丢失。
     * <br>这可能并不是个问题，但是某些情况下就可能会导致等待的线程永远无法醒来。
     */
    public class WaitNotify2{
        MonitorObject myMonitorObject = new MonitorObject();

        boolean wasSignalled = false;

        public void doWait() {
            synchronized (myMonitorObject) {
                try {
                    if (!wasSignalled) {
                        myMonitorObject.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //认为被打断也算是唤醒，并继续执行下面的逻辑代码
                wasSignalled = false;
            }
        }

        public void doNotify() {
            synchronized (myMonitorObject) {
                myMonitorObject.notify();
                wasSignalled = true;
            }
        }
    }

    /**
     * <b>假唤醒</b>
     * <br>由于莫名原因，比如是打断，线程并没有调用notify或者notifyAll，线程就醒来，并开始执行下面的逻辑代码
     * 这样可能会早晨严重的影响。
     * <br>
     * <br>为了防止假唤醒，保存信号量的成员变量放在一个while循环中接受检查。
     * <br><b>这样的一种结构叫做自旋锁</b>
     * <br>
     * <br>如果是多个线程等待相同的信号，被notifyAll唤醒，但是只允许一个执行，这种方法也会使其他的没有获取到锁
     * 的线程，再次进入等待队列，等待唤醒
     *
     */
    public class WaitNotify3{
        MonitorObject myMonitorObject = new MonitorObject();
        boolean wasSignalled = false;

        public void doWait() {
            synchronized (myMonitorObject) {
                while(!wasSignalled) {
                    try {
                        myMonitorObject.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                wasSignalled = false;
            }
        }

        public void doNotify() {
            synchronized (myMonitorObject) {
                myMonitorObject.notify();
                wasSignalled = true;
            }
        }
    }

}
