package com.scudata.common;

import java.io.*;
import java.util.*;
import java.math.*;

/**
 * 用于把字节流读成对象，可以支持读取不同版本程序保存的数据
 * @author RunQian
 *
 */
public class ByteArrayInputRecord {
	private ByteArrayInputStream in;

	public ByteArrayInputRecord(byte[] buf) {
		in = new ByteArrayInputStream(buf);
	}

	public boolean readBoolean() throws IOException {
		int ch = in.read();
		return (ch == 1) ? true : false;
	}

	public byte readByte() throws IOException {
		int ch = in.read();
		if (ch < 0)
			throw new EOFException();
		return (byte) (ch);
	}

	public byte[] readBytes() throws IOException {
		int len = readInt();
		if (len <= 0) {
			return null;
		} else {
			byte[] buf = new byte[len];
			readFully(buf);
			return buf;
		}
	}

	public ArrayList<Byte> readByteArray() throws IOException {
		short len = readShort();
		if (len <= 0) {
			return null;
		} else {
			ArrayList<Byte> buf = new ArrayList<Byte>(len);
			for (int i = 0; i < len; i++) {
				buf.add(new Byte(readByte()));
			}
			return buf;
		}
	}

	public short readShort() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (short) ((ch1 << 8) + (ch2 << 0));
	}

	public final int readUnsignedShort() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (ch1 << 8) + (ch2 << 0);
	}

	public int readInt() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	public long readLong() throws IOException {
		return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
	}

	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	public String readString() throws IOException {
		int size = readInt();
		if (size == -1)
			return null;
		else {
			StringBuffer str = new StringBuffer(size);
			byte bytearr[] = new byte[size];
			int c, char2, char3;
			int count = 0;

			readFully(bytearr, 0, size);

			while (count < size) {
				c = (int) bytearr[count] & 0xff;
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
					count++;
					str.append((char) c);
					break;
				case 12:
				case 13:
					/* 110x xxxx 10xx xxxx */
					count += 2;
					if (count > size)
						throw new UTFDataFormatException();
					char2 = (int) bytearr[count - 1];
					if ((char2 & 0xC0) != 0x80)
						throw new UTFDataFormatException();
					str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
					break;
				case 14:
					/* 1110 xxxx 10xx xxxx 10xx xxxx */
					count += 3;
					if (count > size)
						throw new UTFDataFormatException();
					char2 = (int) bytearr[count - 2];
					char3 = (int) bytearr[count - 1];
					if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
						throw new UTFDataFormatException();
					str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
					break;
				default:
					/* 10xx xxxx, 1111 xxxx */
					throw new UTFDataFormatException();
				}
			}
			// The number of chars produced may be less than utflen
			return new String(str);
		}
	}

	public String[] readStrings() throws IOException {
		short size = readShort();
		if (size <= 0)
			return null;
		else {
			String[] buf = new String[size];
			for (int i = 0; i < size; i++) {
				buf[i] = readString();
			}
			return buf;
		}
	}

	public ArrayList<String> readStringArray() throws IOException {
		short size = readShort();
		if (size <= 0)
			return null;
		else {
			ArrayList<String> buf = new ArrayList<String>(size);
			for (int i = 0; i < size; i++) {
				buf.add(readString());
			}
			return buf;
		}
	}

	public int readFully(byte[] b, int off, int len) throws IOException {
		int total = 0;
		for (;;) {
			int got = in.read(b, off + total, len - total);
			if (got < 0) {
				return (total == 0) ? -1 : total;
			} else {
				total += got;
				if (total == len)
					return total;
			}
		}
	}

	public int readFully(byte[] b) throws IOException {
		return readFully(b, 0, b.length);
	}

	public String readUTF() throws IOException {
		int utflen = readUnsignedShort();
		StringBuffer str = new StringBuffer(utflen);
		byte bytearr[] = new byte[utflen];
		int c, char2, char3;
		int count = 0;

		readFully(bytearr, 0, utflen);

		while (count < utflen) {
			c = (int) bytearr[count] & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				/* 0xxxxxxx */
				count++;
				str.append((char) c);
				break;
			case 12:
			case 13:
				/* 110x xxxx 10xx xxxx */
				count += 2;
				if (count > utflen)
					throw new UTFDataFormatException();
				char2 = (int) bytearr[count - 1];
				if ((char2 & 0xC0) != 0x80)
					throw new UTFDataFormatException();
				str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
				break;
			case 14:
				/* 1110 xxxx 10xx xxxx 10xx xxxx */
				count += 3;
				if (count > utflen)
					throw new UTFDataFormatException();
				char2 = (int) bytearr[count - 2];
				char3 = (int) bytearr[count - 1];
				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
					throw new UTFDataFormatException();
				str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
				break;
			default:
				/* 10xx xxxx, 1111 xxxx */
				throw new UTFDataFormatException();
			}
		}
		// The number of chars produced may be less than utflen
		return new String(str);
	}

	public IRecord readRecord(IRecord r) throws IOException, ClassNotFoundException {
		int size = readInt();
		if (size <= 0)
			return null;
		else {
			byte[] s = new byte[size];
			readFully(s);
			r.fillRecord(s);
			return r;
		}
	}

	// 不要随便用这个方法，用这个方法无法获取非数据非字符串对象，由于无法判断对象具体类名称，所以只读出byte[]
	public Object readObject(boolean test) throws IOException, ClassNotFoundException {
		int b = readByte();
		switch (b) {
		case -1:
			return null;
		case 0:
			String className = readString();
			IRecord rec = null;
			try {
				rec = (IRecord) Class.forName(className).newInstance();
			} catch (Exception e) {
				// 包路径由com.raqsoft修改为了com.scudata，旧包保存的对象可能用到了IRecord的派生类
				if (className.startsWith("com.raqsoft.")) {
					try {
						className = "com.scudata." + className.substring(12);
						rec = (IRecord) Class.forName(className).newInstance();
					} catch (Exception e2) {
						e.printStackTrace();
					}
				} else {
					e.printStackTrace();
				}
			}
			return readRecord(rec);
		case 1:
			return readUTF();
		case 2:
			int scale = readInt();
			byte[] bb = readBytes();
			return new BigDecimal(new BigInteger(bb), scale);
		case 31:
			return new java.sql.Timestamp(readLong());
		case 32:
			return new java.sql.Time(readLong());
		case 33:
			return new java.sql.Date(readLong());
		case 3:
			return new Date(readLong());
		case 4:
			return new Integer(readInt());
		case 5:
			return new Long(readLong());
		case 6:
			return (readByte() == 1) ? Boolean.TRUE : Boolean.FALSE;
		case 7:
			return new BigInteger(readBytes());
		case 8:
			return readBytes();
		case 9:
			return new Double(readDouble());
		case 10:
			return new Float(readFloat());
		case 11:
			return new Byte(readByte());
		case 12:
			return new Short(readShort());
		case 13:
			return readString();
		default:
			return readBytes();
		}
	}

	public boolean readBoolean2() throws IOException, EOFException {
		if (in.available() > 0) {
			int ch = in.read();
			return (ch == 1) ? true : false;
		} else {
			throw new EOFException();
		}
	}

	public byte readByte2() throws IOException, EOFException, EOFException {
		if (in.available() > 0) {
			int ch = in.read();
			if (ch < 0)
				throw new EOFException();
			return (byte) (ch);
		} else {
			throw new EOFException();
		}
	}

	public byte[] readBytes2() throws IOException, EOFException {
		if (in.available() > 0) {
			int len = readInt();
			if (len <= 0) {
				return null;
			} else {
				byte[] buf = new byte[len];
				readFully(buf);
				return buf;
			}
		} else {
			throw new EOFException();
		}
	}

	public ArrayList<Byte> readByteArray2() throws IOException, EOFException {
		if (in.available() > 0) {
			short len = readShort();
			if (len <= 0) {
				return null;
			} else {
				ArrayList<Byte> buf = new ArrayList<Byte>(len);
				for (int i = 0; i < len; i++) {
					buf.add(new Byte(readByte()));
				}
				return buf;
			}
		} else {
			throw new EOFException();
		}
	}

	public short readShort2() throws IOException, EOFException {
		if (in.available() > 0) {
			int ch1 = in.read();
			int ch2 = in.read();
			if ((ch1 | ch2) < 0)
				throw new EOFException();
			return (short) ((ch1 << 8) + (ch2 << 0));
		} else {
			throw new EOFException();
		}
	}

	public int readInt2() throws IOException, EOFException {
		if (in.available() > 0) {
			int ch1 = in.read();
			int ch2 = in.read();
			int ch3 = in.read();
			int ch4 = in.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0)
				throw new EOFException();
			return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
		} else {
			throw new EOFException();
		}
	}

	public float readFloat2() throws IOException, EOFException {
		if (in.available() > 0) {
			return Float.intBitsToFloat(readInt());
		} else {
			throw new EOFException();
		}
	}

	public long readLong2() throws IOException, EOFException {
		if (in.available() > 0) {
			return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
		} else {
			throw new EOFException();
		}
	}

	public double readDouble2() throws IOException, EOFException {
		if (in.available() > 0) {
			return Double.longBitsToDouble(readLong());
		} else {
			throw new EOFException();
		}
	}

	public String readString2() throws IOException, EOFException {
		if (in.available() > 0) {
			return readString();
		} else {
			throw new EOFException();
		}
	}

	public String[] readStrings2() throws IOException, EOFException {
		if (in.available() > 0) {
			short size = readShort();
			if (size <= 0)
				return null;
			else {
				String[] buf = new String[size];
				for (int i = 0; i < size; i++) {
					buf[i] = readString();
				}
				return buf;
			}
		} else {
			throw new EOFException();
		}
	}

	public ArrayList<String> readStringArray2() throws IOException, EOFException {
		if (in.available() > 0) {
			short size = readShort();
			if (size <= 0)
				return null;
			else {
				ArrayList<String> buf = new ArrayList<String>(size);
				for (int i = 0; i < size; i++) {
					buf.add(readString());
				}
				return buf;
			}
		} else {
			throw new EOFException();
		}
	}

	public int readFully2(byte[] b, int off, int len) throws IOException, EOFException {
		if (in.available() > 0) {
			int total = 0;
			for (;;) {
				int got = in.read(b, off + total, len - total);
				if (got < 0) {
					return (total == 0) ? -1 : total;
				} else {
					total += got;
					if (total == len)
						return total;
				}
			}
		} else {
			throw new EOFException();
		}
	}

	public int readFully2(byte[] b) throws IOException, EOFException {
		if (in.available() > 0) {
			return readFully(b, 0, b.length);
		} else {
			throw new EOFException();
		}
	}

	public String readUTF2() throws IOException {
		if (in.available() > 0) {
			int utflen = readShort();
			StringBuffer str = new StringBuffer(utflen);
			byte bytearr[] = new byte[utflen];
			int c, char2, char3;
			int count = 0;

			readFully(bytearr, 0, utflen);

			while (count < utflen) {
				c = (int) bytearr[count] & 0xff;
				switch (c >> 4) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
					/* 0xxxxxxx */
					count++;
					str.append((char) c);
					break;
				case 12:
				case 13:
					/* 110x xxxx 10xx xxxx */
					count += 2;
					if (count > utflen)
						throw new UTFDataFormatException();
					char2 = (int) bytearr[count - 1];
					if ((char2 & 0xC0) != 0x80)
						throw new UTFDataFormatException();
					str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
					break;
				case 14:

					/* 1110 xxxx 10xx xxxx 10xx xxxx */
					count += 3;
					if (count > utflen)
						throw new UTFDataFormatException();
					char2 = (int) bytearr[count - 2];
					char3 = (int) bytearr[count - 1];
					if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
						throw new UTFDataFormatException();
					str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
					break;
				default:
					/* 10xx xxxx, 1111 xxxx */
					throw new UTFDataFormatException();
				}
			}
			// The number of chars produced may be less than utflen
			return new String(str);
		} else {
			throw new EOFException();
		}
	}

	public IRecord readRecord2(IRecord r) throws IOException, ClassNotFoundException, EOFException {
		if (in.available() > 0) {
			int size = readInt();
			if (size <= 0)
				return null;
			else {
				byte[] s = new byte[size];
				in.read(s);
				r.fillRecord(s);
				return r;
			}
		} else {
			throw new EOFException();
		}
	}

	// 不要随便用这个方法，用这个方法无法获取非数据非字符串对象，由于无法判断对象具体类名称，所以只读出byte[]
	public Object readObject2(boolean test) throws IOException, ClassNotFoundException, EOFException {
		if (in.available() > 0) {
			int b = readByte();
			switch (b) {
			case -1:
				return null;
			case 1:
				return readUTF();
			case 2:
				int scale = readInt();
				byte[] bb = readBytes();
				return new BigDecimal(new BigInteger(bb), scale);
			case 3:
				return new Date(readLong());
			case 4:
				return new Integer(readInt());
			case 5:
				return new Long(readLong());
			case 6:
				return (readByte() == 1) ? Boolean.TRUE : Boolean.FALSE;
			case 7:
				return new BigInteger(readBytes());
			case 8:
				return readBytes();
			case 9:
				return new Double(readDouble());
			case 10:
				return new Float(readFloat());
			case 11:
				return new Byte(readByte());
			case 12:
				return new Short(readShort());
			default:
				return readBytes();
			}
		} else {
			throw new EOFException();
		}
	}

	public int available() {
		return in.available();
	}

}
