package com.raqsoft.dw;

import java.io.*;
import java.math.*;
import java.util.Arrays;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.SerialBytes;

/**
 * 行存时写出类
 * 不压缩同值
 * @author runqian
 *
 */
public class RowBufferWriter {
	static final int MARK0 = 0x00;
	static final int NULL = 0x00;
	static final int TRUE = 0x01;
	static final int FALSE = 0x02;
	static final int LONG0 = 0x03;
	static final int FLOAT0 = 0x04;
	static final int DECIMAL0 = 0x05;
	static final int NONE = 0x07;

	static final int MARK1 = 0x10;
	static final int INT16 = 0x10;
	static final int INT32 = 0x11;
	static final int LONG16 = 0x12;
	static final int LONG32 = 0x13;
	static final int LONG64 = 0x14;
	static final int FLOAT16 = 0x15;
	static final int FLOAT32 = 0x16;
	static final int FLOAT64 = 0x17;

	static final int MARK2 = 0x20;
	static final int DECIMAL = 0x20;
	static final int STRING = 0x21;
	static final int SEQUENCE = 0x22;
	static final int TABLE = 0x23;
	static final int BLOB = 0x24;
	static final int RECORD = 0x25;

	static final int MARK3 = 0x30;
	static final int DATE16 = 0x30; // 2000年之后的日期
	static final int DATE32 = 0x31; // 2000年之前的日期
	static final int TIME16 = 0x32;
	static final int TIME17 = 0x33;
	static final int DATETIME32 = 0x34;
	static final int DATETIME33 = 0x35;
	static final int DATETIME64 = 0x36;
	static final int TIME32 = 0x37;
	static final int DATE24 = 0x38; // 2000年之后的日期
	static final int DATE64 = 0x39; // 超界表示不了都得日期用64位保存

	static final int SERIALBYTES = 0x40; // 排号

	static final int INT4 = 0x80;
	static final int INT12 = 0x90;
	static final int HEX4 = 0xA0;
	static final int DIGIT4 = 0xB0;
	static final int STRING4 = 0xC0;
	static final int STRING5 = 0xD0;

	static final int FIXED64 = 0xE0;

	private static final double MINFLOAT = 0.000001;
	private static final int MAX_DIGIT_LEN = 30;

