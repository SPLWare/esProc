package com.raqsoft.cellset.graph.config;

import java.io.*;
import java.awt.*;
import com.raqsoft.common.*;

/**
 * 统计图中的警戒线属性类
 */
public class AlarmLine implements ICloneable, Externalizable, IRecord {
	private final static long serialVersionUID = 82857881736578L;
	private byte version = (byte) 3;
	// 版本3，增加 标识警戒值

	/** 　警戒线名称　 */
	private String name;
	/** 　警戒值　 */
	private String alarmValue;
	/** 　警戒线类型　 */
	private byte lineType = IGraphProperty.LINE_SOLID;
	/** 警戒线颜色 */
	private int color = Color.red.getRGB();

	private byte lineThick = 1; /* 折线图的粗细度 */
	private boolean isDrawAlarmValue = true; /* 标识警戒值 */

	/**
	 * 设置名称
	 * 
	 * @param name
	 *            名称
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置警戒值
	 * 
	 * @param value
	 *            警戒值
	 */
	public void setAlarmValue(String value) {
		this.alarmValue = value;
	}

	/**
	 * 设置是否绘制警戒值
	 * 
	 * @param isDrawAlarmValue
	 *            是否绘制
	 */
	public void setDrawAlarmValue(boolean isDrawAlarmValue) {
		this.isDrawAlarmValue = isDrawAlarmValue;
	}

	/**
	 * 取是否绘制警戒值
	 * 
	 * @return 绘制返回true，否则返回false
	 */
	public boolean isDrawAlarmValue() {
		return isDrawAlarmValue;
	}

	/**
	 * 设置警戒线类型
	 * 
	 * @param type
	 *            警戒线类型，取值为GraphProperty.LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *            LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public void setLineType(byte type) {
		this.lineType = type;
	}

	/**
	 * 设置颜色
	 * 
	 * @param color
	 *            颜色值
	 */
	public void setColor(int color) {
		this.color = color;
	}

	/**
	 * 取名称
	 * 
	 * @return String　名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 取警戒值
	 * 
	 * @return String　警戒值
	 */
	public String getAlarmValue() {
		return alarmValue;
	}

	/**
	 * 取警戒线类型
	 * 
	 * @return byte　警戒线类型，值为GraphProperty.LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *         LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public byte getLineType() {
		return lineType;
	}

	/**
	 * 取颜色
	 * 
	 * @return int　颜色
	 */
	public int getColor() {
		return color;
	}

	/**
	 * 警戒线粗细度
	 * 
	 * @return byte 粗度
	 */
	public byte getLineThick() {
		return lineThick;
	}

	/**
	 * 设置线的粗度
	 * 
	 * @param thick
	 *            粗度值
	 */
	public void setLineThick(byte thick) {
		if (thick < 0 || thick > 10) {
			thick = 10;
		}
		lineThick = thick;
	}

	/**
	 * 深度克隆
	 * 
	 * @return Object 克隆的警戒线对象
	 */
	public Object deepClone() {
		AlarmLine line = new AlarmLine();
		line.setName(name);
		line.setAlarmValue(alarmValue);
		line.setColor(color);
		line.setLineType(lineType);
		line.setLineThick(lineThick);
		line.setDrawAlarmValue(isDrawAlarmValue);
		return line;
	}

	/**
	 * 实现序列化接口
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(version);
		out.writeObject(name);
		out.writeObject(alarmValue);
		out.writeByte(lineType);
		out.writeInt(color);
		out.writeByte(lineThick);
		out.writeBoolean(isDrawAlarmValue);
	}

	/**
	 * 实现序列化接口
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		byte ver = in.readByte();
		name = (String) in.readObject();
		alarmValue = (String) in.readObject();
		lineType = in.readByte();
		color = in.readInt();
		if (ver > 1) {
			lineThick = in.readByte();
		}
		if (ver > 2) {
			isDrawAlarmValue = in.readBoolean();
		}
	}

	/**
	 * 实现IRecord接口
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeString(name);
		out.writeString(alarmValue);
		out.writeByte(lineType);
		out.writeInt(color);
		out.writeByte(lineThick);
		out.writeBoolean(isDrawAlarmValue);
		return out.toByteArray();
	}

	/**
	 * 实现IRecord接口
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		name = in.readString();
		alarmValue = in.readString();
		lineType = in.readByte();
		color = in.readInt();
		if (in.available() > 0) {
			lineThick = in.readByte();
		}
		if (in.available() > 0) {
			isDrawAlarmValue = in.readBoolean();
		}
	}

}
