# java concurrency

### final 关键字
https://www.cnblogs.com/jxldjsn/p/6115764.html

测试了final，却没找到什么意义
```java
	
	static class MyInt{
		private final int a;
		
		public MyInt(int a) {
			for(int i=0; i<200; i++)
				Thread.yield();
			this.a = a;
		}
		
		public int getA() {
			return a;
		}
	}
	
	static MyInt m;
	public static void main(String[] args) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				m = new MyInt(5);
			}
		}).start();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (m == null)
					System.out.println("m is null");
				else {
					int a = m.getA();
					if (a==0)
						System.out.println("m is "+a);
				}
			}
		};
		for (int i=0; i<3000; i++) {
			new Thread(runnable).start();
		}
	}

```