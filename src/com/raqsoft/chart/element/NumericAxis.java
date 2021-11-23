package com.raqsoft.chart.element;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;
import java.lang.Math;
import java.util.*;

import com.raqsoft.dm.*;
import com.raqsoft.util.Variant;
import com.raqsoft.chart.*;
import com.raqsoft.chart.edit.*;
import com.raqsoft.common.*;

/**
 * 数值轴
 * @author Joancy
 *
 */
public class NumericAxis extends TickAxis {
	// 自动计算最大小值的范围
	public boolean autoCalcValueRange = true;
	// 自动范围时从0起始值
	public boolean autoRangeFromZero = true;

	// 值轴属性，最大值
	public double maxValue = 10;

	// 值轴属性，最小值
	public double minValue = 0;

	// 值轴属性，刻度数
	public int scaleNum = 5;

	// 值轴属性，刻度显示格式
	public String format = "#.##";

	// 单位字体
	public String unitFont;// = "宋体";

	// 颜色
	public Color unitColor = Color.blue.darker();

	// 样式
	public int unitStyle;

	// 旋转
	public int unitAngle = 0;

	// 大小
	public int unitSize = 12;

	// 变换
	public int transform = 0; // 0无变换，1比例，2对数，3指数（在Consts中定义常数）

	// 如果是比例变换，比例尺度, 1:scale
	public double scale = 1;

	// 如果是对数变换，对数的底数
	public double logBase = 10;

	// 如果是指数变换，指数的底数
	public double powerExponent = Math.E;

	/* 警戒线定义 */
	public Para warnLineStyle = new Para(new Integer(Consts.LINE_DASHED));
	public Para warnLineWeight = new Para(new Float(1));
	public Para warnLineColor = new Para(Color.red);
	public Sequence warnLineData = null;

	/**
	 * 缺省参数构造函数
	 */
	public NumericAxis() {
	}

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(NumericAxis.class, this);

