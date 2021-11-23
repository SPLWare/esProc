package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * 文本标签的封装类
 * 绘制图形时，文本值需要在最后统一绘制以防止被覆盖
 * @author Joancy
 *
 */
public class ValueLabel {
  public String text;
  public Point2D.Double p;
  public Color c;
  public byte direction=GraphFontView.TEXT_ON_TOP;

  /**
   * 构造一个文本标签对象
   * @param text 文本值
   * @param p 坐标
   * @param c 颜色
   */
  public ValueLabel(String text, Point2D.Double p, Color c) {
	this.text = text;
	this.p = p;
	this.c = c;
  }

  /**
   * 构造一个文本标签对象
   * @param text 文本值
   * @param p 坐标
   * @param c 颜色
   * @param textDirection 方位
   */
  public ValueLabel(String text, Point2D.Double p, Color c,byte textDirection) {
	this.text = text;
	this.p = p;
	this.c = c;
	this.direction = textDirection;
  }
}
