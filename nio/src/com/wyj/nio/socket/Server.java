package com.wyj.nio.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


/**
 * Selector 只能监听非阻塞的 channel，
 * 
 * @author wuyingjie
 * @date 2018年8月29日
 */

public class Server {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		startServer(6666);
	}
	
	public static void startServer(int port) throws IOException, InterruptedException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.socket().bind(new InetSocketAddress("localhost", port));
		Selector selector = Selector.open();
		serverChannel.configureBlocking(false);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		ReadSelector readSelector = new ReadSelector();
		new Thread(readSelector).start();
		WriteSelector writeSelector = new WriteSelector();
		new Thread(writeSelector).start();
		
		while (true) {
			if (selector.select() == 0) {
				System.err.println("unfind sleep 1s");
				Thread.sleep(1000);
				continue;
			}
			
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			for(SelectionKey key : selectedKeys) {
				if (key.isAcceptable()) {
					SocketChannel socketChannel = serverChannel.accept();
					socketChannel.configureBlocking(false);
					System.err.println("get one client socket, register to readable selector");
					readSelector.register(socketChannel);
					readSelector.getSelector().wakeup();
					writeSelector.register(socketChannel);
					writeSelector.getSelector().wakeup();
				}
			}
			selectedKeys.clear();
			
		}
	}
	
	
	static class ReadSelector implements Runnable{

		Selector selector;
		
		public ReadSelector() throws IOException {
			selector = Selector.open();
		}
		
		public void register(SelectableChannel channel) throws IOException {
			channel.register(selector, SelectionKey.OP_READ);
		}
		
		public void register(SelectableChannel channel, Object attach) throws IOException {
			channel.register(selector, SelectionKey.OP_READ, attach);
		}
		
		public Selector getSelector() {
			return selector;
		}
		
		@Override
		public void run() {
			while (true){
				try {
					System.out.println(selector.keys());
					if (selector.select(5000) == 0) {
						System.out.println("read 超时？");
						Thread.sleep(50);
						continue;
					}
					System.out.println("read next round");
					
					Set<SelectionKey> selectionKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectionKeys.iterator();
					while(iterator.hasNext()) {
						SelectionKey key = iterator.next();
						if (key.isReadable()) {
							ByteBuffer buffer = ByteBuffer.allocate(1024);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ReadableByteChannel channel = (ReadableByteChannel)key.channel();
							int length = 0;
							while ((length = channel.read(buffer)) > 0) {
								buffer.flip();
								byte[] bytes = new byte[length];
								buffer.get(bytes);
								baos.write(bytes);
								buffer.clear();
							}
							System.out.println("get msg:"+baos.toString("utf-8"));
							baos.close();
						}
						iterator.remove();
					}
					Thread.sleep(1000);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				
			}
		}
		
	}
	
	static class WriteSelector implements Runnable{

		Selector selector;
		
		public WriteSelector() throws IOException {
			selector = Selector.open();
		}
		
		public void register(SelectableChannel channel) throws IOException {
			channel.register(selector, SelectionKey.OP_WRITE);
		}
		
		public void register(SelectableChannel channel, Object attach) throws IOException {
			channel.register(selector, SelectionKey.OP_WRITE, attach);
		}
		
		public Selector getSelector() {
			return selector;
		}
		
		@Override
		public void run() {
			while (true){
				try {
					if (selector.select(5000) == 0) {
						System.out.println("write 超时");
						Thread.sleep(50);
						continue;
					}
					System.out.println("write next round");
					
					Set<SelectionKey> selectionKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectionKeys.iterator();
					while(iterator.hasNext()) {
						SelectionKey key = iterator.next();
						if (key.isWritable()) {
							String data = "Hello! Client! I'm server";
					    	ByteBuffer buffer = ByteBuffer.allocate(1024);
					    	buffer.put(data.getBytes());
					    	buffer.flip();
					    	boolean write_again = false;
					    	while (buffer.hasRemaining()) {
					    		if(write_again) System.out.println("write again");
					    		write_again = true;
					    		((SocketChannel)key.channel()).write(buffer);
					    	}
					    	System.out.println("write completed");
					    	Thread.sleep(2000);
						}
						iterator.remove();
					}
					Thread.sleep(1000);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				
			}
		}
		
	}
}
