package com.scudata.cellset.graph.draw;

import com.scudata.chart.Consts;

/**
 * 扩展图形系列属性定义
 * @author Joancy
 *
 */
public class ExtGraphSery implements Comparable{
  /** 系列名称  */
  private String name;

  /** 系列表达式  */
  private Number value = null;

  /** 系列Tip  */
  private String tips = null;
  
  private byte axis = Consts.AXIS_LEFT;//Left
  /**
   * 设置系列名称
   * param  name 系列名称
   */
  public void setName(String name) {
	this.name = name;
  }

  /**
   * 设置系列值
   * @param  value 系列值
   */
  public void setValue(Number value) {
	this.value = value;
  }

  /**
   * 获得系列名称
   * @return  String 系列名称
   */
  public String getName() {
	return name;
  }

  /**
   * 获得系列值
   * @return  Object 系列值
   */
  public double getValue() {
	if (value == null) {
	  return 0;
	}
	//如果類型不一致，比如float转成double，直接转换会造成精度不够，会发生存储值误差，所以此处，用字符串过渡一下
	if(value instanceof Float){
		double d = Double.parseDouble(""+value);
		return d;
	}
	return value.doubleValue();
  }

  /**
   * 获取系列值
   * @return 值
   */
  public Number getValueObject() {
	return value;
  }

  /**
   * 判断是否为空值
   * @return 空值返回true，否则返回false
   */
  public boolean isNull() {
	return value == null;
  }

  /**
   * 取生成超链接的tip
   * @return tip串
   */
  public String getTips(){
	return tips;
  }
  
  /**
   * 设置生成超链接的tip串
   * @param tip 串值
   */
  public void setTips( String tip ){
	tips = tip;
  }
  
  /**
   * 取当前系列对应的轴，双轴图时将系列画到对应的轴，分为左右轴
   * @return
   */
  public byte getAxis(){
	return axis;
  }
  public void setAxis( byte axis ){
	this.axis = axis;
  }
  /**
   * 实现比较接口
   */
  public int compareTo(Object o) {
	ExtGraphSery other = (ExtGraphSery)o;
	return new Double(getValue()).compareTo( new Double(other.getValue()));
  }
}
