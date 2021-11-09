package com.raqsoft.chart;

import java.awt.geom.Point2D;
import java.util.*;
/**
 * 坐标轴接口
 * @author Administrator
 *
 */
public interface IAxis{
  public Point2D getBasePoint(ICoor coor);//柱图时的起始点
  
  public String getName();

  public int getLocation();

  
  //绘图前准备工作，参数环境的初始化，在重复repaint时，不再调用该方法
  public void prepare(ArrayList<DataElement> dataElements);
}
