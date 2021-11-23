package com.scudata.dw;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * LZ4压缩解压工具类
 * @author runqian
 *
 */
public class LZ4Util {
	private static ThreadLocal<LZ4Util> local = new ThreadLocal<LZ4Util>() {
		protected synchronized LZ4Util initialValue() {
			return new LZ4Util();
		}
	};
	
	//压缩
	private LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
	
	//解压
	private LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
	
	private int count;
	
	private LZ4Util() {
	}
	
	public static LZ4Util instance() {
		return local.get();
	}
	
	/**
	 * 返回压缩后的长度
	 * @return
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * 返回压缩后的字节数组，字节数组长度可能大于实际长度，需要调用getCount取的实际长度
	 * @param bytes
	 * @return
	 */
	public byte[] compress(byte []bytes) {
		int maxLen = compressor.maxCompressedLength(bytes.length);
		byte []buffer = new byte[maxLen];
		
		count = compressor.compress(bytes, buffer);
		return buffer;
	}
	
	/**
	 * 解压，srcCount为解压后长度
	 * @param bytes
	 * @param buffer
	 * @param srcCount
	 */
	public void decompress(byte []bytes, byte []buffer, int srcCount) {
		decompressor.decompress(bytes, buffer, srcCount);
	}
	
}