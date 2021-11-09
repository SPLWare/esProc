package com.raqsoft.cellset.graph.config;

import java.io.*;
import com.raqsoft.common.*;

/**
 * 封装统计图用到的所有类型字体
 * 
 * @author Joancy
 *
 */
public class GraphFonts implements ICloneable, Externalizable, IRecord {
	private final static long serialVersionUID = 82857881736578L;
	private byte version = ( byte ) 1;

	/** 统计图标题字体 */
	private GraphFont titleFont;
	/** 统计图横轴标题字体 */
	private GraphFont xTitleFont;
	/** 统计图纵轴标题字体 */
	private GraphFont yTitleFont;
	/** 统计图横轴标签字体 */
	private GraphFont xLabelFont;
	/** 统计图纵轴标签字体 */
	private GraphFont yLabelFont;
	/** 统计图图例字体 */
	private GraphFont legendFont;
	/** 统计图图中显示数据字体 */
	private GraphFont dataFont;

	/**
	 * 缺省值构造函数
	 */
	public GraphFonts() {
		titleFont = new GraphFont();
		xTitleFont = new GraphFont();
		yTitleFont = new GraphFont();
		xLabelFont = new GraphFont();
		yLabelFont = new GraphFont();
		legendFont = new GraphFont();
		dataFont = new GraphFont();
	}

	/**
	 * 取标题字体
	 * @return GraphFont　标题字体
	 */
	public GraphFont getTitleFont() {
		return titleFont;
	}

	/**
	 * 设置标题字体
	 * @param font 标题字体
	 */
	public void setTitleFont( GraphFont font ) {
		this.titleFont = font;
	}

	/**
	 * 取横轴标题字体
	 * @return GraphFont　横轴标题字体
	 */
	public GraphFont getXTitleFont() {
		return xTitleFont;
	}

	/**
	 * 设置横轴标题字体
	 * @param font 横轴标题字体
	 */
	public void setXTitleFont( GraphFont font ) {
		this.xTitleFont = font;
	}

	/**
	 * 取纵轴标题字体
	 * @return GraphFont　纵轴标题字体
	 */
	public GraphFont getYTitleFont() {
		return yTitleFont;
	}

	/**
	 * 设置纵轴标题字体
	 * @param font 纵轴标题字体
	 */
	public void setYTitleFont( GraphFont font ) {
		this.yTitleFont = font;
	}

	/**
	 * 取横轴标签字体
	 * @return GraphFont　横轴标签字体
	 */
	public GraphFont getXLabelFont() {
		return xLabelFont;
	}

	/**
	 * 设置横轴标签字体
	 * @param font 横轴标签字体
	 */
	public void setXLabelFont( GraphFont font ) {
		this.xLabelFont = font;
	}

	/**
	 * 取纵轴标签字体
	 * @return GraphFont　纵轴标签字体
	 */
	public GraphFont getYLabelFont() {
		return yLabelFont;
	}

	/**
	 * 设置纵轴标签字体
	 * @param font 纵轴标签字体
	 */
	public void setYLabelFont( GraphFont font ) {
		this.yLabelFont = font;
	}

	/**
	 * 取图例字体
	 * @return GraphFont　图例字体
	 */
	public GraphFont getLegendFont() {
		return legendFont;
	}

	/**
	 * 设置图例字体
	 * @param font 图例字体
	 */
	public void setLegendFont( GraphFont font ) {
		this.legendFont = font;
	}

	/**
	 * 取图中显示数据字体
	 * @return GraphFont　图中显示数据字体
	 */
	public GraphFont getDataFont() {
		return dataFont;
	}

	/**
	 * 设置图中显示数据字体
	 * @param font 图中显示数据字体
	 */
	public void setDataFont( GraphFont font ) {
		this.dataFont = font;
	}

	/**
	 * 深度克隆
	 * @return Object 克隆后的字体集合
	 */
	public Object deepClone() {
		GraphFonts fonts = new GraphFonts();
		fonts.setTitleFont( ( GraphFont ) titleFont.deepClone() );
		fonts.setXTitleFont( ( GraphFont ) xTitleFont.deepClone() );
		fonts.setYTitleFont( ( GraphFont ) yTitleFont.deepClone() );
		fonts.setXLabelFont( ( GraphFont ) xLabelFont.deepClone() );
		fonts.setYLabelFont( ( GraphFont ) yLabelFont.deepClone() );
		fonts.setLegendFont( ( GraphFont ) legendFont.deepClone() );
		fonts.setDataFont( ( GraphFont ) dataFont.deepClone() );
		return fonts;
	}

	/**
	 * 实现序列化接口
	 */
	public void writeExternal( ObjectOutput out ) throws IOException{
		out.writeByte( version );
		out.writeObject( titleFont );
		out.writeObject( xTitleFont );
		out.writeObject( yTitleFont );
		out.writeObject( xLabelFont );
		out.writeObject( yLabelFont );
		out.writeObject( legendFont );
		out.writeObject( dataFont );
	}

	/**
	 * 实现序列化接口
	 */
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException{
		byte ver = in.readByte();
		titleFont = ( GraphFont ) in.readObject();
		xTitleFont = ( GraphFont ) in.readObject();
		yTitleFont = ( GraphFont ) in.readObject();
		xLabelFont = ( GraphFont ) in.readObject();
		yLabelFont = ( GraphFont ) in.readObject();
		legendFont = ( GraphFont ) in.readObject();
		dataFont = ( GraphFont ) in.readObject();
	}

	/**
	 * 实现IRecord接口
	 */
	public byte[] serialize() throws IOException{
	  ByteArrayOutputRecord out = new ByteArrayOutputRecord();
	  out.writeRecord( titleFont );
	  out.writeRecord( xTitleFont );
	  out.writeRecord( yTitleFont );
	  out.writeRecord( xLabelFont );
	  out.writeRecord( yLabelFont );
	  out.writeRecord( legendFont );
	  out.writeRecord( dataFont );
	  return out.toByteArray();
	}

	/**
	 * 实现IRecord接口
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
	  ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
	  titleFont = (GraphFont) in.readRecord(new GraphFont());
	  xTitleFont = (GraphFont) in.readRecord(new GraphFont());
	  yTitleFont = (GraphFont) in.readRecord(new GraphFont());
	  xLabelFont = (GraphFont) in.readRecord(new GraphFont());
	  yLabelFont = (GraphFont) in.readRecord(new GraphFont());
	  legendFont = (GraphFont) in.readRecord(new GraphFont());
	  dataFont = (GraphFont) in.readRecord(new GraphFont());
	}

}