		String group = "numericaxis";
		paramInfos.add(group, new ParamInfo("autoCalcValueRange",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("autoRangeFromZero",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("maxValue", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("minValue", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("scaleNum", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("format"));

		group = "transform";
		paramInfos.add(group,
				new ParamInfo("transform", Consts.INPUT_TRANSFORM));
		paramInfos.add(group, new ParamInfo("scale", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("logBase", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("powerExponent",
				Consts.INPUT_DOUBLE));

		group = "warnlines";
		paramInfos.add(group, new ParamInfo("warnLineStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("warnLineWeight",
				Consts.INPUT_DOUBLE));
		paramInfos.add(group,
				new ParamInfo("warnLineColor", Consts.INPUT_COLOR));
		paramInfos.add(group,
				new ParamInfo("warnLineData", Consts.INPUT_NORMAL));

		group = "unit";
		paramInfos.add(group, new ParamInfo("unitFont", Consts.INPUT_FONT));
		paramInfos.add(group,
				new ParamInfo("unitStyle", Consts.INPUT_FONTSTYLE));
		paramInfos.add(group, new ParamInfo("unitSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("unitAngle", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("unitColor", Consts.INPUT_COLOR));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}
	
	/**
	 * 获取数据的实数值，兼容文本串写法
	 * @param val 数据值
	 * @return 对应的实数
	 */
	public double getNumber(Object val){
		double tmp;
		if (val instanceof Number) {
			tmp = ((Number) val).doubleValue();
		} else {
			tmp = Double.parseDouble(val.toString());
		}
		return tmp;
	}

	double getValueLength(Object val, boolean isAbsolute) {
		double axisLen = getAxisLength();
		double tmp = getNumber(val);
		double len = 0;
		if (isAbsolute) {
			Number nMax = recoverTickValue(t_maxValue);
			Number nMin = recoverTickValue(t_minValue);
			len = axisLen * (tmp / (nMax.doubleValue() - nMin.doubleValue()));
		} else {
			tmp = transform(tmp);
			len = axisLen * (tmp - t_minValue) / (t_maxValue - t_minValue);
		}
		return len;
	}

	/*
	 * 刻度值总是轴上平均分配，且跟轴的位置有关
	 */
	protected double getTickPosition(Object tickVal) {
		double val = ((Number) tickVal).doubleValue();
		double axisLen = getAxisLength();
		double len = axisLen * (val - t_minValue) / (t_maxValue - t_minValue);

		double pos = 0;
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			pos = getLeftX() + len;
			break;
		case Consts.AXIS_LOC_V:
			pos = getBottomY() - len;
			break;
		case Consts.AXIS_LOC_ANGLE:
			pos = startAngle + len;
			break;
		}

		return pos;
	}

	/**
	 * 获取基值的坐标点
	 * @return Point 坐标点
	 */
	public Point2D getBasePoint() {
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			return new Point2D.Double(t_baseValueLine, getBottomY());
		case Consts.AXIS_LOC_V:
			return new Point2D.Double(getLeftX(), t_baseValueLine);
		}
		return null;
	}

	/**
	 * 获取超链接的形状区域
	 * 
	 * @return Shape 无意义，返回null
	 */
	public Shape getShape() {
		return null;
	}

	private double max(double v, Object d) {
		double max = Double.NEGATIVE_INFINITY;
		if (d instanceof Sequence) {
			Sequence al = (Sequence) d;
			max = ((Number) al.max()).doubleValue();
		} else {
			max = getNumber(d);
		}
		return Math.max(max, v);
	}
	
	private double min(ArrayList dataElements) {
		double minValue = Double.POSITIVE_INFINITY;
		for (int i = 0; i < dataElements.size(); i++) {
			DataElement de = (DataElement) dataElements.get(i);
			if(de.isPhysicalCoor()){
				continue;
			}
			Sequence data = de.getAxisData(name);
			minValue = Math.min(minValue,
					((Number) data.min()).doubleValue());
			if(de instanceof Column){
				Column col = (Column)de;
				Sequence data3 = col.getData3();
				if(data3!=null){
					minValue = Math.min(minValue,((Number) data3.min()).doubleValue());					
				}
			}
		}
		return minValue;
	}

	/**
	 * 绘图前的数据准备
	 */
	public void prepare(ArrayList<DataElement> dataElements) {
		super.prepare(dataElements);
		// 当一数值轴上的图元有不同的堆积类型时，取最大的堆积类型为公共类型
		int stackType = Consts.STACK_NONE;
		for (int i = 0; i < dataElements.size(); i++) {
			DataElement de = dataElements.get(i);
			if(de.isPhysicalCoor()){
				continue;
			}
			
			int tmp;
			if (de instanceof Column) {
				tmp = ((Column) de).stackType;
				if (tmp > stackType) {
					stackType = tmp;
				}
			}
			if (de instanceof Sector) {
				tmp = ((Sector) de).stackType;
				if (tmp > stackType) {
					stackType = tmp;
				}
			}
			if (de instanceof Line) {
				tmp = ((Line) de).stackType;
				if (tmp > stackType) {
					stackType = tmp;
				}
			}
			if (stackType >= Consts.STACK_VALUE)
				break;
		}

		// 角轴也不能自动计算
		if (autoCalcValueRange){
			maxValue = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < dataElements.size(); i++) {
				DataElement de = (DataElement) dataElements.get(i);
				if(de.isPhysicalCoor()){
					continue;
				}
				de.parseNumericAxisData(name);

				Object data;
				if (de instanceof Column) {
					Column co = (Column) de;
					co.stackType = stackType;// 重新调整图元的堆积类型
					data = Column.getMaxValue(de, name);
				} else if (de instanceof Line) {
					Line li = (Line) de;
					li.stackType = stackType;
					data = Column.getMaxValue(de, name);
				} else if (de instanceof Sector) {
					Sector se = (Sector) de;
					se.stackType = stackType;// 重新调整图元的堆积类型
					data = Column.getMaxValue(de, name);
				} else {
					data = de.getAxisData(name);
				}
				maxValue = max(maxValue, data);
			}

			if (stackType == Consts.STACK_PERCENT || autoRangeFromZero) {
				if(maxValue>0){
					minValue = 0;	
				}else{
					maxValue=0;
					minValue = min(dataElements);
				}
			} else{
				minValue = min(dataElements);
			}
			
			t_maxValue = transform(Math.max(maxValue, minValue));
			t_minValue = transform(Math.min(maxValue, minValue));

			double absMax = Math
					.max(Math.abs(t_maxValue), Math.abs(t_minValue));
			double tmpScale = 1;
			while (absMax < scaleNum) { // 去掉小数
				tmpScale *= 0.1;
				absMax *= 10;
			}
			absMax = Math.ceil(absMax);
			if (scaleNum < 1) {// 刻度数目小于1时，自动计算出一个刚好除尽最大值的数目
				int tmp = 5;
				double leave = absMax % tmp;
				while (leave > 0) {
					tmp--;
					leave = absMax % tmp;
				}
				scaleNum = tmp;
			}

				double delta = 0;
				if (stackType == Consts.STACK_NONE){
//					否则仅让数值两头的标签增加10%，防止极值刚好落在坐标轴
					delta = (t_maxValue - t_minValue)*0.1; // 留出极值缝隙
				}
				if (t_minValue >= 0) { // 全为正数
					t_maxValue += delta;
					if(t_minValue>0){
						t_minValue -= delta;	
					}
				} else if (t_maxValue <= 0) { // 全为负数
					t_minValue -= delta;
					if(t_maxValue<0){
						t_maxValue += delta;	
					}
				} else { // 有正有负
					t_maxValue += delta;
					t_minValue -= delta;
				}
		} else {
			t_maxValue = transform(Math.max(maxValue, minValue));
			t_minValue = transform(Math.min(maxValue, minValue));
		}

		switch (transform) {
		case Consts.TRANSFORM_SCALE:
			scale = Math.abs(scale);
			if (scale > 1) {
				t_unitText = "1:" + Utils.format(scale, "#,###");
			} else {
				t_unitText = "1:" + scale;
			}
			break;
		case Consts.TRANSFORM_LOG:
			// 对数和指数都已经使用原值作为坐标值，不必再写上变换单位
			break;
		case Consts.TRANSFORM_EXP:
			break;
		}

		if (!(autoCalcValueRange || stackType > Consts.STACK_NONE)) {
			// 自动计算或堆积图或者角轴，才能设置基值
			t_baseValue = t_minValue;
		}

		createCoorValue();
	}

	/**
	 * 求对数
	 * @param base 底
	 * @param d 数值
	 * @return 对数值
	 */
	public static double log(double base, double d) {
		if (base < 0 || d <= 0 || base == 1) {
			return 0;
		}
		return Math.round(Math.log(d) / Math.log(base) * 1000000d) / 1000000d;
	}

	/**
	 * 求指数
	 * @param base 底
	 * @param exp 指数
	 * @return 指数值
	 */
	public static double power(double base, double exp) {
		if (base < 0 || exp < 0 || base == 1) {
			return 0;
		}
		return Math.round(Math.exp(exp * Math.log(base)) * 1000000d) / 1000000d;
	}

	/*
	 * 对数和指数变换时，把坐标单位还原
	 */
	private double recoverTickValue(double value) {
		switch (transform) {
		case Consts.TRANSFORM_LOG:
			return power(logBase, value);
		case Consts.TRANSFORM_EXP:
			return log(powerExponent, value);
		default:
			return value;
		}
	}

	private double transform(double value) {
		switch (transform) {
		case Consts.TRANSFORM_SCALE:
			return value / scale;
		case Consts.TRANSFORM_LOG:
			return log(logBase, value);
		case Consts.TRANSFORM_EXP:
			return power(powerExponent, value);
		case Consts.TRANSFORM_NONE:
		default:
			return value;
		}
	}

	private void createCoorValue() {
		double tmp, delta;
		if (t_minValue >= 0.0 || t_maxValue <= 0) { // ************************************全为正数或者全为负数
			double dCoor = (t_maxValue - t_minValue) / scaleNum;
			delta = Math.ceil(dCoor);
			double cha = delta - dCoor;
			boolean isLeagalRange = cha <= dCoor * 0.2;// 调整后的最大值不能超过原值的20%;

			if (autoCalcValueRange && isLeagalRange) {// 值的范围超过1的做一下最大值调整 //t_maxValue-t_minValue > 1
								// && delta - dCoor<dCoor
				t_maxValue = Math.ceil(t_maxValue);// 取整
				t_minValue = Math.floor(t_minValue);
				delta = Math.ceil((t_maxValue - t_minValue) / scaleNum);// 间隔数值取整
				for (int i = 0; i <= scaleNum; i++) {
					tmp = t_minValue + i * delta;
					t_coorValue.add(new Double(tmp));
				}
				t_maxValue = t_minValue + scaleNum * delta;// 重新设置最大值
			} else {
				delta = (t_maxValue - t_minValue) / scaleNum;
				for (int i = 0; i <= scaleNum; i++) {
					tmp = t_minValue + i * delta;
					t_coorValue.add(new Double(tmp));
				}
			}
		} else { // ************************************有正有负
			double absMax = t_maxValue;
			double absMin = Math.abs(t_minValue);
			delta = Math.max(absMax, absMin) / scaleNum;

			t_coorValue.add(new Double(0));
			boolean positiveFull = false;
			boolean negativeFull = false;
			for (int i = 1; i <= scaleNum; i++) {
				tmp = i * delta;
				if (!positiveFull) {
					if (tmp <= absMax) {
						t_coorValue.add(new Double(tmp));
					} else {
						t_maxValue = tmp;
						positiveFull = true;
						t_coorValue.add(new Double(tmp));
					}
				}

				if (!negativeFull) {
					if (tmp <= absMin) {
						t_coorValue.add(new Double(-tmp));
					} else {
						t_minValue = -tmp;
						negativeFull = true;
						t_coorValue.add(new Double(-tmp));
					}
				}
			}
			t_coorValue.sort("o");
		}

	}

	String getCoorText(Object coorValue) {
		Number coory = (Number) coorValue;
		Number d = recoverTickValue(((Number) coorValue).doubleValue());
		return Utils.format(d, format);
	}

	/**
	 * 设定轴的数据范围
	 * 饼图是个特殊图形，计算饼图的过程中，要根据不同分类动态设置值域范围
	 * 此时的坐标已经没有意义，一般情况下，坐标系也没有画出来的必要
	 * @param max 最大值
	 * @param min 最小值
	 */
	public void setValueRange(double max, double min) {
		t_maxValue = max;
		t_minValue = min;
	}

	/**
	 * 画图前的数据准备工作
	 */
	public void beforeDraw() {
		double length = getAxisLength();

		double dLen = length * (t_baseValue - t_minValue)
				/ (t_maxValue - t_minValue);
		double end;
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			end = getLeftX();
			t_baseValueLine = (int) (end + dLen);
			break;
		case Consts.AXIS_LOC_V:
			end = getBottomY();
			t_baseValueLine = (int) (end - dLen);
			break;
		case Consts.AXIS_LOC_ANGLE:

			// nothing to do
			break;
		}
	}

	/**
	 * 绘制前景层
	 */
	public void drawFore() {
		if (!isVisible()) {
			return;
		}
		super.drawFore();

		// Draw axis UnitText
		if (!StringUtils.isValidString(t_unitText)) {
			return;
		}
		Font font = Utils.getFont(unitFont, unitStyle, unitSize);
		Graphics2D g = e.getGraphics();
		double x, y;

		int locationType = 0;
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			x = getRightX() + labelIndent;// + textSize.width;
			y = getBottomY();
			Utils.drawText(e, t_unitText, x, y, font, unitColor, unitStyle,
					unitAngle, Consts.LOCATION_LM, true);
			break;
		case Consts.AXIS_LOC_V:
			x = getLeftX();
			y = getTopY() - labelIndent;// - textSize.height;
			Utils.drawText(e, t_unitText, x, y, font, unitColor, unitStyle,
					unitAngle, Consts.LOCATION_CB, true);
			break;
		case Consts.AXIS_LOC_ANGLE:
			// 角轴不绘制
			break;
		}

	}

	private Number warnLineData(int index) {
		Object val = warnLineData.get(index);
		if (val == null) {
			return new Double(0);
		}
		if (val instanceof Number) {
			return (Number) val;
		}
		Number n = Variant.parseNumber(val.toString());
		if (n == null) {
			throw new RQException("Warn line data: [ " + val
					+ " ] is not a number!");
		}
		return n;
	}

	private void drawWarnLine(int index, Point2D p, double x1, double y1,
			double x2, double y2) {
		Line2D line = new Line2D.Double(x1, y1, x2, y2);
		drawWarnShape(index, p, line, null);
	}

	private void drawWarnShape(int index, Point2D p, Shape shape,
			Point2D screenPoint) {
		Color wc = warnLineColor.colorValueNullAsDef(index);
		int ws = warnLineStyle.intValue(index);
		float ww = warnLineWeight.floatValue(index);
		Graphics2D g = e.getGraphics();
		if (Utils.setStroke(g, wc, ws, ww)) {
			g.draw(shape);
			String txt = getCoorText(warnLineData(index));
			int locationType = adjustLabelPosition(p);
			Font font = Utils.getFont(labelFont, labelStyle, labelSize);
			double x, y;
			if (screenPoint == null) {
				x = p.getX();
				y = p.getY();
			} else {
				x = screenPoint.getX();
				y = screenPoint.getY();
			}
			Utils.drawText(e, txt, x, y, font, wc, labelStyle, labelAngle,
					locationType, labelOverlapping);
		}
	}

	/**
	 * 绘制中间层
	 */
	public void draw() {
		super.draw();
		if (warnLineData == null) {
			return;
		}

		ArrayList<ICoor> coorList = e.getCoorList();
		Shape warnShape;
		double x, y;
		Point2D p;
		Graphics2D g = e.getGraphics();
		int ws, locationType;
		float ww;
		Color wc;
		int tCount = warnLineData.length();

		switch (location) {
		case Consts.AXIS_LOC_H:
			for (int i = 0; i < coorList.size(); i++) {
				ICoor coor = coorList.get(i);
				if (coor.isPolarCoor()) {
					continue;
				}
				CartesianCoor cc = (CartesianCoor) coor;
				if (cc.getXAxis() != this) {
					continue;
				}
				int coorShift = cc.get3DShift();
				TickAxis yAxis = (TickAxis) cc.getYAxis();
				for (int t = 1; t <= tCount; t++) {
					Object tickVal = warnLineData(t);
					x = getTickPosition(tickVal);
					y = getBottomY() + coorThick;
					p = new Point2D.Double(x, y);

					drawWarnLine(t, p, x + coorShift, yAxis.getBottomY()
							- coorShift, x + coorShift, yAxis.getTopY()
							- coorShift);
				}
			}
			break;
		case Consts.AXIS_LOC_V:
			for (int i = 0; i < coorList.size(); i++) {
				ICoor coor = coorList.get(i);
				if (coor.isPolarCoor()) {
					continue;
				}
				CartesianCoor cc = (CartesianCoor) coor;
				if (cc.getYAxis() != this) {
					continue;
				}
				int coorShift = cc.get3DShift();
				TickAxis xAxis = (TickAxis) cc.getXAxis();
				for (int t = 1; t <= tCount; t++) {
					Object tickVal = warnLineData(t);
					x = getLeftX() - coorThick;
					y = getTickPosition(tickVal);
					p = new Point2D.Double(x, y);
					drawWarnLine(t, p, xAxis.getLeftX() + coorShift, y
							- coorShift, xAxis.getRightX() + coorShift, y
							- coorShift);
				}
			}
			break;
		case Consts.AXIS_LOC_POLAR:
			for (int i = 0; i < coorList.size(); i++) {
				ICoor coor = coorList.get(i);
				if (coor.isCartesianCoor()) {
					continue;
				}
				PolarCoor pc = (PolarCoor) coor;
				if (pc.getPolarAxis() != this) {
					continue;
				}
				TickAxis angleAxis = (TickAxis) pc.getAngleAxis();
				Point2D orginalPoint = new Point2D.Double(getLeftX(),
						getBottomY()); // 原点
				for (int t = 1; t <= tCount; t++) {
					Object tickVal = warnLineData(t);
					ArrayList<Point2D> points = new ArrayList<Point2D>();
					x = getTickPosition(tickVal);
					y = getBottomY() + coorThick;
					p = new Point2D.Double(x, y);

					if (isPolygonalRegion) {
						double polarLen = getTickPosition(tickVal) - getLeftX();
						for (int n = 1; n <= angleAxis.t_coorValue.length(); n++) {
							Object angleTick = angleAxis.t_coorValue.get(n);
							double angle = angleAxis.getTickPosition(angleTick);
							Point2D polarPoint = new Point2D.Double(polarLen,
									angle);
							points.add(pc.getScreenPoint(polarPoint));
						}
						warnShape = Utils.getPath2D(points, isCircleAngle());
					} else { // 扇形
						double w, h, tmpLen;
						tmpLen = x - orginalPoint.getX();
						x = orginalPoint.getX() - tmpLen;
						y = orginalPoint.getY() - tmpLen;
						w = tmpLen * 2;
						h = w;

						warnShape = new Arc2D.Double(x, y, w, h,
								angleAxis.startAngle, angleAxis.endAngle
										- angleAxis.startAngle,
								java.awt.geom.Arc2D.OPEN);
					}
					drawWarnShape(t, p, warnShape, null);
				}
			}
			break;
		case Consts.AXIS_LOC_ANGLE:
			for (int i = 0; i < coorList.size(); i++) {
				ICoor coor = coorList.get(i);
				if (coor.isCartesianCoor()) {
					continue;
				}
				PolarCoor pc = (PolarCoor) coor;
				if (pc.getAngleAxis() != this) {
					continue;
				}
				TickAxis polarAxis = (TickAxis) pc.getPolarAxis();
				Point2D orginalPoint = new Point2D.Double(polarAxis.getLeftX(),
						polarAxis.getBottomY()); // 原点
				double polarLen = polarAxis.getAxisLength();
				for (int t = 1; t <= tCount; t++) {
					Object tickVal = warnLineData(t);
					double angle = getValueLen(tickVal);

					if (isPolygonalRegion) {
					} else { // 扇形
					}
					p = new Point2D.Double(polarLen, angle);
					warnShape = new Line2D.Double(orginalPoint,
							pc.getScreenPoint(p));
					drawWarnShape(t, p, warnShape, pc.getScreenPoint(p));
				}
			}
			break;
		}

	}

	/**
	 * 是否枚举轴
	 * @return false
	 */
	public boolean isEnumAxis() {
		return false;
	}

	/**
	 * 是否日期轴
	 * @return false
	 */
	public boolean isDateAxis() {
		return false;
	}

	/**
	 * 是否数值轴
	 * @return true
	 */
	public boolean isNumericAxis() {
		return true;
	}

	// 所有绘制图形相关的中间计算变量都由transient限定词指出，并且变量名由t_作为前缀
	private transient double t_maxValue=Double.MIN_VALUE, t_minValue=Double.MAX_VALUE;

	// 基值,通常为0；如果用户指定了最小值，则基值为最小值
	private transient double t_baseValue = 0;
	private transient int t_baseValueLine = 0;
	// 单位说明
	private transient String t_unitText = "";

	public static void main(String[] args) {
		double d = 1000;
		double tmp = NumericAxis.log(10, d);
		System.out.println("1:" + tmp);
		tmp = NumericAxis.power(10, tmp);
		System.out.println("2:" + tmp);
	}

	public void checkDataMatch(Sequence data){
		if(data!=null && data.length()>1){
			Object one = data.get(1);
			getNumber(one);
		}
	}


	public double animateDoubleValue(Object val){
		return getNumber( val );
	}
	
}
