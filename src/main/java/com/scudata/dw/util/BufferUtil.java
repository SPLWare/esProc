package com.scudata.dw.util;

public abstract class BufferUtil {
	public static BufferUtil util;
	
	static {
		try {
			Class<?> cls = Class.forName("com.scudata.dw.util.BufferUtilNoVerify");
			util = (BufferUtil) cls.newInstance();
			if (util == null) {
				util = new BufferUtilNormal();
			} else {
				byte[] buf = {0x34,0x12};
				if (0x1234 != util.parseShort(buf, 0))
					util = new BufferUtilNormal();
			}
		} catch (VerifyError e) {
			util = new BufferUtilNormal();
		} catch (ClassNotFoundException e) {
			util = new BufferUtilNormal();
		} catch (InstantiationException e) {
			util = new BufferUtilNormal();
		} catch (IllegalAccessException e) {
			util = new BufferUtilNormal();
		}
	}

	public abstract void parseShort(byte[] in, int[] out, int len);
	
	public abstract void parseShort(byte[] in, long[] out, int len);
	
	public abstract int parseShort(byte[] in, int offset);
	
	public abstract void parseInt(byte[] in, int[] out, int len);
	
	public abstract void parseInt(byte[] in, long[] out, int len);
	
	public abstract int parseInt(byte[] in, int offset);
	
	public abstract void parseLong(byte[] in, long[] out, int len);
	
	public abstract long parseLong(byte[] in, int offset);
	
	public abstract void parseDouble(byte[] in, double[] out, int len);
	
	public abstract double parseDouble(byte[] in, int offset);
	
	public static void parseByteToShortArray(byte[] in, int[] out, int len) {
		util.parseShort(in, out, len);
	}

	public static void parseByteToShortArray(byte[] in, long[] out, int len) {
		util.parseShort(in, out, len);
	}
	
	public static void parseByteToIntArray(byte[] in, int[] out, int len) {
		util.parseInt(in, out, len);
	}
	
	public static void parseByteToIntArray(byte[] in, long[] out, int len) {
		util.parseInt(in, out, len);
	}
	
	public static void parseByteToLongArray(byte[] in, long[] out, int len) {
		util.parseLong(in, out, len);
	}
	
	public static void parseByteToDoubleArray(byte[] in, double[] out, int len) {
		util.parseDouble(in, out, len);
	}
	
	public static int parseToShort(byte[] in, int offset) {
		return util.parseShort(in, offset);
	}
	
	public static int parseToInt(byte[] in, int offset) {
		return util.parseInt(in, offset);
	}
	
	public static long parseToLong(byte[] in, int offset) {
		return util.parseLong(in, offset);
	}
	
	public static double parseToDouble(byte[] in, int offset) {
		return util.parseDouble(in, offset);
	}
}