	static final long BASEDATE; // 1992年之前有的日期不能被86400000整除
	static final long BASETIME;
	static {
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.set(1970, java.util.Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(java.util.Calendar.MILLISECOND, 0);
		BASETIME = calendar.getTimeInMillis();

		calendar.set(java.util.Calendar.YEAR, 2000);
		BASEDATE = calendar.getTimeInMillis();
	}

	static final int INIT_BUFFER_SIZE = 1024 * 64;
	
	private byte []buf; // 写缓冲区
	private int count = 0;//写入的长度
	private StructManager structManager;

	public RowBufferWriter(StructManager structManager) {
		buf = new byte[INIT_BUFFER_SIZE];
		this.structManager = structManager;
	}
	
	public RowBufferWriter(StructManager structManager, byte []buffer) {
		buf = buffer;
		this.structManager = structManager;
	}

	/**
	 * 结束写，返回结果字节数组
	 * @return
	 * @throws IOException
	 */
	public byte[] finish() throws IOException {
		int len = count;
		count = 0;
		return Arrays.copyOf(buf, len);
	}

	public int getCount() {
		return count;
	}
	
	/**
	 * 放弃写入的内容，回到开头位置
	 */
	public void reset() {
		count = 0;
	}
	
	/**
	 * 扩容缓冲区
	 */
	private void enlargeBuffer() {
		buf = Arrays.copyOf(buf, buf.length << 1);
	}
	
	/**
	 * 按指定大小扩容缓冲区
	 * @param newLen
	 */
	private void enlargeBuffer(int newLen) {
		buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newLen));
	}
	
	/**
	 * 写入一个字节
	 * @param b
	 * @throws IOException
	 */
	public void write(int b) throws IOException {
		if (count >= buf.length) {
			enlargeBuffer();
		}
		
		buf[count++] = (byte)b;
	}

	/**
	 * 写入一个字节数组
	 * @param b
	 * @throws IOException
	 */
	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	/**
	 * 写入一个字节数组
	 * @param b 数组
	 * @param off 偏移
	 * @param len 长度
	 * @throws IOException
	 */
	public void write(byte b[], int off, int len) throws IOException {
		if (len > buf.length - count) {
			enlargeBuffer(count + len);
		}
		
		System.arraycopy(b, off, buf, count, len);
		count += len;
	}

	/**
	 * 写入一个字节
	 * @param v
	 * @throws IOException
	 */
	public void writeByte(int v) throws IOException {
		if (count >= buf.length) {
			enlargeBuffer();
		}
		
		buf[count++] = (byte)v;
	}

	public void writeBoolean(boolean v) throws IOException {
		if (count >= buf.length) {
			enlargeBuffer();
		}
		
		buf[count++] = v ? (byte)1 : (byte)0;
	}

	public void writeShort(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	public void writeChar(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	public void writeFloat(float v) throws IOException {
		writeInt(Float.floatToIntBits(v));
	}

	public void writeBytes(String s) throws IOException {
		for (char c : s.toCharArray()) {
			write(c);
		}
	}

	public void writeChars(String s) throws IOException {
		for (char c : s.toCharArray()) {
			writeChar(c);
		}
	}

	public void writeUTF(String str) throws IOException {
		writeString(str);
	}

	/**
	 * 写入一个字节数组 （可以是NULL）
	 * @param v 
	 * @throws IOException
	 */
	public void writeBytes(byte[] v) throws IOException {
		if (v == null) {
			writeInt(-1);
		} else {
			int len = v.length;
			writeInt(len);
			write(v, 0, len);
		}
	}

	public void writeStrings(String[] strs) throws IOException {
		if (strs == null) {
			writeInt(-1);
		} else {
			writeInt(strs.length);
			for (String str : strs) {
				writeString(str);
			}
		}
	}

	/**
	 * 是否是长度小于等于32的数字串
	 * @param charr
	 * @param len
	 * @return
	 */
	private boolean isDigit(char []charr, int len) {
		if (len > MAX_DIGIT_LEN) return false;

		for (int i = 0; i < len; ++i) {
			if (charr[i] < '0' || charr[i] > '9') return false;
		}

		return true;
	}

	/**
	 * 写入长度小于等于32的数字串
	 * @param charr
	 * @param len
	 * @throws IOException
	 */
	private void writeDigit(char []charr, int len) throws IOException {
		if (buf.length - count < MAX_DIGIT_LEN) {
			enlargeBuffer();
		}
		
		byte []writeBuffer = this.buf;
		int seq = count;
		if (len % 2 == 0) {
			writeBuffer[seq++] = (byte)(DIGIT4 | (len / 2));
			for (int i = 0; i < len; ) {
				int d1 = charr[i++] - '0';
				int d2 = charr[i++] - '0';
				writeBuffer[seq++] = (byte)((d1 << 4) | d2);
			}
		} else {
			writeBuffer[seq++] = (byte)(DIGIT4 | (len / 2 + 1));
			len--;
			for (int i = 0; i < len; ) {
				int d1 = charr[i++] - '0';
				int d2 = charr[i++] - '0';
				writeBuffer[seq++] = (byte)((d1 << 4) | d2);
			}
			
			writeBuffer[seq++] = (byte)((charr[len] - '0' << 4) | 0x0F);
		}
		
		count = seq;
	}

	public void writeString(String str) throws IOException {
		if (str == null) {
			write(NULL);
			return;
		}
		
		int strlen = str.length();
		if (strlen == 0) {
			write(STRING4);
			return;
		} else if (strlen == 1) {
			char c = str.charAt(0);
			if (c >= '0' && c <= '9') {
				write(HEX4 | (c - '0'));
				return;
			} else if (c >= 'A' && c <= 'F') {
				write(HEX4 | (c - 'A' + 10));
				return;
			}
		}

		char[] charr = new char[strlen];
		str.getChars(0, strlen, charr, 0);
		if (isDigit(charr, strlen)) {
			writeDigit(charr, strlen);
			return;
		}

		int utflen = 0;
		int c, count = 0;
		for (int i = 0; i < strlen; i++) {
			c = charr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}

		byte[] bytearr = new byte[utflen];
		for (int i = 0; i < strlen; i++) {
			c = charr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				bytearr[count++] = (byte)c;
			} else if (c > 0x07FF) {
				bytearr[count++] = (byte)(0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte)(0x80 | ((c >> 6) & 0x3F));
				bytearr[count++] = (byte)(0x80 | ((c >> 0) & 0x3F));
			} else {
				bytearr[count++] = (byte)(0xC0 | ((c >> 6) & 0x1F));
				bytearr[count++] = (byte)(0x80 | ((c >> 0) & 0x3F));
			}
		}

		if (utflen <= 0x1F) {
			write(STRING4 | utflen);
			write(bytearr);
		} else {
			write(STRING);
			writeInt(utflen);
			write(bytearr);
		}
	}

	private void writeDecimal(BigDecimal bd) throws IOException {
		byte []bts = bd.unscaledValue().toByteArray();
		int scale = bd.scale();
		if (scale == 0 && bts[0] == 0 && bts.length == 1) {
			write(DECIMAL0);
		} else {
			write(DECIMAL);

			write(scale);
			write(bts.length);
			write(bts);
		}
	}

	private void writeDecimal(BigInteger bi) throws IOException {
		byte []bts = bi.toByteArray();
		if (bts[0] == 0 && bts.length == 1) {
			write(DECIMAL0);
		} else {
			write(DECIMAL);

			write(0);
			write(bts.length);
			write(bts);
		}
	}

	private void writeDouble(double d, long v, int scale) throws IOException {
		if (v <= 0x3FFF) {
			int n = (int)v;
			write(FLOAT16);
			write((n >>> 8) | scale);
			write(n & 0xFF);
		} else if (v <= 0x3FFFFFFF) {
			int n = (int)v;
			write(FLOAT32);
			write((n >>> 24) | scale);
			write((n >>> 16) & 0xFF);
			write((n >>>  8) & 0xFF);
			write(n & 0xFF);
		} else {
			writeDouble64(d);
		}
	}

	private void writeDouble64(double d) throws IOException {
		write(FLOAT64);
		long v = Double.doubleToLongBits(d);
		writeLong64(v);
	}

	public void writeDouble(double d) throws IOException {
		if (d > 0.0 && d <= 0x3FFFFFFF) {
			double v = Math.ceil(d);
			if (v - d < MINFLOAT) {
				long l = (long)v;
				if (l % 100 == 0) {
					writeDouble(d, l / 100, 0xC0);
				} else {
					writeDouble(d, (long)v, 0x00);
				}
			} else {
				double d1 = d * 100;
				v = Math.ceil(d1);
				if (v - d1 < MINFLOAT) {
					writeDouble(d, (long)v, 0x40);
				} else {
					d1 = d * 10000;
					v = Math.ceil(d1);
					if (v - d1 < MINFLOAT) {
						writeDouble(d, (long)v, 0x80);
					} else {
						writeDouble64(d);
					}
				}
			}
		} else if (d == 0.0) {
			write(FLOAT0);
		} else {
			writeDouble64(d);
		}
	}

	public void writeLong(long v) throws IOException {
		if (v == 0L) {
			write(LONG0);
		} else if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
			int n = (int)v;
			if (n >= 0) {
				if (n <= 0xFFFF) {
					write(LONG16);
					write(n >>> 8);
					write(n & 0xFF);
				} else {
					write(LONG32);
					write(n >>> 24);
					write((n >>> 16) & 0xFF);
					write((n >>>  8) & 0xFF);
					write(n & 0xFF);
				}
			} else {
				write(LONG32);
				write(n >>> 24);
				write((n >>> 16) & 0xFF);
				write((n >>>  8) & 0xFF);
				write(n & 0xFF);
			}
		} else {
			write(LONG64);
			writeLong64(v);
		}
	}
	
	public void writeInt32(int n) throws IOException {
		write(n >>> 24);
		write((n >>> 16) & 0xFF);
		write((n >>>  8) & 0xFF);
		write(n & 0xFF);
	}

	public void writeInt64(int n) throws IOException {
		write(0);
		write(0);
		write(0);
		write(0);
		write(n >>> 24);
		write((n >>> 16) & 0xFF);
		write((n >>>  8) & 0xFF);
		write(n & 0xFF);
	}
	
	// 5字节表示long，单文件大小要求不超1T
	public void writeLong40(long v) throws IOException {
		if (buf.length - count < 5) {
			enlargeBuffer();
		}
		
		byte []writeBuffer = this.buf;
		int seq = count;
		writeBuffer[seq++] = (byte)(v >>> 32);
		writeBuffer[seq++] = (byte)(v >>> 24);
		writeBuffer[seq++] = (byte)(v >>> 16);
		writeBuffer[seq++] = (byte)(v >>>  8);
		writeBuffer[seq++] = (byte)(v >>>  0);
		count = seq;
	}
	
	// 6字节表示long，单文件大小要求不超256T，仅共数据挖掘表，文件定位数据的保存
	public void writeLong48(long v) throws IOException {
		if (buf.length - count < 6) {
			enlargeBuffer();
		}
		
		byte []writeBuffer = this.buf;
		int seq = count;
		writeBuffer[seq++] = (byte)(v >>> 40);
		writeBuffer[seq++] = (byte)(v >>> 32);
		writeBuffer[seq++] = (byte)(v >>> 24);
		writeBuffer[seq++] = (byte)(v >>> 16);
		writeBuffer[seq++] = (byte)(v >>>  8);
		writeBuffer[seq++] = (byte)(v >>>  0);
		count = seq;
	}
	
	public void writeLong64(long v) throws IOException {
		if (buf.length - count < 8) {
			enlargeBuffer();
		}
		
		byte []writeBuffer = this.buf;
		int seq = count;
		writeBuffer[seq++] = (byte)(v >>> 56);
		writeBuffer[seq++] = (byte)(v >>> 48);
		writeBuffer[seq++] = (byte)(v >>> 40);
		writeBuffer[seq++] = (byte)(v >>> 32);
		writeBuffer[seq++] = (byte)(v >>> 24);
		writeBuffer[seq++] = (byte)(v >>> 16);
		writeBuffer[seq++] = (byte)(v >>>  8);
		writeBuffer[seq++] = (byte)(v >>>  0);
		count = seq;
	}

	public void writeInt(int n) throws IOException {
		if (n >= 0) {
			if (n <= 0x0F) {
				write(INT4 | n);
			} else if (n <= 0x0FFF) {
				write(INT12 | (n >>> 8));
				write(n & 0xFF);
			} else if (n <= 0xFFFF) {
				write(INT16);
				write(n >>> 8);
				write(n & 0xFF);
			} else {
				write(INT32);
				write(n >>> 24);
				write((n >>> 16) & 0xFF);
				write((n >>>  8) & 0xFF);
				write(n & 0xFF);
			}
		} else {
			write(INT32);
			write(n >>> 24);
			write((n >>> 16) & 0xFF);
			write((n >>>  8) & 0xFF);
			write(n & 0xFF);
		}
	}

	private void writeTimestamp(java.util.Date dt) throws IOException {
		long t = dt.getTime();
		if (t % 1000 == 0) {
			long v = t / 1000;
			if (v < 0) {
				v = -v;
				if (v <= 0xFFFFFFFFL) {
					write(DATETIME33);
					write((int)(v >>> 24));
					write((int)(v >>> 16));
					write((int)(v >>>  8));
					write((int)(v >>>  0));
					return;
				}
			} else {
				if (v <= 0xFFFFFFFFL) {
					write(DATETIME32);
					write((int)(v >>> 24));
					write((int)(v >>> 16));
					write((int)(v >>>  8));
					write((int)(v >>>  0));
					return;
				}
			}
		}

		write(DATETIME64);
		writeLong64(t);
	}

	private void writeDate(java.sql.Date date) throws IOException {
		long v = date.getTime();
		if (v >= BASEDATE) {
			// 精确到天
			int d = (int)((v - BASEDATE) / 86400000);
			if (d > 0xFFFF) {
				if (d > 0xFFFFFF) {
					write(DATE64);
					writeLong64(v);
				} else {
					write(DATE24);
					write((d >>> 16));
					write((d >>>  8) & 0xFF);
					write(d & 0xFF);
				}
			} else {
				write(DATE16);
				write(d >>> 8);
				write(d & 0xFF);
			}
		} else {
			// 精确到秒
			long d = (BASEDATE - v) / 1000;
			if (d > 0xFFFFFFFFL) {
				write(DATE64);
				writeLong64(v);
			} else {
				write(DATE32);
				writeInt32((int)d);
			}
		}
	}

	private void writeTime(java.sql.Time time) throws IOException {
		int t = (int)((time.getTime() - BASETIME) % 86400000);
		if (t < 0) t += 86400000;

		if (t % 1000 == 0) {
			t /= 1000;
			if (t > 0xFFFF) {
				write(TIME17);
				write((t >>> 8) & 0xFF);
				write(t & 0xFF);
			} else {
				write(TIME16);
				write(t >>> 8);
				write(t & 0xFF);
			}
		} else {
			write(TIME32);
			write(t >>> 24);
			write((t >>> 16) & 0xFF);
			write((t >>>  8) & 0xFF);
			write(t & 0xFF);
		}
	}
	
	private void writeTime32(java.sql.Time time) throws IOException {
		int t = (int)((time.getTime() - BASETIME) % 86400000);
		if (t < 0) t += 86400000;
		write(TIME32);
		write(t >>> 24);
		write((t >>> 16) & 0xFF);
		write((t >>>  8) & 0xFF);
		write(t & 0xFF);
	}
	
	private void writeRecord(Record r) throws IOException {
		DataStruct ds = r.dataStruct();
		int fcount = ds.getFieldCount();
		int id = structManager.getDataStructID(ds);
		Object []vals = r.getFieldValues();
		
		write(RECORD);
		writeInt(id);
		for (int f = 0; f < fcount; ++f) {
			writeObject(vals[f]);
		}
	}
	
	private void writeSequence(Sequence seq) throws IOException {
		ListBase1 mems = seq.getMems();
		int len = mems.size();

		DataStruct ds = seq.dataStruct();
		if (ds == null) {
			write(SEQUENCE);
			writeInt(len);
			for (int i = 1; i <= len; ++i) {
				writeObject(mems.get(i));
			}
		} else {
			int fcount = ds.getFieldCount();
			int id = structManager.getDataStructID(ds);
			
			write(TABLE);
			writeInt(id);
			writeInt(len);
			for (int i = 1; i <= len; ++i) {
				Record r = (Record)mems.get(i);
				Object []vals = r.getFieldValues();
				for (int f = 0; f < fcount; ++f) {
					writeObject(vals[f]);
				}
			}
		}
	}

	public void writeObject(Object obj) throws IOException {
		if (obj == null) {
			write(NULL);
		} else if (obj instanceof String) {
			writeString((String)obj);
		} else if (obj instanceof Integer) {
			writeInt(((Number)obj).intValue());
		} else if (obj instanceof Double) {
			writeDouble(((Number)obj).doubleValue());
		} else if (obj instanceof BigDecimal) {
			writeDecimal((BigDecimal)obj);
		} else if (obj instanceof Long) {
			writeLong(((Number)obj).longValue());
		} else if (obj instanceof java.sql.Date) {
			writeDate((java.sql.Date)obj);
		} else if (obj instanceof java.sql.Time) {
			writeTime((java.sql.Time)obj);
		} else if (obj instanceof java.util.Date) {
			writeTimestamp((java.util.Date)obj);
		} else if (obj instanceof Boolean) {
			if (((Boolean)obj).booleanValue()) {
				write(TRUE);
			} else {
				write(FALSE);
			}
		} else if (obj instanceof BigInteger) {
			writeDecimal((BigInteger)obj);
		} else if (obj instanceof Float) {
			writeDouble(((Number)obj).doubleValue());
		} else if (obj instanceof Number) { // Byte  Short
			writeInt(((Number)obj).intValue());
		} else if (obj instanceof Sequence) {
			writeSequence((Sequence)obj);
		} else if (obj instanceof Record) {
			writeRecord((Record)obj);
		} else if (obj instanceof byte[]) {
			write(BLOB);
			writeBytes((byte[])obj);
		} else if (obj instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)obj;
			int len = sb.length();
			if (len < 16) {
				write(SERIALBYTES | len);
			} else {
				// 0表示长度16
				write(SERIALBYTES);
			}
			
			write(sb.toByteArray());
		} else {
			throw new RQException("error type: " + obj.getClass().getName());
		}
	}
	
	public void writeFixedLengthObject(Object obj) throws IOException {
		if (obj == null) {
			write(FIXED64);
			writeInt64(0);//write(NULL);
		} else if (obj instanceof Integer) {
			write(FIXED64);
			write(INT32);
			writeInt32(((Number)obj).intValue());
			write(0);
			write(0);
			write(0);
		} else if (obj instanceof Double) {
			writeDouble64(((Number)obj).doubleValue());
		} else if (obj instanceof Long) {
			write(LONG64);
			writeLong64(((Number)obj).longValue());
		} else if (obj instanceof java.sql.Date) {
			write(DATE64);
			writeLong64(((java.sql.Date)obj).getTime());
		} else if (obj instanceof java.sql.Time) {
			write(FIXED64);
			writeTime32((java.sql.Time)obj);
			write(0);
			write(0);
			write(0);
		} else if (obj instanceof java.util.Date) { // java.sql.Timestamp
			write(DATETIME64);
			writeLong64(((java.util.Date)obj).getTime());
		} else if (obj instanceof Float) {
			writeDouble64(((Number)obj).doubleValue());
		} else if (obj instanceof Number) { // Byte  Short
			write(FIXED64);
			write(INT32);
			writeInt32(((Number)obj).intValue());
			write(0);
			write(0);
			write(0);
		} else if (obj instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)obj;
			
			//扩大为长度16的排号
			byte[] arr = new byte[16];
			int len = sb.length();
			System.arraycopy(sb.toByteArray(), 16 - len, arr, 0, len);
			
			write(SERIALBYTES);//长度16
			write(arr);
		} else {
			throw new RQException("error type: " + obj.getClass().getName());
		}
	}
	
	public void writeNone() throws IOException {
		
		write(NONE);
	}
	
}