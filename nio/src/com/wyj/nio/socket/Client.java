package com.wyj.nio.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * @author wuyingjie
 * @date 2018年8月29日
 */

public class Client {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Selector selector = Selector.open();
		SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", 6666));
		channel.configureBlocking(false);
		int opts = SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE;
		channel.register(selector, opts);
		while(true) {
		  int readyChannels = selector.select();
		  if(readyChannels == 0) {
			  System.out.println("unfind sleep 0.5s");
			  Thread.sleep(500);
			  continue;
		  }
		  Set<SelectionKey> selectedKeys = selector.selectedKeys();
		  Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
		  while(keyIterator.hasNext()) {
		    SelectionKey key = keyIterator.next();
		    if(key.isAcceptable()) {
		    	System.out.println("acceptable");
		        // a connection was accepted by a ServerSocketChannel.
		    } else if (key.isConnectable()) {
		    	System.out.println("connectable");
		        // a connection was established with a remote server.
		    } else if (key.isReadable()) {
		    	System.out.println("readable");
		    	ByteBuffer buffer = ByteBuffer.allocate(1024);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ReadableByteChannel c = (ReadableByteChannel)key.channel();
				int length = 0;
				while ((length = c.read(buffer)) > 0) {
					buffer.flip();
					byte[] bytes = new byte[length];
					buffer.get(bytes);
					baos.write(bytes);
					buffer.clear();
				}
				System.out.println("get msg:"+baos.toString("utf-8"));
				baos.close();
		        // a channel is ready for reading
		    } else if (key.isWritable()) {
		    	String data = "Hello! Server! I'm client";
		    	ByteBuffer buffer = ByteBuffer.allocate(1024);
		    	buffer.put(data.getBytes());
		    	buffer.flip();
		    	while (buffer.hasRemaining()) {
		    		System.out.println("write again?");
		    		((SocketChannel)key.channel()).write(buffer);
		    	}
		    	System.out.println("writable");
		        // a channel is ready for writing
		    	Thread.sleep(2000);
		    }
		    Thread.sleep(2000);
		    keyIterator.remove();
		  }
		}

	}
	
	SocketChannel socketChannel;
	
	
	public Client() {
		
	}
	
	public void start() throws IOException {
		socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 6666));
		
	}
	
	public void read() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int length = 0;
		while((length = socketChannel.read(buffer)) > 0) {
			buffer.flip();
			byte[] bytes = new byte[length];
			buffer.get(bytes);
			baos.write(bytes);
			buffer.clear();
		}
		System.out.println(baos.toString("utf-8"));
		baos.close();
	}

	private void write(byte[] data) {
		
	}
}
