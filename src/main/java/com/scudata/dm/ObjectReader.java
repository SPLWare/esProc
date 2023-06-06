package com.scudata.dm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.UTFDataFormatException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.common.DateCache;
import com.scudata.common.ObjectCache;

/**
 * 用于从输入流读Object，对应的读为ObjectWriter
 * ObjectWriter和ObjectReader的方法是一一对应的，比如writeInt和readInt对应，writeInt32和readInt32对应。
 * 读的时候一定要调用和写的方法相对应的方法，不能写的时候用writeInt32而读的时候用readInt。
 * 此输入流有自己的读缓冲区
 * @author WangXiaoJun
 *
 */
public class ObjectReader extends InputStream implements ObjectInput {
	private InputStream in; // 输入流
	private byte[] buffer; // 每次读入的字节缓存
	private int index; // 下一字节在buffer中的索引
	private int count; // 读入buffer的实际字节数目
	private long position; // 读入光标在流中的位置

	private byte[] readBuffer = new byte[32]; // 读对象时用的临时缓存区
	private char[] charBuffer = new char[128]; // 读字符串时用的临时缓存区

	
	// 以下常量为了常用的对象复用
	private static final String []HEXSTRINGS = new String[] {"0", "1", "2", "3", "4", "5", "6",
		"7", "8", "9", "A", "B", "C", "D", "E", "F"};
	
	private static final Long LONG0 = new Long(0);
	private static final Double FLOAT0 = new Double(0);
	private static final BigDecimal DECIMAL0 = new BigDecimal(BigInteger.ZERO);
	private static final Integer []INT15 = new Integer[]{new Integer(0), new Integer(1), new Integer(2), new Integer(3),
		new Integer(4), new Integer(5), new Integer(6), new Integer(7), new Integer(8), new Integer(9), new Integer(10),
		new Integer(11), new Integer(12), new Integer(13), new Integer(14), new Integer(15)};

	/**
	 * 构建读对象
	 * @param in 输入流
	 */
	public ObjectReader(InputStream in) {
		this(in, Env.FILE_BUFSIZE);
	}

	/**
	 * 构建读对象
	 * @param in 输入流
	 * @param bufSize 缓冲区大小
	 */
	public ObjectReader(InputStream in, int bufSize) {
		this.in = in;
		buffer = new byte[bufSize];
	}

	private int readBuffer() throws IOException {
		do {
			count = in.read(buffer);
		} while (count == 0);

		if (count > 0) {
			position += count;
			index = 0;
		} else {
			index = -1;
		}

		return count;
	}

	/**
	 * 返回当前的读取位置
	 * @return long
	 */
	public long position() {
		if (count > 0) {
			return position - count + index;
		} else {
			return position;
		}
	}
	
	private void skipFully(int n) throws IOException {
		int total = count - index;
		if (total >= n) {
			index += n;
		} else {
			while (true) {
				if (readBuffer() < 1) {
					throw new EOFException();
				}

				int dif = n - total;
				if (dif <= count) {
					index = dif;
					break;
				} else {
					total += count;
				}
			}
		}
	}

	/**
	 * 跳过指定的字节
	 * @param n 字节数
	 * @throws IOException
	 */
	public int skipBytes(int n) throws IOException {
		if (n < 1) return 0;

		int total = count - index;
		if (total >= n) {
			index += n;
			return n;
		} else {
			if (total != 0) {
				index = count;
			}

			int cur = 0;
			while (total < n && ((cur = (int)in.skip(n - total)) > 0)) {
				position += cur;
				total += cur;
			}

			return total;
		}
	}

	/**
	 * 跳到指定的位置，只能往前跳
	 * @param pos 位置
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		if (pos < position) {
			if (pos < 0) {
				throw new RuntimeException();
			}

			long dif = position - pos;
			if (dif <= count) {
				index = count - (int)dif;
			} else { // 只能往前seek
				throw new RuntimeException();
			}
		} else {
			index = count;
			skip(pos - position);
		}
	}

	/**
	 * 跳过指定的字节
	 * @param n 字节数
	 * @throws IOException
	 */
	public long skip(long n) throws IOException {
		if (n < 1) return 0;

		long total = count - index;
		if (total >= n) {
			index += (int)n;
			return n;
		} else {
			if (total != 0) {
				index = count;
			}

			long cur = 0;
			while (total < n && ((cur = in.skip(n - total)) > 0)) {
				position += cur;
				total += cur;
			}

			return total;
		}
	}

