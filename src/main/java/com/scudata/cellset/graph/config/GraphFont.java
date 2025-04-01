package com.scudata.cellset.graph.config;

import java.io.*;

import com.scudata.common.*;

import java.awt.*;

/**
 *  统计图中的字体类定义
 * @author Joancy
 *
 */
public class GraphFont implements ICloneable, Externalizable, IRecord {
	private final static long serialVersionUID = 82857881736578L;
	private byte version = ( byte ) 1;

	/** 字体名称 */
	private String family;
	/** 字体大小 */
	private int size = 12;
	/** 是否自动调整大小 */
	private boolean autoResize = false;
	/** 是否粗体 */
	private boolean bold = false;
	/** 字体颜色 */
	private int color = Color.black.getRGB();
	/** 是否竖排文字 */
	private boolean verticalText = false;
	/** 旋转角度 */
	private int angle;

	/**
	 * 设置字体名称
	 * @param family 字体名称
	 */
	public void setFamily( String family ) {
		this.family = family;
	}

	/**
	 * 设置字体大小
	 * @param size 字体大小
	 */
	public void setSize( int size ) {
		this.size = size;
	}

	/**
	 * 设置是否自动调整字体大小
	 * @param b 是否自动调整
	 */
	public void setAutoResize( boolean b ) {
		this.autoResize = b;
	}

	/**
	 * 设置是否粗体
	 * @param b 是否粗体
	 */
	public void setBold( boolean b ) {
		this.bold = b;
	}

	/**
	 * 设置字体颜色
	 * @param c 颜色值
	 */
	public void setColor( int c ) {
		this.color = c;
	}

	/**
	 * 设置是否竖排文字
	 * @param b 是否竖排文字
	 */
	public void setVerticalText( boolean b ) {
		this.verticalText = b;
	}

	/**
	 * 设置旋转角度
	 * @param angle 角度值
	 */
	public void setAngle( int angle ) {
		this.angle = angle;
	}

	/**
	 * 取字体名称
	 * @return String　字体名称
	 */
	public String getFamily() {
		return family;
	}

	/**
	 * 取字体大小
	 * @return int　字体大小
	 */
	public int getSize() {
		return size;
	}

	/**
	 * 是否自动调整大小
	 * @return boolean 自动调整时返回true，否则返回false
	 */
	public boolean isAutoResize() {
		return autoResize;
	}

	/**
	 * 是否粗体
	 * @return boolean 粗体返回true，否则返回false
	 */
	public boolean isBold() {
		return bold;
	}

	/**
	 * 取字体颜色
	 * @return int　字体颜色
	 */
	public int getColor() {
		return color;
	}

	/**
	 * 是否竖排文字
	 * @return boolean 竖排文字返回true，否则返回false
	 */
	public boolean isVerticalText() {
		return verticalText;
	}

	/**
	 * 取旋转角度
	 * @return int　旋转角度
	 */
	public int getAngle() {
		return angle;
	}

	/**
	 * 深度克隆
	 * @return Object 克隆后的字体对象
	 */
	public Object deepClone() {
		GraphFont font = new GraphFont();
		font.setFamily( family );
		font.setSize( size );
		font.setColor( color );
		font.setBold( bold );
		font.setAngle( angle );
		font.setAutoResize( autoResize );
		font.setVerticalText( verticalText );
		return font;
	}

	/**
	 * 实现序列化接口
	 */
	public void writeExternal( ObjectOutput out ) throws IOException{
		out.writeByte( version );
		out.writeObject( family );
		out.writeInt( size );
		out.writeInt( color );
		out.writeBoolean( bold );
		out.writeInt( angle );
		out.writeBoolean( autoResize );
		out.writeBoolean( verticalText );
	}

	/**
	 * 实现序列化接口
	 */
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException{
		byte ver = in.readByte();
		family = ( String ) in.readObject();
		size = in.readInt();
		color = in.readInt();
		bold = in.readBoolean();
		angle = in.readInt();
		autoResize = in.readBoolean();
		verticalText = in.readBoolean();
	}

	/**
	 * 实现IRecord接口
	 */
	public byte[] serialize() throws IOException{
	  ByteArrayOutputRecord out = new ByteArrayOutputRecord();
	  out.writeString( family );
	  out.writeInt( size );
	  out.writeInt( color );
	  out.writeBoolean( bold );
	  out.writeInt( angle );
	  out.writeBoolean( autoResize );
	  out.writeBoolean( verticalText );
	  return out.toByteArray();
	}

	/**
	 * 实现IRecord接口
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
	  ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
	  family = in.readString();
	  size = in.readInt();
	  color = in.readInt();
	  bold = in.readBoolean();
	  angle = in.readInt();
	  autoResize = in.readBoolean();
	  verticalText = in.readBoolean();
	}

}
