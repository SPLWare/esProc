package com.raqsoft.chart;

import java.awt.*;
import java.util.ArrayList;

import com.raqsoft.chart.edit.*;

/**
 * 图元接口，图元中的参数只能有两种类型，
 * 1：基本类型(int,float,double,boolean,String)
 * 2：参数类型(Param)
 * 当属性为参数类型Param时，为方便程序书写，不允许定义null初始值，而应定义成 new Param(null)
 */
public interface IElement {
  /**
   * 列出图元的参数信息列表，提供给编辑器使用
   * @return ArrayList，每个成员均为ParamInfo对象
   */
  public ParamInfoList getParamInfoList();

  /**
   * 图形元素是否可见
   * @return 是返回true，否则返回false
   */
  public boolean isVisible();
  
  /**
   * 图元的重画前数值初始化，由于初始化值在图元绘制中有先后引用，所以要先初始化，再开始画图
   * 绘图引擎会依次调用每个绘图元素的beforeDraw, drawBack,draw, drawFore
   */
  public void beforeDraw();

  /**
   * 绘制背景层,注意在背景层应绘制所有的填充色，阴影等，才不会覆盖后续的层。该层不能绘线及文字
   */
  public void drawBack();

  /**
   * 绘制中间层，中间层绘制图形的填充色
   */
  public void draw(); 

  /**
   *  绘制前景层，前景层绘制图形的边线，以及文字等
   */
  public void drawFore();

  /**
   *  取图元的边界形状，用于响应鼠标事件
   * @return 边界形状列表
   */
  public ArrayList<Shape> getShapes();
  
  /**
   *  取图元的边界形状的相应超链接
   * @return 超链接列表
   */
  public ArrayList<String> getLinks();

  /**
   * 设置绘图引擎，绘图引擎相当于大管家，绘图元素可以通过引擎获取到其他元素的相应信息
   * @param e 绘图引擎
   */
  public void setEngine( Engine e );
  
/**
 * 获取绘图引擎
 * @return 绘图引擎
 */
  public Engine getEngine();

}
