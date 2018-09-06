package com.wyj.nio.file;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 通常我们使用 RandomAccessFile 来打�?文件流，因为该文件流可读也可�?
 * 文件流是无法设置非阻塞的
 * @author wuyingjie
 * @date 2018年8月29日
 */

public class TestFileChannel {

	
	public static void main(String[] args) throws IOException {
		read();
	}
	
	public static void read() throws IOException {
		FileInputStream fis = new FileInputStream("./asserts/test.txt");
		FileChannel channel = fis.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(10);
		// 10 和 1024在有没有汉字的情况下是不一样的
//		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int length = 0;
		while ((length=channel.read(buffer)) > 0){
//			System.out.println(length);
			buffer.flip();
			byte[] bytes = new byte[length];
			buffer.get(bytes);
			System.out.println(new String(bytes));
			buffer.clear();
		}
		channel.close();
		fis.close();
	}
	
	public static void write() throws IOException {	
		FileInputStream fis = new FileInputStream("./asserts/test.txt");
		FileOutputStream fos  = new FileOutputStream("./asserts/out.txt");
		
		FileChannel readChannel = fis.getChannel();
		FileChannel writeChannel = fos.getChannel();
		
		System.out.println("file size:"+readChannel.size());
		
		ByteBuffer buffer = ByteBuffer.allocate(10);
		
		while(readChannel.read(buffer) > 0) {
			buffer.flip();
			while(buffer.hasRemaining()) {	//加上这个为了完全保证数据的正确性，平常跑的时候 没有也行
				writeChannel.write(buffer);
			}
			buffer.clear();
		}
		
		writeChannel.close();
		readChannel.close();
		fos.close();
		fis.close();
	}
}
