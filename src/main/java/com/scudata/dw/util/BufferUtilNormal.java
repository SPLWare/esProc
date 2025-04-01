package com.scudata.dw.util;

public class BufferUtilNormal extends BufferUtil {
	public static void parseShort(byte[] in, int[] out, int len) {
		int offset = 0;
		len = len * 2;
		
		for (int i = 0; i < len; i += 2) {
			out[offset++] = ((in[i + 1]) << 8) | (in[i] & 0xFF);
		}
	}
	
	public static void parseShort(byte[] in, long[] out, int len) {
		int offset = 0;
		len = len * 2;
		
		for (int i = 0; i < len; i += 2) {
			out[offset++] = ((in[i + 1]) << 8) | (in[i] & 0xFF);
		}
	}
	
	public static int parseShort(byte[] in, int offset) {
		return ((in[offset + 1]) << 8) | (in[offset] & 0xff);
	}
	
	public static void parseInt(byte[] in, int[] out, int len) {
		int offset = 0;
		len = len * 4;
		
		for (int i = 0; i < len; i += 4) {
			out[offset++] = (in[i + 3] << 24) + ((in[i + 2] & 0xff) << 16) +
				((in[i + 1] & 0xff) << 8) + (in[i] & 0xff);
		}
	}

	public static void parseInt(byte[] in, long[] out, int len) {
		int offset = 0;
		len = len * 4;
		
		for (int i = 0; i < len; i += 4) {
			out[offset++] = (in[i + 3] << 24) + ((in[i + 2] & 0xff) << 16) +
				((in[i + 1] & 0xff) << 8) + (in[i] & 0xff);
		}
	}
	
	public static int parseInt(byte[] in, int offset) {
		return (in[offset + 3] << 24) + ((in[offset + 2] & 0xff) << 16) +
		((in[offset + 1] & 0xff) << 8) + (in[offset] & 0xff);
	}

	public static void parseLong(byte[] in, long[] out, int len) {
		int offset = 0;
		len = len * 8;
		
		for (int i = 0; i < len; i += 8) {
			out[offset++] = (((long)in[i + 7] << 56) +
					((long)(in[i + 6] & 0xff) << 48) +
					((long)(in[i + 5] & 0xff) << 40) +
					((long)(in[i + 4] & 0xff) << 32) +
					((long)(in[i + 3] & 0xff) << 24) +
					((in[i + 2] & 0xff) << 16) +
					((in[i + 1] & 0xff) <<  8) +
					(in[i] & 0xff));
		}
	}

	public static long parseLong(byte[] in, int offset) {
		return (((long)in[offset + 7] << 56) +
				((long)(in[offset + 6] & 0xff) << 48) +
				((long)(in[offset + 5] & 0xff) << 40) +
				((long)(in[offset + 4] & 0xff) << 32) +
				((long)(in[offset + 3] & 0xff) << 24) +
				((in[offset + 2] & 0xff) << 16) +
				((in[offset + 1] & 0xff) <<  8) +
				(in[offset] & 0xff));
	}

	public static void parseDouble(byte[] in, double[] out, int len) {
		int offset = 0;
		len = len * 8;
		
		for (int i = 0; i < len; i += 8) {
			long v = (((long)in[i + 7] << 56) +
					((long)(in[i + 6] & 0xff) << 48) +
					((long)(in[i + 5] & 0xff) << 40) +
					((long)(in[i + 4] & 0xff) << 32) +
					((long)(in[i + 3] & 0xff) << 24) +
					((in[i + 2] & 0xff) << 16) +
					((in[i + 1] & 0xff) <<  8) +
					(in[i] & 0xff));
			out[offset++] = Double.longBitsToDouble(v);
		}
	}

	public static double parseDouble(byte[] in, int offset) {
		long v = (((long)in[offset + 7] << 56) +
				((long)(in[offset + 6] & 0xff) << 48) +
				((long)(in[offset + 5] & 0xff) << 40) +
				((long)(in[offset + 4] & 0xff) << 32) +
				((long)(in[offset + 3] & 0xff) << 24) +
				((in[offset + 2] & 0xff) << 16) +
				((in[offset + 1] & 0xff) <<  8) +
				(in[offset] & 0xff));
		return Double.longBitsToDouble(v);
	}
	
	//以下是带输入offset的
	public static void parseShort(byte[] in, int inOffset, int[] out, int len) {
		int offset = 0;
		len = len * 2 + inOffset;
		
		for (int i = inOffset; i < len; i += 2) {
			out[offset++] = ((in[i + 1]) << 8) | (in[i] & 0xFF);
		}
	}
	
	public static void parseShort(byte[] in, int inOffset, long[] out, int len) {
		int offset = 0;
		len = len * 2 + inOffset;
		
		for (int i = inOffset; i < len; i += 2) {
			out[offset++] = ((in[i + 1]) << 8) | (in[i] & 0xFF);
		}
	}
	
	public static void parseInt(byte[] in, int inOffset, int[] out, int len) {
		int offset = 0;
		len = len * 4 + inOffset;
		
		for (int i = inOffset; i < len; i += 4) {
			out[offset++] = (in[i + 3] << 24) + ((in[i + 2] & 0xff) << 16) +
				((in[i + 1] & 0xff) << 8) + (in[i] & 0xff);
		}
	}

	public static void parseInt(byte[] in, int inOffset, long[] out, int len) {
		int offset = 0;
		len = len * 4 + inOffset;
		
		for (int i = inOffset; i < len; i += 4) {
			out[offset++] = (in[i + 3] << 24) + ((in[i + 2] & 0xff) << 16) +
				((in[i + 1] & 0xff) << 8) + (in[i] & 0xff);
		}
	}
	
	public static void parseLong(byte[] in, int inOffset, long[] out, int len) {
		int offset = 0;
		len = len * 8 + inOffset;
		
		for (int i = inOffset; i < len; i += 8) {
			out[offset++] = (((long)in[i + 7] << 56) +
					((long)(in[i + 6] & 0xff) << 48) +
					((long)(in[i + 5] & 0xff) << 40) +
					((long)(in[i + 4] & 0xff) << 32) +
					((long)(in[i + 3] & 0xff) << 24) +
					((in[i + 2] & 0xff) << 16) +
					((in[i + 1] & 0xff) <<  8) +
					(in[i] & 0xff));
		}
	}
	
	public static void parseDouble(byte[] in, int inOffset, double[] out, int len) {
		int offset = 0;
		len = len * 8 + inOffset;
		
		for (int i = inOffset; i < len; i += 8) {
			long v = (((long)in[i + 7] << 56) +
					((long)(in[i + 6] & 0xff) << 48) +
					((long)(in[i + 5] & 0xff) << 40) +
					((long)(in[i + 4] & 0xff) << 32) +
					((long)(in[i + 3] & 0xff) << 24) +
					((in[i + 2] & 0xff) << 16) +
					((in[i + 1] & 0xff) <<  8) +
					(in[i] & 0xff));
			out[offset++] = Double.longBitsToDouble(v);
		}
	}
}
