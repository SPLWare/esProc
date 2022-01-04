package com.scudata.cellset.graph;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.*;

import com.scudata.common.*;

/**
 * 统计图或者图层等图片数据的相关属性
 * @author Joancy
 *
 */
public class ImageValue implements ICloneable, Externalizable, IRecord {
	private static final long serialVersionUID = 1L;

	private byte imageType;
	private String html;
	private byte[] value;//不管是何格式，都要产生图形流，用于导出或者打印到图形设备
	private String customHtml;

	private byte[] flashXml;
	
	private transient IRedraw redraw = null;
	
	public void setIRedraw(IRedraw ir){
		redraw = ir;
	}
	
	public boolean canRedraw(){
		return redraw!=null;
	}
	
	public void repaint(Graphics g, int w, int h){
		redraw.repaint((Graphics2D)g, w, h);
	}
	/**
	 * 缺省构造函数
	 */
	public ImageValue() {
	}

	/**
	 * 指定参数的构造函数
	 * @param value 图像内容的字节数组
	 * @param type 图形格式
	 * @param html 图形对应的超链接
	 */
	public ImageValue( byte[] value, byte type, String html ) {
		this.value = value;
		this.imageType = type;
		this.html = html;
	}

	/**
	 * 指定参数的构造函数
	 * @param value 图像内容的字节数组
	 * @param type 图形格式
	 * @param html 图形对应的超链接
	 * @param flashXml flash格式的图像内容数组
	 * 由于flash图像没法在java中直接绘制，所以flash格式时会保留两份副本
	 * 一份为普通图像内容value，用于界面绘制
	 * 一份为flash自身内容flashXml，用于输出
	 */
	public ImageValue( byte[] value, byte type, String html, byte[] flashXml ) {
		this.value = value;
		this.imageType = type;
		this.html = html;
		this.flashXml = flashXml;
	}

	/**
	 * 设置字节内容的图像数据
	 * @param value 图像数据
	 */
	public void setValue( byte[] value ) {
		this.value = value;
	}

	/**
	 * 设置图片的格式
	 * @param type 图片格式 值为：IGraphProperty.IMAGE_XXX
	 */
	public void setImageType( byte type ) {
		this.imageType = type;
	}

	/**
	 * 统计图时，如果有超链接，将超链接设置进来
	 * @param html 超链接文本
	 */
	public void setHtml( String html ) {
		this.html = html;
	}

	/**
	 * 取图片内容数据
	 * @return 图片内容
	 */
	public byte[] getValue() {
		return this.value;
	}

	/**
	 * 取图片格式
	 * @return 图片格式
	 */
	public byte getImageType() {
		return this.imageType;
	}

	/**
	 * 取超链接文本
	 * @return 超链接内容
	 */
	public String getHtml() {
		return this.html;
	}

	/**
	 * 也可以自定义超链接，设置自定义内容
	 * @param s 超链接内容
	 */
	public void setCustomHtml( String s ) {
		customHtml = s;
	}

	/**
	 * 取自定义超链接内容
	 * @return 自定义超链接
	 */
	public String getCustomHtml() {
		return customHtml;
	}

	/**
	 * 如果图片生成flash，设置flash内容
	 * @param bytes flash内容的字节数据
	 */
	public void setFlashXml( byte[] bytes ) {
		this.flashXml = bytes;
	}

	/**
	 * 取flash内容的字节数据
	 * @return 字节数据
	 */
	public byte[] getFlashXml() {
		return this.flashXml;
	}

	/**
	 * svg流跟flash不会同时存在，实现两个不同的设置方法
	 * 但存储内容是共用的
	 * @param bytes byte[] svg格式的内容数据
	 */
	public void setSvgBytes( byte[] bytes ) {
		this.flashXml = bytes;
	}

	/**
	 * 取svg格式的内容数据
	 * @return 字节数据
	 */
	public byte[] getSvgBytes() {
		return this.flashXml;
	}

	/**
	 * 实现序列化接口
	 */
	public void writeExternal( ObjectOutput out ) throws IOException {
		out.writeByte( 3 ); //Macro的版本
		out.write( imageType );
		out.writeObject( value );
		out.writeObject( html );
		out.writeObject( flashXml );
		out.writeObject( customHtml );
	}

	/**
	 * 实现序列化接口
	 */
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		byte version = in.readByte();
		imageType = in.readByte();
		value = ( byte[] ) in.readObject();
		html = ( String ) in.readObject();
		if ( version > 1 ) {
			flashXml = ( byte[] ) in.readObject();
		}
		else if ( imageType == GraphProperty.IMAGE_FLASH ) {
			flashXml = value;
		}
		if( version > 2 ) customHtml = ( String ) in.readObject();
	}

	/**
	 * 实现IRecord接口
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte( imageType );
		out.writeBytes( value );
		out.writeString( html );
		out.writeBytes( flashXml );
		out.writeString( customHtml );
		return out.toByteArray();
	}

	/**
	 * 实现IRecord接口
	 */
	public void fillRecord( byte[] buf ) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord( buf );
		imageType = in.readByte();
		value = in.readBytes();
		html = in.readString();
		if ( in.available() > 0 ) {
			flashXml = in.readBytes();
		}
		else if ( imageType == GraphProperty.IMAGE_FLASH ) {
			flashXml = value;
		}
		if( in.available() > 0 ) {
			customHtml = in.readString();
		}
	}

	/**
	 * 深度克隆一个图像数据对象
	 * @return 克隆后的对象
	 */
	public Object deepClone() {
		ImageValue v = new ImageValue();
		v.imageType = this.imageType;
		if ( this.value != null ) {
			v.value = ( byte[] )this.value.clone();
		}
		if ( this.html != null ) {
			v.html = new String( this.html );
		}
		if ( this.flashXml != null ) {
			v.flashXml = this.flashXml;
		}
		if ( this.customHtml != null ) {
			v.setCustomHtml( this.customHtml );
		}
		return v;
	}

}
