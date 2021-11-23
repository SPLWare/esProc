package com.scudata.cellset.graph.draw;

import java.awt.Color;

import com.scudata.cellset.graph.*;

/**
 * 统计图中的警戒线属性类
 */
public class ExtAlarmLine {
	/**　警戒线名称　*/
	private String name = null;
	/**　警戒值　*/
	private double value = 0;
	/**　警戒线类型　*/
	private byte lineType = GraphProperty.LINE_SOLID;
	/**  警戒线颜色 */
	private int color = Color.red.getRGB();
	/* 直线的粗度 */
	private float lineThick;
	private boolean isDrawAlarmValue = true;

	/**
	 * 取名称
	 * @return String　名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 取警戒值
	 * @return String　警戒值
	 */
	public double getAlarmValue() {
		return value;
	}

	/**
	 * 取警戒线类型
	 * @return byte　警戒线类型，值为GraphProperty.LINE_NONE, LINE_SOLID, LINE_LONG_DASH, LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public byte getLineType() {
		return lineType;
	}

	/**
	 * 取颜色
	 * @return int　颜色
	 */
	public int getColor() {
		return color;
	}

	/**
	 * 设置名称
	 * @param name　名称
	 */
	public void setName(String name) {
		this.name= name;
	}

	/**
	 * 设置警戒值
	 * @param value　警戒值
	 */
	public void setAlarmValue(double value) {
		this.value= value;
	}

	/**
	 * 设置警戒线类型
	 * @param type　警戒线类型，值为GraphProperty.LINE_NONE, LINE_SOLID, LINE_LONG_DASH, LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public void setLineType(byte type) {
		lineType=type;
	}

	/**
	 * 设置颜色
	 * @param color　颜色
	 */
	public void setColor(int color) {
		this.color= color;
	}

	/**
	 * 设置粗度
	 * @param thick 粗度
	 */
	public void setLineThick( float thick ){
	  this.lineThick = thick;
	}
	/**
	 * 取线粗度
	 * @return 粗度
	 */
	public float getLineThick(){
	  return lineThick;
	}

	/**
	 * 设置是否绘制警戒值
	 * @param isDrawAlarmValue 是否绘制警戒值
	 */
	public void setDrawAlarmValue( boolean isDrawAlarmValue ){
		  this.isDrawAlarmValue = isDrawAlarmValue;
	}
	/**
	 * 取是否绘制警戒值
	 * @return 绘制返回true，否则false
	 */
	public boolean isDrawAlarmValue(){
	  return isDrawAlarmValue;
	}

}
