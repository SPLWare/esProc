package com.scudata.dw;

import java.io.*;
import java.math.*;

import com.scudata.common.DateCache;
import com.scudata.common.ObjectCache;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dm.Table;

/**
 * 列存时的读取处理类
 * @author runqian
 *
 */
public class BufferReader {
	private StructManager structManager;
	private byte[] buffer; // 每次读入的字节缓存
	private int index; // 下一字节在buffer中的索引
	private int count; // 读入buffer的实际字节数目

	private byte[] readBuffer = new byte[32];
	private char[] charBuffer = new char[128];
	
	private Object repeatValue;//连续相同的值
	private int repeatCount = 0;//连续相同的值的个数

	private static final String []HEXSTRINGS = new String[] {"0", "1", "2", "3", "4", "5", "6",
		"7", "8", "9", "A", "B", "C", "D", "E", "F"};

	private static final Long LONG0 = new Long(0);
	private static final Double FLOAT0 = new Double(0);
	private static final BigDecimal DECIMAL0 = new BigDecimal(BigInteger.ZERO);
	private static final Integer []INT15 = new Integer[]{new Integer(0), new Integer(1), new Integer(2), new Integer(3),
		new Integer(4), new Integer(5), new Integer(6), new Integer(7), new Integer(8), new Integer(9), new Integer(10),
		new Integer(11), new Integer(12), new Integer(13), new Integer(14), new Integer(15)};

	private static final Object NONE = new Object();
	
	public BufferReader(StructManager structManager, byte[] buffer) {
		this.structManager = structManager;
		this.buffer = buffer;
		this.count = buffer.length;
	}
	
	public BufferReader(StructManager structManager, byte[] buffer, int index, int count) {
		this.structManager = structManager;
		this.buffer = buffer;
		this.index = index;
		this.count = count;
	}

	// 返回当前读取位置
	public long position() {
		return index;
	}
	
	public void skipFully(int n) throws IOException {
		int total = count - index;
		if (total >= n) {
			index += n;
		} else {
			index = count;
			throw new EOFException();
		}
	}

	public int skipBytes(int n) throws IOException {
		if (n < 1) return 0;

		int total = count - index;
		if (total >= n) {
			index += n;
			return n;
		} else {
			index = count;
			return total;
		}
	}

	public long skip(long n) throws IOException {
		if (n < 1) return 0;

		long total = count - index;
		if (total >= n) {
			index += (int)n;
			return n;
		} else {
			index = count;
			return total;
		}
	}

	public boolean hasNext() throws IOException {
		return count > index;
	}

	public int available() throws IOException {
		return count - index;
	}

	public void reset() {
		index = 0;
		repeatCount = 0;
	}
	
	public void close() throws IOException {
		index = count;
	}

	public boolean readBoolean() throws IOException {
		return buffer[index++] != 0;
	}

	public int read() throws IOException {
		return buffer[index++] & 0xff;
	}

	private int read2() throws IOException {
		return buffer[index++] & 0xff;
	}

	public int read(byte []b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte []b, int off, int len) throws IOException {
		int total = count - index;
		if (total >= len) {
			System.arraycopy(buffer, index, b, off, len);
			index += len;
			return len;
		} else if (total > 0) {
			System.arraycopy(buffer, index, b, off, total);
			index = count;
			return total;
		} else {
			return -1;
		}
	}

	public void readFully(byte []b) throws IOException {
		if (read(b, 0, b.length) != b.length) {
			throw new EOFException();
		}
	}

	public void readFully(byte []b, int off, int len) throws IOException {
		if (read(b, off, len) != len) {
			throw new EOFException();
		}
	}

	public byte readByte() throws IOException {
		return buffer[index++];
	}

	public int readUnsignedByte() throws IOException {
		return buffer[index++] & 0xff;
	}

	public short readShort() throws IOException {
		int ch1 = buffer[index++] << 8;
		return (short)(ch1 + (buffer[index++] & 0xff));
	}

