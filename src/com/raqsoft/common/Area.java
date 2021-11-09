package com.raqsoft.common;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 区域类，表示一块区域
 */
public class Area implements ICloneable, Externalizable, Cloneable,
		Comparable<Area>, IRecord {
	private static final long serialVersionUID = 1l;
	/** 列类型由short改成了int */
	private static byte version = (byte) 2;

	/** 起始行号，结束行号 */
	private int r1 = -1, r2 = -1;
	/** 起始列号，结束列号 */
	private int c1 = -1, c2 = -1;

	public Area() {
	}

	/**
	 * 构造函数
	 * 
	 * @param r1
	 *            int 起始行号
	 * @param r2
	 *            int 结束行号
	 */
	public Area(int r1, int r2) {
		this.r1 = r1;
		this.r2 = r2;
	}

	/**
	 * 构造函数
	 * 
	 * @param r1
	 *            int 起始行号
	 * @param c1
	 *            int 起始列号
	 * @param r2
	 *            int 结束行号
	 * @param c2
	 *            int 结束列号
	 */
	public Area(int r1, int c1, int r2, int c2) {
		this.r1 = r1;
		this.r2 = r2;

		this.c1 = c1;
		this.c2 = c2;
	}

	/**
	 * 取得起始行号
	 * 
	 * @return int
	 */
	public int getBeginRow() {
		return r1;
	}

	/**
	 * 设置起始行号
	 * 
	 * @param r
	 *            int 行号
	 */
	public void setBeginRow(int r) {
		this.r1 = r;
	}

	/**
	 * 得到结束行号
	 * 
	 * @return int
	 */
	public int getEndRow() {
		return r2;
	}

	/**
	 * 设置结束行号
	 * 
	 * @param r
	 *            int
	 */
	public void setEndRow(int r) {
		this.r2 = r;
	}

	/**
	 * 取得起始列号
	 * 
	 * @return int
	 */
	public int getBeginCol() {
		return this.c1;
	}

	/**
	 * 设置起始列号
	 * 
	 * @param c
	 *            int
	 */
	public void setBeginCol(int c) {
		this.c1 = c;
	}

	/**
	 * 取得结束列号
	 * 
	 * @return int
	 */
	public int getEndCol() {
		return this.c2;
	}

	/**
	 * 设置结束列号
	 * 
	 * @param c
	 *            int
	 */
	public void setEndCol(int c) {
		this.c2 = c;
	}

	public void setArea(int br, int bc, int er, int ec) {
		this.r1 = br;
		this.r2 = er;
		this.c1 = bc;
		this.c2 = ec;
	}

	/**
	 * 与另一个区域对象进行比较，依次比较起始行、起始列、结束行、结束列， 若比较过程中存在不相等则返回其差，否则返回0
	 * 
	 * @param other
	 *            Area
	 * @return int
	 */
	public int compareTo(Area other) {
		int x = r1 - other.r1;
		if (x != 0) {
			return x;
		}
		x = c1 - other.c1;
		if (x != 0) {
			return x;
		}
		x = r2 - other.r2;
		if (x != 0) {
			return x;
		}
		return c2 - other.c2;
	}

	/**
	 * 判断row,col是否在当前区域内
	 * 
	 * @param row
	 *            int
	 * @param col
	 *            int
	 * @return boolean
	 */
	public boolean contains(int row, int col) {
		return (r1 <= row && r2 >= row && c1 <= col && c2 >= col);
	}

	/**
	 * 是否包含另一区域
	 * 
	 * @param a
	 *            Area
	 * @return boolean
	 */
	public boolean contains(Area a) {
		return contains(a.getBeginRow(), a.getBeginCol())
				&& contains(a.getEndRow(), a.getEndCol())
				&& contains(a.getBeginRow(), a.getEndCol())
				&& contains(a.getEndRow(), a.getBeginCol());
	}

	/**
	 * 序列化输出本类
	 * 
	 * @param out
	 *            ObjectOutput
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(version);
		out.writeInt(r1);
		out.writeInt(r2);
		out.writeInt(c1);
		out.writeInt(c2);
	}

	/**
	 * 序列化输入本类
	 * 
	 * @param in
	 *            ObjectInput
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		in.readByte();
		r1 = in.readInt();
		r2 = in.readInt();
		c1 = in.readInt();
		c2 = in.readInt();
	}

	/**
	 * 序列化输出本类
	 * 
	 * @return ObjectOutput
	 * @throws IOException
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeInt(r1);
		out.writeInt(r2);
		out.writeInt(c1);
		out.writeInt(c2);
		return out.toByteArray();
	}

	/**
	 * 序列化输入本类
	 * 
	 * @param buf
	 *            byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		r1 = in.readInt();
		r2 = in.readInt();
		c1 = in.readInt();
		c2 = in.readInt();
	}

	/**
	 * 克隆本类
	 * 
	 * @return Object
	 */
	public Object deepClone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Error when clone Area");
		}
	}
}