	/**
	 * 是否还有字节没有读完
	 * @return true：是，false：已读完
	 * @throws IOException
	 */
	public boolean hasNext() throws IOException {
		if (count > index) {
			return true;
		} else {
			readBuffer();
			return count > 0;
		}
	}

	/**
	 * 取可用的字节数，注意此方法不是返回全部可用的字节数，只是返回缓冲区中剩余的字节数，如果已到输入流结尾则返回0
	 * @return int 字节数
	 * @throws IOException
	 */
	public int available() throws IOException {
		if (count > index) {
			return count - index; // 不能加is.available()，可能阻塞
		} else {
			//return in.available(); //有时候没结束返回0
			readBuffer();
			return count > 0 ? count : 0;
		}
	}

	/**
	 * 关闭输入流
	 * @throws IOException
	 */
	public void close() throws IOException {
		in.close();
		index = count;
	}

	/**
	 * 读一个布尔值
	 * @return boolean 布尔值
	 * @throws IOException
	 */
	public boolean readBoolean() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return buffer[index++] != 0;
	}

	/**
	 * 读一个字节
	 * @return int 字节值
	 * @throws IOException
	 */
	public int read() throws IOException {
		if (index >= count && readBuffer() < 0) {
			return -1;
		}

		return buffer[index++] & 0xff;
	}

	// 读一个字节，如果已结束则抛出异常
	private int read2() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return buffer[index++] & 0xff;
	}

	/**
	 * 读指定数量字节
	 * @param b 字节数组，用于存放读入的字节
	 * @return int 实际读入都得字节数
	 * @throws IOException
	 */
	public int read(byte []b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * 读指定数量字节
	 * @param b 字节数组，用于存放读入的字节
	 * @param off 起始位置，包括
	 * @param len 长度
	 * @return int 实际读入都得字节数
	 * @throws IOException
	 */
	public int read(byte []b, int off, int len) throws IOException {
		int total = count - index;
		if (total >= len) {
			System.arraycopy(buffer, index, b, off, len);
			index += len;
			return len;
		} else {
			if (total != 0) {
				System.arraycopy(buffer, index, b, off, total);
			}
			
			while (readBuffer() > 0) {
				int rest = len - total;
				if (count >= rest) {
					System.arraycopy(buffer, 0, b, off + total, rest);
					index = rest;
					return len;
				} else {
					System.arraycopy(buffer, 0, b, off + total, count);
					total += count;
				}
			}

			return total > 0 ? total : -1;
		}
	}

	/**
	 * 读指定数量字节，如果剩余字节不足则抛出异常
	 * @param b 字节数组，用于存放读入的字节
	 * @throws IOException
	 */
	public void readFully(byte []b) throws IOException {
		if (read(b, 0, b.length) != b.length) {
			throw new EOFException();
		}
	}

	/**
	 * 读指定数量字节，如果剩余字节不足则抛出异常
	 * @param b 字节数组，用于存放读入的字节
	 * @param off 起始位置，包括
	 * @param len 长度
	 * @throws IOException
	 */
	public void readFully(byte []b, int off, int len) throws IOException {
		if (read(b, off, len) != len) {
			throw new EOFException();
		}
	}

	/**
	 * 读一个字节，如果已结束则抛出异常
	 * @return byte 字节值
	 * @throws IOException
	 */
	public byte readByte() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return buffer[index++];
	}

	/**
	 * 读一个无符号字节，如果已结束则抛出异常
	 * @return int 字节值
	 * @throws IOException
	 */
	public int readUnsignedByte() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return buffer[index++] & 0xff;
	}

	/**
	 * 读一个短整数，如果已结束则抛出异常
	 * @return short
	 * @throws IOException
	 */
	public short readShort() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		int ch1 = buffer[index++] << 8;
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return (short)(ch1 + (buffer[index++] & 0xff));
	}

	/**
	 * 读一个无符号短整数，如果已结束则抛出异常
	 * @return int
	 * @throws IOException
	 */
	public int readUnsignedShort() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		int ch1 = (buffer[index++] & 0xff) << 8;
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return ch1 + (buffer[index++] & 0xff);
	}

	/**
	 * 读一个char，如果已结束则抛出异常
	 * @return char 字节值
	 * @throws IOException
	 */
	public char readChar() throws IOException {
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		int ch1 = buffer[index++] << 8;
		if (index >= count && readBuffer() < 0) {
			throw new EOFException();
		}

		return (char)(ch1 + (buffer[index++] & 0xff));
	}

	/**
	 * 读一个浮点数，如果已结束则抛出异常
	 * @return float
	 * @throws IOException
	 */
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * 读一个双精度浮点数，如果已结束则抛出异常
	 * @return double
	 * @throws IOException
	 */
	public double readDouble() throws IOException {
		int b = read2();
		if (b == ObjectWriter.FLOAT16) {
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
		} else if (b == ObjectWriter.FLOAT32) {
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

	/**
	 * 不支持此方法
	 */
	public String readLine() throws IOException {
		throw new IOException("readLine not supported");
	}

	/**
	 * 读一个用utf编码写出的字符串，如果已结束则抛出异常
	 * @return String
	 * @throws IOException
	 */
	public String readUTF() throws IOException {
		return readString();
	}

	/**
	 * 读一个字节数组，如果已结束则抛出异常
	 * @return 字节数组
	 * @throws IOException
	 */
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

	/**
	 * 读一个字符串数组，如果已结束则抛出异常
	 * @return 字符串数组
	 * @throws IOException
	 */
	public String[] readStrings() throws IOException {
		int len = readInt();
		if (len < 0) {
			return null;
		} else {
			String[] strs = new String[len];
			for (int i = 0; i < len; ++i) {
				strs[i] = (String)readObject();
			}

			return strs;
		}
	}

	private String readString() throws IOException {
		int b = read2();
		int t1 = b & 0xF0;

		if (t1 == ObjectWriter.STRING4 || t1 == ObjectWriter.STRING5) {
			return readString(b & 0x1F);
		} else if (t1 == ObjectWriter.DIGIT4) {
			return readDigits(b & 0x0F);
		} else if (t1 == ObjectWriter.HEX4) {
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
			c = (int)bytearr[i] & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				/* 0xxx xxxx*/
				i++;
				charBuffer[seq++] = (char)c;
				break;
			case 12:
			case 13:

				/* 110x xxxx 10xx xxxx*/
				i += 2;
				if (i > size)
					throw new UTFDataFormatException();
				char2 = (int)bytearr[i - 1];
				if ((char2 & 0xC0) != 0x80)
					throw new UTFDataFormatException();
				charBuffer[seq++] = (char)(((c & 0x1F) << 6) | (char2 & 0x3F));
				break;
			case 14:

				/* 1110 xxxx  10xx xxxx  10xx xxxx */
				i += 3;
				if (i > size)
					throw new UTFDataFormatException();
				char2 = (int)bytearr[i - 2];
				char3 = (int)bytearr[i - 1];
				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
					throw new UTFDataFormatException();
				charBuffer[seq++] = (char)(((c & 0x0F) << 12) |
								  ((char2 & 0x3F) << 6) |
								  ((char3 & 0x3F) << 0));
				break;
			default:

				/* 10xx xxxx,  1111 xxxx */
				throw new UTFDataFormatException();
			}
		}

		// The number of chars produced may be less than utflen
		return new String(charBuffer, 0, seq);
	}

	/**
	 * 读一个长整数，如果已结束则抛出异常
	 * @return long
	 * @throws IOException
	 */
	public long readLong() throws IOException {
		int b = read2();
		if (b == ObjectWriter.LONG0) {
			return 0;
		} else if (b == ObjectWriter.LONG16) {
			return readUInt16();
		} else if (b == ObjectWriter.LONG32) {
			return readInt32();
		} else {
			return readLong64();
		}
	}

	/**
	 * 读一个整数，如果已结束则抛出异常
	 * @return int
	 * @throws IOException
	 */
	public int readInt() throws IOException {
		int b = read2();
		if (b == ObjectWriter.INT16) {
			return readUInt16();
		} else if (b == ObjectWriter.INT32) {
			return readInt32();
		} else if ((b & 0xF0) == ObjectWriter.INT12) {
			return ((b & 0x0F) << 8) + read2();
		} else { // INT4
			return b & 0x0F;
		}
	}

	private int readUInt16() throws IOException {
		if (count - index > 1) {
			int n = ((buffer[index] & 0xFF) << 8) + (buffer[index + 1] & 0xFF);
			index += 2;
			return n;
		} else if (count == index) {
			if (readBuffer() < 1) {
				throw new EOFException();
			}

			return readUInt16();
		} else {
			int n = ((buffer[index] & 0xFF) << 8);
			if (readBuffer() < 1) {
				throw new EOFException();
			}

			this.index = 1;
			return n + (buffer[0] & 0xFF);
		}
	}
	
	private int readUInt24() throws IOException {
		if (count - index >= 3) {
			byte []data = this.buffer;
			int index = this.index;
			this.index += 3;
			return ((data[index] & 0xff) << 16) +
				((data[index + 1] & 0xff) << 8) + (data[index + 2] & 0xff);
		} else {
			byte []readBuffer = this.readBuffer;
			readFully(readBuffer, 0, 3);
			return ((readBuffer[0] & 0xff) << 16) +
				((readBuffer[1] & 0xff) << 8) + (readBuffer[2] & 0xff);
		}
	}

	/**
	 * 读一个4字节整数，如果已结束则抛出异常
	 * @return int
	 * @throws IOException
	 */
	public int readInt32() throws IOException {
		if (count - index >= 4) {
			byte []data = this.buffer;
			int index = this.index;
			this.index += 4;
			return (data[index] << 24) + ((data[index + 1] & 0xff) << 16) +
				((data[index + 2] & 0xff) << 8) + (data[index + 3] & 0xff);
		} else {
			byte []readBuffer = this.readBuffer;
			readFully(readBuffer, 0, 4);
			return (readBuffer[0] << 24) + ((readBuffer[1] & 0xff) << 16) +
				((readBuffer[2] & 0xff) << 8) + (readBuffer[3] & 0xff);
		}
	}

	private long readULong32() throws IOException {
		if (count - index >= 4) {
			byte []data = this.buffer;
			int index = this.index;
			this.index += 4;
			return ((long)(data[index] & 0xff) << 24) + ((data[index + 1] & 0xff) << 16) +
				((data[index + 2] & 0xff) << 8) + (data[index + 3] & 0xff);
		} else {
			byte []readBuffer = this.readBuffer;
			readFully(readBuffer, 0, 4);
			return ((long)(readBuffer[0] & 0xff) << 24) + ((readBuffer[1] & 0xff) << 16) +
				((readBuffer[2] & 0xff) << 8) + (readBuffer[3] & 0xff);
		}
	}

	/**
	 * 读一个5字节长整数，如果已结束则抛出异常
	 * @return long
	 * @throws IOException
	 */
	public long readLong40() throws IOException {
		if (count - index >= 5) {
			byte []data = this.buffer;
			int index = this.index;
			this.index += 5;
			return (((long)(data[index] & 0xff) << 32) +
					((long)(data[index + 1] & 0xff) << 24) +
					((data[index + 2] & 0xff) << 16) +
					((data[index + 3] & 0xff) <<  8) +
					(data[index + 4] & 0xff));
		} else {
			byte []readBuffer = this.readBuffer;
			readFully(readBuffer, 0, 5);
			return (((long)(readBuffer[0] & 0xff) << 32) +
					((long)(readBuffer[1] & 0xff) << 24) +
					((readBuffer[2] & 0xff) << 16) +
					((readBuffer[3] & 0xff) <<  8) +
					(readBuffer[4] & 0xff));
		}
	}
	
	/**
	 * 读一个8字节长整数，如果已结束则抛出异常
	 * @return long
	 * @throws IOException
	 */
	public long readLong64() throws IOException {
		if (count - index >= 8) {
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
		} else {
			byte []readBuffer = this.readBuffer;
			readFully(readBuffer, 0, 8);
			return (((long)readBuffer[0] << 56) +
					((long)(readBuffer[1] & 0xff) << 48) +
					((long)(readBuffer[2] & 0xff) << 40) +
					((long)(readBuffer[3] & 0xff) << 32) +
					((long)(readBuffer[4] & 0xff) << 24) +
					((readBuffer[5] & 0xff) << 16) +
					((readBuffer[6] & 0xff) <<  8) +
					(readBuffer[7] & 0xff));
		}
	}

	private Object readMark0(int b) throws IOException {
		switch (b) {
		case ObjectWriter.NULL:
			return null;
		case ObjectWriter.FLOAT0:
			return FLOAT0;
		case ObjectWriter.DECIMAL0:
			return DECIMAL0;
		case ObjectWriter.LONG0:
			return LONG0;
		case ObjectWriter.TRUE:
			return Boolean.TRUE;
		default: // ObjectWriter.FALSE
			return Boolean.FALSE;
		}
	}

	private Object readMark1(int b) throws IOException {
		switch (b) {
		case ObjectWriter.INT16:
			return ObjectCache.getInteger(readUInt16());
			//return new Integer(readUInt16());
		case ObjectWriter.INT32:
			return new Integer(readInt32());
		case ObjectWriter.FLOAT16:
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
		case ObjectWriter.FLOAT32:
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
		case ObjectWriter.FLOAT64:
			return new Double(Double.longBitsToDouble(readLong64()));
		case ObjectWriter.LONG16:
			return new Long(readUInt16());
		case ObjectWriter.LONG32:
			return new Long(readInt32());
		default:
			return new Long(readLong64());
		}
	}

	private void skipMark1(int b) throws IOException {
		switch (b) {
		case ObjectWriter.INT16:
		case ObjectWriter.FLOAT16:
		case ObjectWriter.LONG16:
			skipFully(2);
			break;
		case ObjectWriter.INT32:
		case ObjectWriter.FLOAT32:
		case ObjectWriter.LONG32:
			skipFully(4);
			break;
		default: // FLOAT64 LONG64
			skipFully(8);
		}
	}

	private Object readMark2(int b) throws IOException {
		switch (b) {
		case ObjectWriter.STRING:
			return readString(readInt());
		case ObjectWriter.DECIMAL:
			int scale = read2();
			int len = read2();
			byte[] buf = new byte[len];
			readFully(buf, 0, len);
			return new BigDecimal(new BigInteger(buf), scale);
		case ObjectWriter.SEQUENCE:
			len = readInt();
			Sequence seq = new Sequence(len);
			for (int i = 0; i < len; ++i) {
				seq.add(readObject());
			}

			return seq;
		case ObjectWriter.TABLE:
			return readTable();
		case ObjectWriter.RECORD:
			return readRecord();
		case ObjectWriter.BLOB:
			return readByteArray();
		default: // ObjectWriter.AVG
			AvgValue avg = new AvgValue();
			avg.readData(this);
			return avg;
		}
	}

	private void skipMark2(int b) throws IOException {
		switch (b) {
		case ObjectWriter.STRING:
			skipFully(readInt());
			break;
		case ObjectWriter.DECIMAL:
			skipFully(1);
			skipFully(read2());
			break;
		case ObjectWriter.SEQUENCE:
			int len = readInt();
			for (int i = 0; i < len; ++i) {
				skipObject();
			}
			
			break;
		case ObjectWriter.TABLE:
			skipTable();
			break;
		default: // BLOB
			int n = readInt();
			if (n > 0) skipFully(n);
		}
	}

	private Object readMark3(int b) throws IOException {
		switch (b) {
		case ObjectWriter.DATE16:
			return DateCache.getDate(readUInt16());
			//return new java.sql.Date(ObjectWriter.BASEDATE + readUInt16() * 86400000L);
		case ObjectWriter.DATE32:
			return new java.sql.Date(ObjectWriter.BASEDATE - readULong32() * 1000L);
		case ObjectWriter.DATETIME32:
			return new java.sql.Timestamp(readULong32() * 1000L);
		case ObjectWriter.DATETIME33:
			return new java.sql.Timestamp(readULong32() * -1000L);
		case ObjectWriter.DATETIME64:
			return new java.sql.Timestamp(readLong64());
		case ObjectWriter.TIME16:
			return new java.sql.Time(ObjectWriter.BASETIME + readUInt16() * 1000);
		case ObjectWriter.TIME17:
			return new java.sql.Time(ObjectWriter.BASETIME + (0x10000 | readUInt16()) * 1000);
		case ObjectWriter.TIME32:
			return new java.sql.Time(ObjectWriter.BASETIME + readInt32());
		case ObjectWriter.DATE24:
			return new java.sql.Date(ObjectWriter.BASEDATE + readUInt24() * 86400000L);
		default: // ObjectWriter.DATE64
			return new java.sql.Date(readLong64());
		}
	}

	private void skipMark3(int b) throws IOException {
		switch (b) {
		case ObjectWriter.DATE32:
		case ObjectWriter.DATETIME32:
		case ObjectWriter.DATETIME33:
		case ObjectWriter.TIME32:
			skipFully(4);
			break;
		case ObjectWriter.DATETIME64:
			skipFully(8);
			break;
		case ObjectWriter.DATE24:
			skipFully(3);
			break;
		default: // ObjectWriter.DATE16 ObjectWriter.TIME16 ObjectWriter.TIME17
			skipFully(2);
		}
	}

	/**
	 * 读一个对象
	 * @return Object
	 * @throws IOException
	 */
	public Object readObject() throws IOException {
		int b = read2();
		switch(b & 0xF0) {
		case ObjectWriter.MARK0:
			return readMark0(b);
		case ObjectWriter.MARK1:
			return readMark1(b);
		case ObjectWriter.MARK2:
			return readMark2(b);
		case ObjectWriter.MARK3:
			return readMark3(b);
		case ObjectWriter.INT4:
			return INT15[b & 0x0F];
		case ObjectWriter.INT12:
			return ObjectCache.getInteger(((b & 0x0F) << 8) + read2());
			//return new Integer(((b & 0x0F) << 8) + read2());
		case ObjectWriter.STRING4:
		case ObjectWriter.STRING5:
			return readString(b & 0x1F);
		case ObjectWriter.DIGIT4:
			return readDigits(b & 0x0F);
		case ObjectWriter.HEX4:
			return HEXSTRINGS[b & 0x0F];
		case ObjectWriter.SERIALBYTES:
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

	/**
	 * 跳过一个对象
	 * @throws IOException
	 */
	public void skipObject() throws IOException {
		int b = read2();
		switch(b & 0xF0) {
		case ObjectWriter.MARK0:
			break;
		case ObjectWriter.MARK1:
			skipMark1(b);
			break;
		case ObjectWriter.MARK2:
			skipMark2(b);
			break;
		case ObjectWriter.MARK3:
			skipMark3(b);
			break;
		case ObjectWriter.INT4:
			break;
		case ObjectWriter.INT12:
			skipFully(1);
			break;
		case ObjectWriter.STRING4:
		case ObjectWriter.STRING5:
			skipFully(b & 0x1F);
			break;
		case ObjectWriter.DIGIT4:
			skipFully(b & 0x0F);
			break;
		case ObjectWriter.HEX4:
			break;
		case ObjectWriter.SERIALBYTES:
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
		int fcount = readInt();
		String []names = new String[fcount];
		for (int i = 0; i < fcount; ++i) {
			names[i] = readString();
		}
		
		Record r = new Record(new DataStruct(names));
		for (int f = 0; f < fcount; ++f) {
			r.setNormalFieldValue(f, readObject());
		}
		
		return r;
	}
	
	private Table readTable() throws IOException {
		int fcount = readInt();
		String []names = new String[fcount];
		for (int i = 0; i < fcount; ++i) {
			names[i] = readString();
		}

		Object []vals = new Object[fcount];
		int len = readInt();
		Table table = new Table(names, len);
		
		for (int i = 1; i <= len; ++i) {
			for (int f = 0; f < fcount; ++f) {
				vals[f] = readObject();
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
	
	public InputStream getInputStream() {
		return in;
	}
	
	public void setInputStream(InputStream in) {
		this.in = in;
	}
	
	public ObjectReader(ObjectReader reader) {
		in = reader.in; 
		buffer = reader.buffer.clone(); // 每次读入的字节缓存
		index = reader.index; // 下一字节在buffer中的索引
		count = reader.count; // 读入buffer的实际字节数目
		position = reader.position;
	}
}
