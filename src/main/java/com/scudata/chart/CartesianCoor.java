package com.scudata.chart;

import java.awt.geom.Point2D;

import com.scudata.chart.element.*;

/**
 * 构造一个用于绘图的逻辑直角坐标系
 * 
 * @author Joancy
 *
 */
public class CartesianCoor implements ICoor {
	// 构成坐标系的两根刻度轴，由于轴本身定义横轴还是纵轴，所以坐标系中不命名横轴纵轴，只要组合的两个轴分别有横轴纵轴即可
	TickAxis a1, a2;

	/**
	 * 设置第一根刻度轴
	 * @param axis	刻度轴
	 */
	public void setAxis1(TickAxis axis) {
		this.a1 = axis;
	}

	/**
	 * 获取第一根刻度轴
	 * @return 刻度轴1
	 */
	public TickAxis getAxis1() {
		return a1;
	}

	/**
	 * 获取x轴，也即横轴
	 * @return 坐标系的x轴
	 */
	public TickAxis getXAxis() {
		if (a1.getLocation() == Consts.AXIS_LOC_H) {
			return a1;
		}
		return a2;
	}

	/**
	 * 设置第二根刻度轴
	 * @param axis	刻度轴
	 */
	public void setAxis2(TickAxis axis) {
		this.a2 = axis;
	}

	/**
	 * 获取第二根刻度轴
	 * @return 刻度轴2
	 */
	public TickAxis getAxis2() {
		return a2;
	}

	/**
	 * 获取y轴，也即纵轴
	 * @return 坐标系的y轴
	 */
	public TickAxis getYAxis() {
		if (a1.getLocation() == Consts.AXIS_LOC_V) {
			return a1;
		}
		return a2;
	}

	/**
	 * 计算相对应两根轴的逻辑值的物理坐标
	 * @param val1	对应1轴的逻辑值
	 * @param val2	对应2轴的逻辑值
	 * @return Point2D 精度为double的物理坐标
	 */
	public Point2D getScreenPoint(Object val1, Object val2) {
		double i1 = a1.getValueLen(val1);
		double i2 = a2.getValueLen(val2);
		double x, y;
		if (getXAxis() == a1) {
			x = a1.getLeftX() + i1;
			y = a2.getBottomY() - i2;
		} else {
			x = a2.getLeftX() + i2;
			y = a1.getBottomY() - i1;
		}
		return new Point2D.Double(x, y);
	}

	/**
	 * 3d偏移坐标量,如果有枚举轴，则取枚举轴的thickRate，否则取axis1的该值；
	 * 
	 * @return int 
	 */
	public int get3DShift() {
		boolean is3D = ((TickAxis) a1).is3D || ((TickAxis) a2).is3D;
		if (!is3D)
			return 0;
		int maxShift = 60;
		int shift;
		EnumAxis ea = getEnumAxis();
		if (ea == null) {
			shift = (int) ((TickAxis) a1).getEngine().getYPixel(
					((TickAxis) a1).threeDThickRatio);
		} else {
			shift = (int) (ea.getSeriesWidth() * ea.threeDThickRatio);
		}
		return Math.min(shift, maxShift);
	}

	protected static TickAxis getAxis(ICoor coor, Class axisType) {
		TickAxis axis = coor.getAxis1();
		if (axisType.isInstance(axis)) {
			return axis;
		}
		axis = coor.getAxis2();
		if (axisType.isInstance(axis)) {
			return axis;
		}
		return null;
	}

	/**
	 * 获取坐标系中的数值轴，该方法通常用于枚举轴跟数值轴组合的坐标系时，如果两根轴都是数值轴，则只返回1轴。
	 * 
	 * @return 数值轴
	 */
	public NumericAxis getNumericAxis() {
		TickAxis axis = getAxis(this, NumericAxis.class);
		if (axis != null)
			return (NumericAxis) axis;
		return null;
	}

	/**
	 * 获取坐标系中的枚举轴，用法同getNumericAxis
	 * 
	 * @return 枚举轴
	 */
	public EnumAxis getEnumAxis() {
		TickAxis axis = getAxis(this, EnumAxis.class);
		if (axis != null)
			return (EnumAxis) axis;
		return null;
	}

	/**
	 * 生成坐标系的描述信息，通常用于调试。
	 */
	public String toString() {
		return "CartesianCoor Axis1:" + a1.getName() + " Axis2:" + a2.getName();
	}

	/**
	 * 判断两个坐标系是否相等
	 * @return 相等时返回true，否则false
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof CartesianCoor) {
			CartesianCoor cc = (CartesianCoor) obj;
			return cc.a1.equals(a1) && cc.a2.equals(a2);
		}
		return false;
	}

	/**
	 * 是否为极坐标系
	 */
	public boolean isPolarCoor() {
		return false;
	}

	/**
	 * 是否为直角坐标系
	 */
	public boolean isCartesianCoor() {
		return true;
	}

	/**
	 * 是否为包含枚举轴的坐标系
	 */
	public boolean isEnumBased() {
		TickAxis ta = getAxis1();
		return ta.isEnumAxis();
	}
}