	public int readUnsignedShort() throws IOException {
		int ch1 = (buffer[index++] & 0xff) << 8;
		return ch1 + (buffer[index++] & 0xff);
	}

	public char readChar() throws IOException {
		int ch1 = buffer[index++] << 8;
		return (char)(ch1 + (buffer[index++] & 0xff));
	}

	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() throws IOException {
		int b = read2();
		if (b == BufferWriter.FLOAT16) {
			int n = readUInt16();
			int scale = n >>> 14; // 高两位存小数位数
			n &= 0x3FFF;

			if (scale == 0) {
				return n;
			} else if (scale == 1) {
				return n / 100.0;
			} else if (scale == 2) {
				return n / 10000.0;
			} else {
				return n * 100;
			}
		} else if (b == BufferWriter.FLOAT32) {
			int n = readInt32();
			int scale = n >>> 30; // 高两位存小数位数
			n &= 0x3FFFFFFF;

			if (scale == 0) {
				return n;
			} else if (scale == 1) {
				return n / 100.0;
			} else if (scale == 2) {
				return n / 10000.0;
			} else {
				return n * 100.0;
			}
		} else {
			return Double.longBitsToDouble(readLong64());
		}
	}

	public String readLine() throws IOException {
		throw new IOException("readLine not supported");
	}

	public String readUTF() throws IOException {
		return readString();
	}

	public byte[] readByteArray() throws IOException {
		int len = readInt();
		if (len < 0) {
			return null;
		} else {
			byte[] buf = new byte[len];
			readFully(buf, 0, len);
			return buf;
		}
	}

	public String[] readStrings() throws IOException {
		int len = readInt();
		if (len < 0) {
			return null;
		} else {
			String[] strs = new String[len];
			for (int i = 0; i < len; ++i) {
				strs[i] = readString();
			}

			return strs;
		}
	}

	public String readString() throws IOException {
		int b = read2();
		if (b == BufferWriter.NULL) {
			return null;
		}
		
		int t1 = b & 0xF0;
		if (t1 == BufferWriter.STRING4 || t1 == BufferWriter.STRING5) {
			return readString(b & 0x1F);
		} else if (t1 == BufferWriter.DIGIT4) {
			return readDigits(b & 0x0F);
		} else if (t1 == BufferWriter.HEX4) {
			return HEXSTRINGS[b & 0x0F];
		} else {
			int n = readInt();
			return readString(n);
		}
	}

	private String readDigits(int size) throws IOException {
		byte[] readBuffer = this.readBuffer;
		readFully(readBuffer, 0, size);

		int b = readBuffer[size - 1] & 0xFF;
		int low = b & 0x0F;

		char []chars;
		if (low > 9) {
			int len = 2 * size - 1;
			chars = new char[len];
			chars[len - 1] = (char)('0' + (b >>> 4));
		} else {
			int len = 2 * size;
			chars = new char[len];
			chars[len - 2] = (char)('0' + (b >>> 4));
			chars[len - 1] = (char)('0' + low);
		}

		--size;
		for (int i = 0, q = 0; i < size; ++i) {
			b = readBuffer[i] & 0xFF;
			chars[q++] = (char)('0' + (b >>> 4));
			chars[q++] = (char)('0' + (b & 0x0F));
		}

		return new String(chars);
	}

