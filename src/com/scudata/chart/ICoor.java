package com.scudata.chart;

import java.awt.geom.Point2D;

import com.scudata.chart.element.*;

/**
 * 坐标系接口
 * @author Joancy
 *
 */
public interface ICoor {
	/**
	 * 设置构成坐标系的刻度轴1
	 * 
	 * @param a 刻度轴
	 */
  public void setAxis1(TickAxis a);

	/**
	 * 设置构成坐标系的刻度轴2
	 * 
	 * @param a 刻度轴
	 */
  public void setAxis2(TickAxis a);

/**
 * 获取刻度轴1
 * @return 刻度轴
 */
  public TickAxis getAxis1();

  /**
   * 获取刻度轴2
   * @return 刻度轴
   */
  public TickAxis getAxis2();

  /**
   * 获取逻辑值val1，val2在该坐标系下的绝对像素坐标
   * @param val1 Object 对应轴1的逻辑坐标
   * @param val2 Object 对应轴2的逻辑坐标
   * @return Point 实数精度的像素坐标
   */
  public Point2D getScreenPoint(Object val1, Object val2);

  /**
   * 获取数值轴，如果有的话，一般用于数值轴跟枚举轴组合的坐标系时
   * @return 数值轴
   */
  public NumericAxis getNumericAxis();
  
  /**
   * 获取坐标系里面的枚举轴
   * @return 枚举轴
   */
  public EnumAxis getEnumAxis();
  
  /**
   * 判断当前坐标系是否为极坐标系
   * @return 是返回true，否则返回false
   */
  public boolean isPolarCoor();
  
  /**
   * 判断当前坐标系是否为直角坐标系
   * @return 是返回true，否则返回false
   */
  public boolean isCartesianCoor();
  
	/**
	 * 根据图元的设置判断当前坐标系是否垂向于一条枚举轴
	 * 只有垂向于枚举轴的数值才能堆积
	 * @return 是返回true，否则返回false
	 */
  public boolean isEnumBased();
}
