package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;

import com.scudata.chart.Consts;

/**
 * 封装了形状，颜色等属性的数据点
 * 类似于文本标签，数据点由于一般比较小，也需要在后面统一绘制
 * 防止被覆盖
 * @author Joancy
 *
 */
public class ValuePoint {
	public Point2D.Double p;
	public Color borderColor,fillColor=null;
	public int shape = Consts.PT_CIRCLE;
	public int radius = -1;
	/**
	 * 构造一个数据点
	 * @param p 坐标
	 * @param bc 边框颜色
	 */
	public ValuePoint(Point2D.Double p, Color bc) {
		this.p = p;
		this.borderColor = bc;
		this.fillColor = bc;
	}

	/**
	 * 构造一个数据点
	 * @param p 坐标
	 * @param bc 边框颜色
	 * @param fillColor 填充颜色
	 * @param shape 点的形状
	 * @param radius 点的半径
	 */
	public ValuePoint(Point2D.Double p, Color bc,Color fillColor, int shape, int radius) {
		this(p, bc);
		this.shape = shape;
		this.radius = radius;
	}
}