	private String readString(int size) throws IOException {
		if (size == 0) return "";

		char []charBuffer = this.charBuffer;
		if (size > charBuffer.length) {
			charBuffer = new char[size];
			this.charBuffer = charBuffer;
		}

		int seq = 0;
		int c, char2, char3;
		byte []bytearr;
		int i = 0;

		if (count - index >= size) {
			bytearr = this.buffer;
			i = index;
			this.index += size;
			size += i;
		} else {
			bytearr = new byte[size];
			readFully(bytearr, 0, size);
		}

		while (i < size) {
			c = (int) bytearr[i] & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				/* 0xxx xxxx */
				i++;
				charBuffer[seq++] = (char) c;
				break;
			case 12:
			case 13:
				/* 110x xxxx 10xx xxxx */
				i += 2;
				if (i > size)
					throw new UTFDataFormatException();
				char2 = (int) bytearr[i - 1];
				if ((char2 & 0xC0) != 0x80)
					throw new UTFDataFormatException();
				charBuffer[seq++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
				break;
			case 14:
				/* 1110 xxxx 10xx xxxx 10xx xxxx */
				i += 3;
				if (i > size)
					throw new UTFDataFormatException();
				char2 = (int) bytearr[i - 2];
				char3 = (int) bytearr[i - 1];
				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
					throw new UTFDataFormatException();
				charBuffer[seq++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
				break;
			default:
				/* 10xx xxxx, 1111 xxxx */
				throw new UTFDataFormatException();
			}
		}

		// The number of chars produced may be less than utflen
		if (seq == 1) 
			return ObjectCache.getString(charBuffer);
		return new String(charBuffer, 0, seq);
	}

	public long readLong() throws IOException {
		int b = read2();
		if (b == BufferWriter.LONG0) {
			return 0;
		} else if (b == BufferWriter.LONG16) {
			return readUInt16();
		} else if (b == BufferWriter.LONG32) {
			return readInt32();
		} else {
			return readLong64();
		}
	}

	public int readInt() throws IOException {
		int b = read2();
		if (b == BufferWriter.INT16) {
			return readUInt16();
		} else if (b == BufferWriter.INT32) {
			return readInt32();
		} else if ((b & 0xF0) == BufferWriter.INT12) {
			return ((b & 0x0F) << 8) + read2();
		} else { // INT4
			return b & 0x0F;
		}
	}

	private int readUInt16() throws IOException {
		int n = ((buffer[index] & 0xFF) << 8) + (buffer[index + 1] & 0xFF);
		index += 2;
		return n;
	}
	
	private int readUInt24() throws IOException {
		byte []data = this.buffer;
		int index = this.index;
		this.index += 3;
		return ((data[index] & 0xff) << 16) +
			((data[index + 1] & 0xff) << 8) + (data[index + 2] & 0xff);
	}

	public int readInt32() throws IOException {
		byte []data = this.buffer;
		int index = this.index;
		this.index += 4;
		return (data[index] << 24) + ((data[index + 1] & 0xff) << 16) +
			((data[index + 2] & 0xff) << 8) + (data[index + 3] & 0xff);
	}

	private long readULong32() throws IOException {
		byte []data = this.buffer;
		int index = this.index;
		this.index += 4;
		return ((long)(data[index] & 0xff) << 24) + ((data[index + 1] & 0xff) << 16) +
			((data[index + 2] & 0xff) << 8) + (data[index + 3] & 0xff);
	}
	
	// 5字节表示long，单文件大小要求不超1T
	public long readLong40() throws IOException {
		byte []data = this.buffer;
		int index = this.index;
		this.index += 5;
		return (((long)(data[index] & 0xff) << 32) +
				((long)(data[index + 1] & 0xff) << 24) +
				((data[index + 2] & 0xff) << 16) +
				((data[index + 3] & 0xff) <<  8) +
				(data[index + 4] & 0xff));
	}
	
	// 6字节表示long，单文件大小要求不超256T,仅用于数据挖掘表，文件定位数据的读取
	public long readLong48() throws IOException {
		byte []data = this.buffer;
		int index = this.index;
		this.index += 6;
		return (((long)(data[index] & 0xff) << 40) +
				((long)(data[index + 1] & 0xff) << 32) +
				((long)(data[index + 2] & 0xff) << 24) +
				((data[index + 3] & 0xff) << 16) +
				((data[index + 4] & 0xff) <<  8) +
				(data[index + 5] & 0xff));
	}

	public long readLong64() throws IOException {
		byte []data = this.buffer;
		int index = this.index;
		this.index += 8;
		return (((long)data[index] << 56) +
				((long)(data[index + 1] & 0xff) << 48) +
				((long)(data[index + 2] & 0xff) << 40) +
				((long)(data[index + 3] & 0xff) << 32) +
				((long)(data[index + 4] & 0xff) << 24) +
				((data[index + 5] & 0xff) << 16) +
				((data[index + 6] & 0xff) <<  8) +
				(data[index + 7] & 0xff));
	}

	private Object readMark0(int b) throws IOException {
		switch (b) {
		case BufferWriter.NULL:
			return null;
		case BufferWriter.FLOAT0:
			return FLOAT0;
		case BufferWriter.DECIMAL0:
			return DECIMAL0;
		case BufferWriter.LONG0:
			return LONG0;
		case BufferWriter.TRUE:
			return Boolean.TRUE;
		case BufferWriter.FALSE:
			return Boolean.FALSE;
		default: // none
			return NONE;
		}
	}

	private Object readMark1(int b) throws IOException {
		switch (b) {
		case BufferWriter.INT16:
			return ObjectCache.getInteger(readUInt16());
		case BufferWriter.INT32:
			return new Integer(readInt32());
		case BufferWriter.FLOAT16:
			int n = readUInt16();
			switch (n >>> 14) {
			case 0:
				return new Double(n & 0x3FFF);
			case 1:
				return new Double((n & 0x3FFF) / 100.0);
			case 2:
				return new Double((n & 0x3FFF) / 10000.0);
			default:
				return new Double((n & 0x3FFF) * 100);
			}
		case BufferWriter.FLOAT32:
			n = readInt32();
			switch (n >>> 30) {
			case 0:
				return new Double(n & 0x3FFFFFFF);
			case 1:
				return new Double((n & 0x3FFFFFFF) / 100.0);
			case 2:
				return new Double((n & 0x3FFFFFFF) / 10000.0);
			default:
				return new Double((n & 0x3FFFFFFF) * 100);
			}
		case BufferWriter.FLOAT64:
			return new Double(Double.longBitsToDouble(readLong64()));
		case BufferWriter.LONG16:
			return new Long(readUInt16());
		case BufferWriter.LONG32:
			return new Long(readInt32());
		default:
			return new Long(readLong64());
		}
	}

	private void skipMark1(int b) throws IOException {
		switch (b) {
		case BufferWriter.INT16:
		case BufferWriter.FLOAT16:
		case BufferWriter.LONG16:
			index += 2;
			break;
		case BufferWriter.INT32:
		case BufferWriter.FLOAT32:
		case BufferWriter.LONG32:
			index += 4;
			break;
		default: // FLOAT64 LONG64
			index += 8;
		}
	}

	private Object readMark2(int b) throws IOException {
		switch (b) {
		case BufferWriter.STRING:
			return readString(readInt());
		case BufferWriter.DECIMAL:
			int scale = read2();
			int len = read2();
			byte[] buf = new byte[len];
			readFully(buf, 0, len);
			return new BigDecimal(new BigInteger(buf), scale);
		case BufferWriter.SEQUENCE:
			len = readInt();
			Sequence seq = new Sequence(len);
			for (int i = 0; i < len; ++i) {
				seq.add(innerReadObject());
			}
			return seq;
		case BufferWriter.TABLE:
			return readTable();
		case BufferWriter.RECORD:
			return readRecord();
		default: // BLOB
			return readByteArray();
		}
	}

	private void skipMark2(int b) throws IOException {
		switch (b) {
		case BufferWriter.STRING:
			skipFully(readInt());
			break;
		case BufferWriter.DECIMAL:
			skipFully(1);
			skipFully(read2());
			break;
		case BufferWriter.SEQUENCE:
			int len = readInt();
			for (int i = 0; i < len; ++i) {
				skipObject();
			}
			
			break;
		case BufferWriter.TABLE:
			skipTable();
			break;
		default: // BLOB
			int n = readInt();
			if (n > 0) skipFully(n);
		}
	}

	private Object readMark3(int b) throws IOException {
		switch (b) {
		case BufferWriter.DATE16:
			return DateCache.getDate(readUInt16());
		case BufferWriter.DATE32:
			return new java.sql.Date(BufferWriter.BASEDATE - readULong32() * 1000L);
		case BufferWriter.DATETIME32:
			return new java.sql.Timestamp(readULong32() * 1000L);
		case BufferWriter.DATETIME33:
			return new java.sql.Timestamp(readULong32() * -1000L);
		case BufferWriter.DATETIME64:
			return new java.sql.Timestamp(readLong64());
		case BufferWriter.TIME16:
			return new java.sql.Time(BufferWriter.BASETIME + readUInt16() * 1000);
		case BufferWriter.TIME17:
			return new java.sql.Time(BufferWriter.BASETIME + (0x10000 | readUInt16()) * 1000);
		case BufferWriter.TIME32:
			return new java.sql.Time(BufferWriter.BASETIME + readInt32());
		case BufferWriter.DATE24:
			return new java.sql.Date(BufferWriter.BASEDATE + readUInt24() * 86400000L);
		default: // BufferWriter.DATE64
			return new java.sql.Date(readLong64());
		}
	}

	private void skipMark3(int b) throws IOException {
		switch (b) {
		case BufferWriter.DATE32:
		case BufferWriter.DATETIME32:
		case BufferWriter.DATETIME33:
		case BufferWriter.TIME32:
			skipFully(4);
			break;
		case BufferWriter.DATETIME64:
			skipFully(8);
			break;
		case BufferWriter.DATE24:
			skipFully(3);
			break;
		default: // BufferWriter.DATE16 BufferWriter.TIME16 BufferWriter.TIME17
			skipFully(2);
		}
	}

	public boolean isNone(Object obj) {
		return obj == NONE;
	}
	
	public Object readObject() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return repeatValue;
		}
		
