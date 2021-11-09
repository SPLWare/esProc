package com.raqsoft.chart;

import java.awt.geom.*;

import com.raqsoft.chart.element.*;

/**
 * 极坐标系
 * @author Joancy
 *
 */
public class PolarCoor implements ICoor {
	TickAxis a1, a2;

	/**
	 * 设置刻度轴1
	 */
	public void setAxis1(TickAxis axis) {
		this.a1 = axis;
	}

	/**
	 * 获取刻度轴1
	 */
	public TickAxis getAxis1() {
		return a1;
	}

	/**
	 * 获取极轴
	 * @return 极轴对象
	 */
	public TickAxis getPolarAxis() {
		if (a1.getLocation() == Consts.AXIS_LOC_POLAR) {
			return a1;
		}
		return a2;
	}

	/**
	 * 设置刻度轴2
	 */
	public void setAxis2(TickAxis axis) {
		this.a2 = axis;
	}

	/**
	 * 获取刻度轴2
	 */
	public TickAxis getAxis2() {
		return a2;
	}

	/**
	 * 获取角轴
	 * @return 角轴对象
	 */
	public TickAxis getAngleAxis() {
		if (a1.getLocation() == Consts.AXIS_LOC_ANGLE) {
			return a1;
		}
		return a2;
	}

	/**
	 * 返回极坐标系下的数值点；其中 P.x = 极轴长度， P.y=角轴角度
	 * @param val1 逻辑坐标1
	 * @param val2 逻辑坐标2
	 * @return Point 对应极坐标
	 */
	public Point2D getPolarPoint(Object val1, Object val2) {
		double i1 = a1.getValueLen(val1);
		double i2 = a2.getValueLen(val2);
		double r,a;
		if (getPolarAxis() == a1) {
			r = i1;
			a = i2;
		}else{
			r = i2;
			a = i1;
		}
		return new Point2D.Double(r, a);
	}

	/**
	 * 将极坐标点转换为图形屏幕像素位置
	 * @param polarPoint 极坐标点
	 * @return 屏幕像素绝对坐标
	 */
	public Point2D getScreenPoint(Point2D polarPoint) {
		TickAxis polarAxis = (TickAxis) getPolarAxis();
		TickAxis angleAxis = (TickAxis) getAngleAxis();
		double r,a;
		r = polarPoint.getX();
		a = polarPoint.getY()+angleAxis.getBottomY();
		double radAngle = a * Math.PI / 180;
		double x = polarAxis.getLeftX() + r * Math.cos(radAngle);
		double y = polarAxis.getBottomY() - r * Math.sin(radAngle);
		return new Point2D.Double(x, y);
	}

	/**
	 * 直接根据逻辑坐标值，计算出屏幕像素绝对坐标
	 * @param 逻辑坐标1
	 * @param 逻辑坐标2
	 * @return 屏幕像素绝对坐标
	 */
	public Point2D getScreenPoint(Object val1, Object val2) {
		Point2D pDot = getPolarPoint(val1, val2);
		return getScreenPoint(pDot);
	}

	/**
	 * 获取相对原点极轴长为polarLen的椭圆边界
	 * @param polarLen 极轴长度
	 * @return Double 边界对象
	 */
	public Rectangle2D getEllipseBounds(double polarLen) {
		TickAxis polarAxis = (TickAxis) getPolarAxis();
		double x = polarAxis.getLeftX() - polarLen;
		double y = polarAxis.getBottomY() - polarLen;
		double w = polarLen * 2;
		double h = w;
		Rectangle2D ellipseBounds = new Rectangle2D.Double(x, y, w, h);
		return ellipseBounds;
	}

	/**
	 * 判断两个坐标系是否相等
	 * @param 另一个坐标系
	 * @return 相等返回true， 否则返回false
	 */
	public boolean equals(Object another) {
		if (another instanceof PolarCoor) {
			PolarCoor apc = (PolarCoor) another;
			return apc.getPolarAxis() == getPolarAxis()
					&& apc.getAngleAxis() == getAngleAxis();
		}
		return false;
	}

	/**
	 * 用法同CartesianCoor
	 */
	public NumericAxis getNumericAxis() {
		TickAxis axis = CartesianCoor.getAxis(this, NumericAxis.class);
		if (axis != null)
			return (NumericAxis) axis;
		return null;
	}

	/**
	 * 用法同CartesianCoor
	 */
	public EnumAxis getEnumAxis() {
		TickAxis axis = CartesianCoor.getAxis(this, EnumAxis.class);
		if (axis != null)
			return (EnumAxis) axis;
		return null;
	}

	/**
	 * 坐标系的描述信息
	 */
	public String toString() {
		return "PolarCoor Axis1:" + a1.getName() + " Axis2:" + a2.getName();
	}

	/**
	 * 是否为极坐标系
	 * @return true
	 */
	public boolean isPolarCoor() {
		return true;
	}

	/**
	 * 是否为直角坐标系
	 * @return false
	 */
	public boolean isCartesianCoor() {
		return false;
	}

	/**
	 * 垂向的1轴是枚举轴，就算是基于枚举轴
	 */
	public boolean isEnumBased() {
			TickAxis ta = getAxis1();
			return ta.isEnumAxis();
	}
}
