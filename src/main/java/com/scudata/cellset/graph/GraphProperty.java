package com.scudata.cellset.graph;

import java.io.*;

import com.scudata.common.*;

/**
 * 公有属性之外的数据属性
 * @author Joancy
 *
 */
public class GraphProperty extends PublicProperty {
	private final static long serialVersionUID = 82857881736578L;
	/** 分类定义 */
	private String category; // 分类单元格（源格）
	private String series; // 系列单元格，可空（源格）
	private String value; // 值单元格（源格）

	private int x, y, w = 400, h = 260;

	/**
	 * 取统计图分类定义
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * 值单元格
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * 获取系列表达式
	 * @return 系列表达式 
	 */
	public String getSeries() {
		return series;
	}

	/**
	 * x坐标
	 * @return 坐标值
	 */
	public int getX() {
		return x;
	}

	/**
	 * y坐标
	 * @return 坐标值
	 */
	public int getY() {
		return y;
	}

	/**
	 * 图形宽度
	 * @return 宽度值
	 */
	public int getW() {
		return w;
	}

	/**
	 * 图形高度
	 * @return 高度值
	 */
	public int getH() {
		return h;
	}

	/**
	 * 设置统计图分类定义
	 * 
	 * @param categories
	 *            统计图分类定义
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * 值表达式
	 * @param value 表达式
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * 设置系列表达式
	 * @param series 表达式
	 */
	public void setSeries(String series) {
		this.series = series;
	}

	/**
	 * 设置横坐标
	 * @param x 坐标值
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * 设置纵坐标
	 * @param y 坐标值
	 */
	public void setY(int y) {
		this.y = y;
	}

	/**
	 * 设置图形宽度
	 * @param w 宽度值
	 */
	public void setW(int w) {
		this.w = w;
	}

	/**
	 * 设置图形高度
	 * @param h 高度值
	 */
	public void setH(int h) {
		this.h = h;
	}

	/**
	 * 设置坐标位置
	 * @param x 横坐标
	 * @param y 纵坐标
	 */
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * 设置大小
	 * @param w 宽度
	 * @param h 高度
	 */
	public void setSize(int w, int h) {
		this.w = w;
		this.h = h;
	}

	/**
	 * 深度克隆
	 * 
	 * @return Object 克隆后的图形属性
	 */
	public Object deepClone() {
		GraphProperty gp = new GraphProperty();
		gp.setCategory(category);
		gp.setValue(value);
		gp.setSeries(series);
		gp.setLocation(getX(),getY());
		gp.setSize(getW(), getH());
		gp.setPublicProperty(this);
		return gp;
	}

	/**
	 * 实现序列化接口
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		byte ver = 1;
		out.writeByte(ver);
		out.writeObject(category);
		out.writeObject(value);
		out.writeObject(series);
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(w);
		out.writeInt(h);
	}

	/**
	 * 实现序列化接口
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		byte ver = in.readByte();
		category = (String) in.readObject();
		value = (String) in.readObject();
		series = (String) in.readObject();

		x = in.readInt();
		y = in.readInt();
		w = in.readInt();
		h = in.readInt();
	}

	/**
	 * 实现IRecord接口
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeBytes(super.serialize());
		
		out.writeString(category);
		out.writeString(value);
		out.writeString(series);

		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(w);
		out.writeInt(h);
		return out.toByteArray();
	}

	/**
	 * 实现IRecord接口
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		byte[] superRecord = in.readBytes();
		super.fillRecord( superRecord );
		
		category = in.readString();
		value = in.readString();
		series = in.readString();

		x = in.readInt();
		y = in.readInt();
		w = in.readInt();
		h = in.readInt();
	}

}