		return innerReadObject();
	}

	private Object innerReadObject() throws IOException {
		int b = read2();
		switch(b & 0xF0) {
		case BufferWriter.MARK0:
			return readMark0(b);
		case BufferWriter.MARK1:
			return readMark1(b);
		case BufferWriter.MARK2:
			return readMark2(b);
		case BufferWriter.MARK3:
			return readMark3(b);
		case BufferWriter.INT4:
			return INT15[b & 0x0F];
		case BufferWriter.INT12:
			return ObjectCache.getInteger(((b & 0x0F) << 8) + read2());
		case BufferWriter.STRING4:
		case BufferWriter.STRING5:
			return readString(b & 0x1F);
		case BufferWriter.DIGIT4:
			return readDigits(b & 0x0F);
		case BufferWriter.HEX4:
			return HEXSTRINGS[b & 0x0F];
		case BufferWriter.REPEAT3:
			return readRepeat(b);
		case BufferWriter.SERIALBYTES:
			int len = b & 0x0F;
			if (len == 0) {
				len = 16;
			}
			
			byte []bytes = new byte[len];
			read(bytes);
			return new SerialBytes(bytes, len);
		default:
			throw new RuntimeException();
		}
	}
	
	private Object readRepeat(int b) throws IOException {
		// 返回一个值，重复数减去1
		int count;
		if ((b & 0x08) == 0) {
			count = (b & 0x07) + 1;
		} else {
			count = ((b & 0x07) << 8) + read2() + 1;
		}
		
		repeatValue = innerReadObject();
		repeatCount = count;
		return repeatValue;
	}
	
	// 返回重复数，0表示结束
	public void skipObject() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return;
		}
		
		int b = read2();
		switch(b & 0xF0) {
		case BufferWriter.MARK0:
			break;
		case BufferWriter.MARK1:
			skipMark1(b);
			break;
		case BufferWriter.MARK2:
			skipMark2(b);
			break;
		case BufferWriter.MARK3:
			skipMark3(b);
			break;
		case BufferWriter.INT4:
			break;
		case BufferWriter.INT12:
			skipFully(1);
			break;
		case BufferWriter.STRING4:
		case BufferWriter.STRING5:
			skipFully(b & 0x1F);
			break;
		case BufferWriter.DIGIT4:
			skipFully(b & 0x0F);
			break;
		case BufferWriter.HEX4:
			break;
		case BufferWriter.REPEAT3:
			readRepeat(b);
			break;
		case BufferWriter.SERIALBYTES:
			int len = b & 0x0F;
			if (len > 0) {
				skipFully(len);
			} else {
				skipFully(16);
			}
			
			break;
		default:
			throw new RuntimeException();
		}
	}
	
	private Record readRecord() throws IOException {
		int id = readInt();
		DataStruct ds = structManager.getDataStruct(id);
		int fcount = ds.getFieldCount();
		
		Object []vals = new Object[fcount];
		for (int f = 0; f < fcount; ++f) {
			vals[f] = innerReadObject();
		}
		
		return new Record(ds, vals);
	}
	
	private Table readTable() throws IOException {
		int id = readInt();
		int len = readInt();
		
		DataStruct ds = structManager.getDataStruct(id);
		int fcount = ds.getFieldCount();
		Object []vals = new Object[fcount];
		Table table = new Table(ds, len);
		
		for (int i = 1; i <= len; ++i) {
			for (int f = 0; f < fcount; ++f) {
				vals[f] = innerReadObject();
			}
			
			table.newLast(vals);
		}
		
		return table;
	}

	private void skipTable() throws IOException {
		int fcount = readInt();
		for (int i = 0; i < fcount; ++i) {
			readString(); // 跳过字段名
		}

		int len = readInt();
		for (int i = 1; i <= len; ++i) {
			for (int f = 0; f < fcount; ++f) {
				skipObject();
			}
		}
	}
	
	/**
	 * 返回当前读到的缓冲区的位置
	 * @return
	 */
	public int getCurPos() {
		return index;
	}
	
	private int repeatInt;
	private long repeatLong;
	private double repeatDouble;
	
	public int readBaseInt() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return repeatInt;
		}

		int b = read2();
		if (b == BufferWriter.INT16) {
			return readUInt16();
		} else if (b == BufferWriter.INT32) {
			return readInt32();
		}
		
		int mask = b & 0xF0;
		if (mask == BufferWriter.INT4) {
			return b & 0x0F;
		} else if (mask == BufferWriter.INT12) {
			return ((b & 0x0F) << 8) + read2();
		} else {
			return readRepeatInt(b);
		}
	}
	
	public long readBaseLong() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return repeatLong;
		}

		int b = read2();
		if (b == BufferWriter.INT16) {
			return readUInt16();
		} else if (b == BufferWriter.INT32) {
			return readInt32();
		}
		
		int mask = b & 0xF0;
		if (mask == BufferWriter.INT4) {
			return b & 0x0F;
		} else if (mask == BufferWriter.INT12) {
			return ((b & 0x0F) << 8) + read2();
		} 
		
		if (b == BufferWriter.LONG0) {
			return 0L;
		} else if (b == BufferWriter.LONG16) {
			return readUInt16();
		} else if (b == BufferWriter.LONG32) {
			return readInt32();
		} else if (b == BufferWriter.LONG64){
			return readLong64();
		} else {
			return readRepeatLong(b);
		}
	}
	
	public double readBaseDouble() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return repeatDouble;
		}
		
		int b = read2();
		if (b == BufferWriter.FLOAT0) {
			return 0.0;
		} else if (b == BufferWriter.FLOAT16) {
			int n = readUInt16();
			int scale = n >>> 14; // 高两位存小数位数
			n &= 0x3FFF;

			if (scale == 0) {
				return n;
			} else if (scale == 1) {
				return n / 100.0;
			} else if (scale == 2) {
				return n / 10000.0;
			} else {
				return n * 100;
			}
		} else if (b == BufferWriter.FLOAT32) {
			int n = readInt32();
			int scale = n >>> 30; // 高两位存小数位数
			n &= 0x3FFFFFFF;

			if (scale == 0) {
				return n;
			} else if (scale == 1) {
				return n / 100.0;
			} else if (scale == 2) {
				return n / 10000.0;
			} else {
				return n * 100.0;
			}
		} else if (b == BufferWriter.FLOAT64) {
			return Double.longBitsToDouble(readLong64());
		} else {
			return readRepeatDouble(b);
		}
	}
	
	public long readBaseDate() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return repeatLong;
		}
		
		int b = read2();
		
		switch (b) {
		case BufferWriter.DATE16:
			return BufferWriter.BASEDATE + readUInt16() * 86400000L;
		case BufferWriter.DATE32:
			return BufferWriter.BASEDATE - readULong32() * 1000L;
		case BufferWriter.DATETIME32:
			throw new RuntimeException();
		case BufferWriter.DATETIME33:
			throw new RuntimeException();
		case BufferWriter.DATETIME64:
			throw new RuntimeException();
		case BufferWriter.TIME16:
			throw new RuntimeException();
		case BufferWriter.TIME17:
			throw new RuntimeException();
		case BufferWriter.TIME32:
			throw new RuntimeException();
		case BufferWriter.DATE24:
			return BufferWriter.BASEDATE + readUInt24() * 86400000L;
		case BufferWriter.DATE64:
			return readLong64();
		default: 
			return readRepeatDate(b);
		}
	}
	
	private int readRepeatInt(int b) throws IOException {
		// 返回一个值，重复数减去1
		int count;
		if ((b & 0x08) == 0) {
			count = (b & 0x07) + 1;
		} else {
			count = ((b & 0x07) << 8) + read2() + 1;
		}
		
		repeatInt = readBaseInt();
		repeatCount = count;
		return repeatInt;
	}
	
	private long readRepeatLong(int b) throws IOException {
		// 返回一个值，重复数减去1
		int count;
		if ((b & 0x08) == 0) {
			count = (b & 0x07) + 1;
		} else {
			count = ((b & 0x07) << 8) + read2() + 1;
		}
		
		repeatLong = readBaseLong();
		repeatCount = count;
		return repeatLong;
	}
	
	private double readRepeatDouble(int b) throws IOException {
		// 返回一个值，重复数减去1
		int count;
		if ((b & 0x08) == 0) {
			count = (b & 0x07) + 1;
		} else {
			count = ((b & 0x07) << 8) + read2() + 1;
		}
		
		repeatDouble = readBaseDouble();
		repeatCount = count;
		return repeatDouble;
	}
	
	private long readRepeatDate(int b) throws IOException {
		// 返回一个值，重复数减去1
		int count;
		if ((b & 0x08) == 0) {
			count = (b & 0x07) + 1;
		} else {
			count = ((b & 0x07) << 8) + read2() + 1;
		}
		
		repeatLong = readBaseDate();
		repeatCount = count;
		return repeatLong;
	}
	
	public void skipBaseInt() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return;
		}
		
		int b = read2();
		switch(b & 0xF0) {
		case BufferWriter.MARK0:
			break;
		case BufferWriter.MARK1:
			skipMark1(b);
			break;
		case BufferWriter.MARK2:
			skipMark2(b);
			break;
		case BufferWriter.MARK3:
			skipMark3(b);
			break;
		case BufferWriter.INT4:
			break;
		case BufferWriter.INT12:
			skipFully(1);
			break;
		case BufferWriter.STRING4:
		case BufferWriter.STRING5:
			skipFully(b & 0x1F);
			break;
		case BufferWriter.DIGIT4:
			skipFully(b & 0x0F);
			break;
		case BufferWriter.HEX4:
			break;
		case BufferWriter.REPEAT3:
			readRepeatInt(b);
			break;
		case BufferWriter.SERIALBYTES:
			int len = b & 0x0F;
			if (len > 0) {
				skipFully(len);
			} else {
				skipFully(16);
			}
			
			break;
		default:
			throw new RuntimeException();
		}
	}
	
	public void skipBaseLong() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return;
		}
		
		int b = read2();
		switch(b & 0xF0) {
		case BufferWriter.MARK0:
			break;
		case BufferWriter.MARK1:
			skipMark1(b);
			break;
		case BufferWriter.MARK2:
			skipMark2(b);
			break;
		case BufferWriter.MARK3:
			skipMark3(b);
			break;
		case BufferWriter.INT4:
			break;
		case BufferWriter.INT12:
			skipFully(1);
			break;
		case BufferWriter.STRING4:
		case BufferWriter.STRING5:
			skipFully(b & 0x1F);
			break;
		case BufferWriter.DIGIT4:
			skipFully(b & 0x0F);
			break;
		case BufferWriter.HEX4:
			break;
		case BufferWriter.REPEAT3:
			readRepeatLong(b);
			break;
		case BufferWriter.SERIALBYTES:
			int len = b & 0x0F;
			if (len > 0) {
				skipFully(len);
			} else {
				skipFully(16);
			}
			
			break;
		default:
			throw new RuntimeException();
		}
	}
	
	public void skipBaseDouble() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return;
		}
		
		int b = read2();
		switch(b & 0xF0) {
		case BufferWriter.MARK0:
			break;
		case BufferWriter.MARK1:
			skipMark1(b);
			break;
		case BufferWriter.MARK2:
			skipMark2(b);
			break;
		case BufferWriter.MARK3:
			skipMark3(b);
			break;
		case BufferWriter.INT4:
			break;
		case BufferWriter.INT12:
			skipFully(1);
			break;
		case BufferWriter.STRING4:
		case BufferWriter.STRING5:
			skipFully(b & 0x1F);
			break;
		case BufferWriter.DIGIT4:
			skipFully(b & 0x0F);
			break;
		case BufferWriter.HEX4:
			break;
		case BufferWriter.REPEAT3:
			readRepeatDouble(b);
			break;
		case BufferWriter.SERIALBYTES:
			int len = b & 0x0F;
			if (len > 0) {
				skipFully(len);
			} else {
				skipFully(16);
			}
			
			break;
		default:
			throw new RuntimeException();
		}
	}
	
	public void skipBaseDate() throws IOException {
		if (repeatCount > 0) {
			repeatCount--;
			return;
		}
		
		int b = read2();
		switch(b & 0xF0) {
		case BufferWriter.MARK0:
			break;
		case BufferWriter.MARK1:
			skipMark1(b);
			break;
		case BufferWriter.MARK2:
			skipMark2(b);
			break;
		case BufferWriter.MARK3:
			skipMark3(b);
			break;
		case BufferWriter.INT4:
			break;
		case BufferWriter.INT12:
			skipFully(1);
			break;
		case BufferWriter.STRING4:
		case BufferWriter.STRING5:
			skipFully(b & 0x1F);
			break;
		case BufferWriter.DIGIT4:
			skipFully(b & 0x0F);
			break;
		case BufferWriter.HEX4:
			break;
		case BufferWriter.REPEAT3:
			readRepeatDate(b);
			break;
		case BufferWriter.SERIALBYTES:
			int len = b & 0x0F;
			if (len > 0) {
				skipFully(len);
			} else {
				skipFully(16);
			}
			
			break;
		default:
			throw new RuntimeException();
		}
	}
}
