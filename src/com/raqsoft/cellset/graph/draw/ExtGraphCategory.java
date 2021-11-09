package com.raqsoft.cellset.graph.draw;

import java.util.*;
import com.raqsoft.util.*;

/**
 * 图形分类定义扩展属性
 * 
 * @author Joancy
 *
 */
public class ExtGraphCategory implements Comparable{
  /** 分类表达式 或者 时序状态图分类表达式或甘特图和里程碑图的项目*/
  private Object name;
  private String nameFmt = null;//显示值所用的格式串

  /**此分类下的系列 (ExtGraphSery或者 ExtGraphTimeStatus)*/
  private ArrayList series;

  /**
   * 获得名称
   * @return Object 分类名称
   */
  public Object getName() {
	return name;
  }

  /**
   * 取分类名称的字符串表示法
   * @return 字符串名称
   */
  public String getNameString(){
	return Variant.toString(name);
  }

  /**
   * 汇总当前分类下的系列数值
   * @return 汇总值
   */
  public double getSumSeries() {
	double d = 0;
	for (int i = 0; i < series.size(); i++) {
	  ExtGraphSery egs = (ExtGraphSery) series.get(i);
	  d += egs.getValue();
	}
	return d;
  }
  /**
   * 将该分类中所有大于0的数值加起来
   * 堆积图会用到
   * @return double 正数汇总值
   */
  public double getPositiveSumSeries() {
	double d = 0;
	for (int i = 0; i < series.size(); i++) {
	  ExtGraphSery egs = (ExtGraphSery) series.get(i);
	  double v = egs.getValue();
	  if( v<=0 ){
	continue;
	  }
	  d += egs.getValue();
	}
	return d;
  }

  /**
   * 统计系列值为负的汇总值
   * @return 负数汇总值
   */
  public double getNegativeSumSeries() {
	double d = 0;
	for (int i = 0; i < series.size(); i++) {
	  ExtGraphSery egs = (ExtGraphSery) series.get(i);
	  double v = egs.getValue();
	  if( v>=0 ){
	continue;
	  }
	  d += egs.getValue();
	}
	return d;
  }

  /**
   * 取系列对象
   * @param seriesName 系列名称
   * @return 系列对象
   */
  public ExtGraphSery getExtGraphSery( Object seriesName ){
	ExtGraphSery egs;
	for( int i=0; i<series.size(); i++ ){
	  egs = (ExtGraphSery)series.get(i);
	  String name = egs.getName();
	  if(( name!=null && name.equals(seriesName)) || (name==null && seriesName==null)){
	return egs;
	  }
	}
	egs = new ExtGraphSery();
	egs.setName(Variant.toString(seriesName) );
	return egs;
  }
  /**
   * 获得本分类下的系列
   * @return ArrayList (ExtGraphSery或者 ExtGraphTimeStatus) 本分类下的系列
   */
  public ArrayList getSeries() {
	return series;
  }

  /**
   * 设置名称
   * @param name 分类名称
   */
  public void setName(Object name) {
	this.name = name;
  }

  /**
   * 获得本分类下的系列
   * @param series (ExtGraphSery或者 ExtGraphTimeStatus) 本分类下的系列
   */
  public void setSeries(ArrayList series) {
	this.series = series;
  }

  /**
   * 实现比较函数
   * 根据系列的汇总值进行比较
   * @param o Object 分类对象 
   * @return int 比较结果
   */
  public int compareTo(Object o) {
	ExtGraphCategory otherEgc = (ExtGraphCategory)o;
	Double self = new Double(this.getSumSeries());
	Double other = new Double( otherEgc.getSumSeries() );
	return self.compareTo( other );
  }

}
