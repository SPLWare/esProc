package com.scudata.parallel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import com.scudata.dm.Env;

/**
 * 封装了参数调整后的数据套接字
 * 使用该对象后，客户端和服务端可以直接进行高效的数据传输
 * @author Joancy
 *
 */
public class SocketData{
	int socketBuf = Env.getFileBufSize();
	private Socket socket;
	
	private ObjectOutputStream oos=null;
	private ObjectInputStream ois=null;
	
	/**
	 * 基于socket构建一个数据套接字
	 * @param socket 套接字对象
	 * @throws Exception 构造出错时抛出异常
	 */
	public SocketData(Socket socket) throws Exception{
		this.socket = socket;
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		socket.setReceiveBufferSize(socketBuf);
		socket.setSendBufferSize(socketBuf);
		socket.setSoLinger(true, 1);
		socket.setReuseAddress(true);
	}
	
	/**
	 * 客户端创建套接字数据后，需要产生客户端通讯流
	 * @throws IOException 出错时抛出异常
	 */
	public void holdCommunicateStreamClient() throws IOException{
		OutputStream os = socket.getOutputStream();
		// 测试结果，socket的接收缓冲越大，原则上越快；对于文件流的读写，没有缓冲流时，
		//速度也越快，而且多个缓冲流且缓冲块设置很大时，会内存溢出，故注释
		// 纯内存数据发送时，有缓冲流会快
		BufferedOutputStream bos = new BufferedOutputStream(os);// ,socketBuf,有了缓冲后，size本身的作用不是很大
		oos = new ObjectOutputStream(bos);
		oos.flush();//流创建后立刻flush，避免多次连续写时死锁；
		
		InputStream is = socket.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		ois = new ObjectInputStream(bis);
	}

	/**
	 * 服务端创建套接字数据后，需要产生服务端通讯流
	 * @throws IOException 出错时抛出异常
	 */
	public void holdCommunicateStreamServer() throws IOException{
		InputStream is = socket.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		ois = new ObjectInputStream(bis);

		OutputStream os = socket.getOutputStream();
		// 测试结果，socket的接收缓冲越大，原则上越快；对于文件流的读写，没有缓冲流时，速度也越快，而且多个缓冲流且缓冲块设置很大时，会内存溢出，故注释
		// 纯内存数据发送时，有缓冲流会快
		BufferedOutputStream bos = new BufferedOutputStream(os);// ,socketBuf,有了缓冲后，size本身的作用不是很大
		oos = new ObjectOutputStream(bos);
		oos.flush();//流创建后立刻flush，避免多次连续写时死锁；
		
	}

	/**
	 * 连接到套接字地址
	 * @param endpoint 套接字地址
	 * @param timeout 连接超时的时间，单位毫秒
	 * @throws Exception 连接异常
	 */
	public void connect(SocketAddress endpoint,
            int timeout) throws Exception{
		try{
			socket.connect( endpoint, timeout);
			holdCommunicateStreamClient();
		}catch(Exception e){
			throw new Exception(endpoint+":"+e.getMessage(),e);
		}
	}
	
	/**
	 * 获取原始套接字
	 * @return 套接字对象
	 */
	public Socket getSocket(){
		return socket;
	}
	
	/**
	 * 写出一个对象
	 * @param obj 数据对象
	 * @throws IOException 写出错异常
	 */
	public void write(Object obj)
			throws IOException {
		oos.writeUnshared(obj);
		oos.flush();
		oos.reset();//写完后清空内存，避免内存溢出
	}

	/**
	 * 读出对象数据
	 * @return 数据对象
	 * @throws IOException IO异常
	 * @throws ClassNotFoundException 类异常
	 */
	public Object read()
			throws IOException, ClassNotFoundException {
		Object obj = ois.readUnshared();
		return obj;
	}

	/**
	 * 查看套接字是否关闭
	 * @return 关闭时返回true，否则返回false
	 */
	public boolean isClosed(){
		return socket.isClosed();
	}
	
	/**
	 * 服务器端关闭时调用该方法，释放套接字
	 * @throws Exception 关闭异常
	 */
	public void serverClose() throws Exception{
		socket.close();
	}

	/**
	 * 客户端关闭时调用该方法，释放套接字
	 * 客户端需要先发送null命令通知服务端退出线程
	 * @throws Exception 关闭异常
	 */
	public void clientClose() throws Exception{
		write(null);//关掉服务线程端的socket后，通讯流已经中断了
		serverClose();
	}
}