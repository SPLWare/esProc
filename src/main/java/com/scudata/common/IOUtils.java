package com.scudata.common;

import java.io.*;
public class IOUtils
{

    private static boolean isLetter(char c) {
    	return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
    }

	
	/**
	 * 判断 文件名是否以盘符:/或/开头(\和/等效)
	 */
	public static boolean isAbsolutePath(String fileName){
		int len = fileName.length();
		if(len==0)
			return false;
		fileName = fileName.replace('\\', '/');
		char c0 = fileName.charAt(0);
		if(c0=='/' || (len>=3 && isLetter(c0) 
				&& fileName.charAt(1)==':' && fileName.charAt(2)=='/'))
			return true;
		return false;
	}
	
	/**
	 * 取父子合并后的路径,并以/替换\
	 * @return String parent为null、child为null或绝对路径时均返回child，否则将parent与child合并后返回
	 */
	public static String getPath(String parent, String child){
		if(parent==null || child==null || isAbsolutePath(child))
			return child;
		return new File(parent,child).getPath().replace('\\', '/');
	}
	
	/**
	 * 按绝对路径、当前线程类装载器、IOUtils类装载器次序查找文件
	 */
	public static InputStream findResource(String fileName) {
		InputStream in = null;
		try {
			//File f = new File(fileName);
			if(isAbsolutePath(fileName))
				in = new FileInputStream(fileName);
		} catch (Exception e) {}

		if (in == null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl != null) {
				try {
					in = cl.getResourceAsStream(fileName);
				} catch (Exception e) {}
			}
		}
		if (in == null) {
			try {
				in = IOUtils.class.getResourceAsStream(fileName);
			} catch (Exception e) {}
		}
		return in;
	}
	
	/**
	 * 按绝对路径、指定路径、当前线程类装载器、IOUtils类装载器次序查找文件
	 * 注：path为null时等同于findResourde(fileName)
	 */
	public static InputStream findResource(String fileName, String path) {
		if ( path == null )
			return findResource(fileName);
			
		InputStream in = null;
		try {
			in = new FileInputStream(fileName);
		} catch (Exception e) {}

		if (in == null ) {
			try {
				in = new FileInputStream(new File(path, fileName));
			} catch (Exception e) {}
		}

		if (in == null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl != null) {
				try {
					in = cl.getResourceAsStream(fileName);
				} catch (Exception e) {}
			}
		}
		if (in == null) {
			try {
				in = IOUtils.class.getResourceAsStream(fileName);
			} catch (Exception e) {}
		}
		return in;
	}
	
	/**
	 * 从输入流中读取字节填满数组，如果填不满则抛出EOFException
	 * @param in 输入流
	 * @param bytes 目的数组
	 * @throws IOException
	 */
	public static void readFully( InputStream in, byte[] bytes ) throws IOException{
		int count = bytes.length;
		int offset = 0;
		while (offset < count) {
			int read = in.read(bytes, offset, count - offset);
			if (read < 0) {
				throw new EOFException();
			} else {
				offset += read;
			}
		}
	}

	/**
	 * 从输入流中读取字节填满数组，如果文件结束则可能没有填满
	 * @param in 输入流
	 * @param bytes 目的数组
	 * @throws IOException
	 * @return int 填入的字节数
	 */
	public static int readBytes( InputStream in, byte[] bytes ) throws IOException{
		int count = bytes.length;
		int offset = 0;
		while (offset < count) {
			int read = in.read(bytes, offset, count - offset);
			if (read < 0) {
				break;
			} else {
				offset += read;
			}
		}
		
		return offset;
	}
	
	public static byte readByte( InputStream in ) throws IOException {
		int ch = in.read();
		if (ch < 0)
			throw new EOFException();
		return (byte)(ch);
	}

	public static short readShort( InputStream in ) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		if ((ch1 | ch2) < 0)
			 throw new EOFException();
		return (short)((ch1 << 8) + (ch2 << 0));
	}

	public static void writeInt(OutputStream out, int v) throws IOException {
		out.write((v >>> 24) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>>  8) & 0xFF);
		out.write((v >>>  0) & 0xFF);
	}

	public static int readInt( InputStream in ) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			 throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	public static long readLong( InputStream in ) throws IOException {
		return ((long)(readInt(in)) << 32) + (readInt(in) & 0xFFFFFFFFL);
	}

	public static byte[] readByteArray( InputStream in, byte[] v ) throws IOException {
		if ( v.length != in.read( v ) )
			throw new IOException( "byte array len invalid" );
		return v;
	}

	public static String readString( InputStream in ) throws IOException {
		short len = readShort( in );
		if ( len == 0 )
			return null;
		StringBuffer buf = new StringBuffer(len);
		for( int i = 0; i < len; i ++ )
			buf.append( (char)(readShort(in)) );
		return buf.toString();
	}

	public final static void dump(byte[] buf) {
		int n = 0;
		for(byte b : buf) {
			n++;
			String s = Integer.toHexString(b);
			int len = s.length();
			if(len==1) {
				System.out.print('0');
				System.out.print(s);
			}else{
				System.out.print( s.substring(len-2, len) );
			}
			System.out.print(' ');
			if(n%16==0) System.out.println();
		}
		if(n%16!=0) System.out.println();
	}

}