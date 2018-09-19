package com.wyj.concurrency.thread_interrupt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 扫描c盘下的所有文件，当输入 quit的时候 退出
 * Author: wyj
 * Email: 18346668711@163.com
 * Date: 2018/9/19
 */
public class FileScanner {

    /**
     * 扫描某个目录的所有文件
     * @param f
     * @throws InterruptedException
     */
    private static void listFile(File f) throws InterruptedException {
        if (f == null) return;

        if (f.isFile()) {
            System.out.println(f.getAbsolutePath());
            return ;
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("user quit");
        }

        File[] files = f.listFiles();
        for (File tmp : files) {
            listFile(tmp);
        }
    }

    /**
     * 读入用户输入
     * @return
     */
    private static String readFromConsole() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return reader.readLine();
        } catch (IOException e) {
            return "";
        }
    }

    public static void main(String[] args) {
        File f = new File("C:\\Users\\KIM_JIE");
        System.out.println(f.getAbsolutePath());

        final Thread scannerThread = new Thread(() -> {
            try {
                listFile(new File("C:\\Users\\KIM_JIE"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        //另外启动一个线程去 读取用户的输入
        new Thread(() -> {
            while(true) {
                if ("q".equals(readFromConsole())) {
                    if (scannerThread.isAlive()) {
                        scannerThread.interrupt();
                    }
                    break;
                } else {
                    System.out.println("input 'q' to exit");
                }
            }
        }).start();

        scannerThread.start();
    }

}
