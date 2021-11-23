package com.raqsoft.chart.element;

import com.raqsoft.chart.*;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

import com.raqsoft.common.*;
import com.raqsoft.dm.*;
import com.raqsoft.chart.edit.*;

/**
 * 刻度轴 所有实际数据值的父类
 * 
 * @author Joancy
 */

public abstract class TickAxis extends ObjectElement implements IAxis {
	private static int ARROW_SHIFT = 8;

	/***** 逻辑轴属性 *****/
	// 轴名称
	public String name; // 名称

	// 是否可见
	public boolean visible = true;

	// 轴位置
	public int location = Consts.AXIS_LOC_H;

	// 横轴属性：起始物理横坐标(xStart>xEnd表示向左)
	public double xStart = 0.1;

	// 横轴属性：结束物理横坐标（不含箭头）
	public double xEnd = 0.8;

	// 横轴属性：纵坐标
	public double xPosition = 0.8;

	// 纵轴属性：起始物理纵坐标(yStart<yEnd表示向下)
	public double yStart = 0.8;

	// 纵轴属性：结束物理纵坐标
	public double yEnd = 0.1;

	// 纵轴属性：横坐标
	public double yPosition = 0.1;

	// 极轴属性：原点物理横坐标
	public double polarX = 0.4;

	// 极轴属性：原点物理纵坐标
	public double polarY = 0.5;

	// 极轴长度
	public double polarLength = 0.3;

	// 极轴的高宽比，不等于1即椭圆
	// public double HWRage = 1;

	// 角轴属性：起始角度(0-360度)
	public double startAngle = 0;

	// 角轴属性：结束角度(0-360度)
	public double endAngle = 360;

	// 系列属性：并列数,该参数 意义不大，可以直接指定series属性从而确定系列的显示个数
	// public int seriesNum = 0;

	// 横轴、纵轴定义：是否立体
	// 只有线、柱、梯形支持立体
	public boolean is3D = false;

	// 3d厚度比率，直角坐标系下为厚度与系列宽度比，数值轴时，可以数值像素值。极坐标系下暂不支持
	public double threeDThickRatio = 0.38;

	/***** 刻度轴外观属性 *****/
	// 轴色
	public Color axisColor = Color.LIGHT_GRAY;

	// 轴线形
	public int axisLineStyle = Consts.LINE_SOLID;

	// 轴线宽
	public int axisLineWeight = 1;

	// 轴箭头，固定长度？
	public int axisArrow = Consts.LINE_ARROW_NONE;

	// 轴标题文字
	public String title;

	// 轴标题文字字体
	public String titleFont;// = "宋体";

	// 轴标题文字字型
	public int titleStyle;

	// 轴标题文字字号
	public int titleSize = 16;

	// 标题文字留空，为标题文字至刻度标签或者坐标轴中心（无刻度标签时）的距离
	public int titleIndent = 2;

	// 轴标题文字字色
	public Color titleColor = Color.black;

	// 轴标题倾斜角度
	public int titleAngle;

	// 是否有刻度标签
	public boolean allowLabels = true;

	// 刻度标签字体
	public String labelFont;// = "宋体";

	// 刻度标签字型
	public int labelStyle;

	// 刻度标签字号
	public int labelSize = 12;

	// 刻度标签留空，为刻度标签至坐标轴中心线的距离
	public int labelIndent = 2;

	// 刻度标签字色
	public Color labelColor = Color.darkGray;

	// 刻度标签显示间隔
	public int labelStep = 0;

	// 刻度标签倾斜角度
	public int labelAngle = 0;

	// 刻度标签能否重叠
	public boolean labelOverlapping = false;

	// 刻度线位置
	public int scalePosition = Consts.TICK_LEFTDOWN;

	// 刻度线线形
	public int scaleStyle = Consts.LINE_SOLID;

	// 刻度线线宽
	public int scaleWeight = 1;

	// 刻度线尺寸
	public int scaleLength = 3;

	// 刻度线显示间隔
	public int displayStep = 0;

	// 间隔区属性，是否有刻度间隔区
	public boolean allowRegions = true;

	// // 间隔区属性，间隔区起止于刻度中间位置
	// public boolean regionMiddle = true;

	// 间隔区属性，刻度间隔线
	public int regionLineStyle = Consts.LINE_SOLID;

	// 间隔区属性，刻度间隔线色
	public Color regionLineColor = Color.lightGray;

	// 间隔区属性，刻度间隔线宽
	public int regionLineWeight = 1;

	// 间隔区属性，刻度间隔区颜色
	public Para regionColors = new Para(null);// new Color(241,243,235)

	// 间隔区属性，刻度间隔区透明度(不包括间隔线)
	public float regionTransparent = 0.6f;

	// 间隔区属性，刻度间隔区是否为多边形（只在极坐标中有效，为false时为扇形或环形）
	public boolean isPolygonalRegion = false;

	/**
	 * 缺省值构造函数
	 */
	public TickAxis() {
		// 缺省间隔色为透明色间隔灰色
		Sequence seq = new Sequence();
		ChartColor c1 = new ChartColor(Color.white);
		c1.setGradient(false);
		seq.add(c1);
		ChartColor c2 = new ChartColor(new Color(241, 243, 235));
		c2.setGradient(false);
		seq.add(c2);
		regionColors = new Para(seq);// 给区域赋值初始颜色
	}

	/**
	 * 获取编辑参数信息列表
	 * 
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(TickAxis.class, this);
		paramInfos.add(new ParamInfo("name"));
		paramInfos.add(new ParamInfo("visible", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("location", Consts.INPUT_AXISLOCATION));
		paramInfos.add(new ParamInfo("is3D", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("threeDThickRatio", Consts.INPUT_DOUBLE));

		String group = "xaxis";
		paramInfos.add(group, new ParamInfo("xStart", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("xEnd", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("xPosition", Consts.INPUT_DOUBLE));

		group = "yaxis";
		paramInfos.add(group, new ParamInfo("yStart", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("yEnd", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("yPosition", Consts.INPUT_DOUBLE));

		group = "polaraxis";
		paramInfos.add(group, new ParamInfo("polarX", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("polarY", Consts.INPUT_DOUBLE));
		paramInfos
				.add(group, new ParamInfo("polarLength", Consts.INPUT_DOUBLE));

		group = "angleAxis";
		paramInfos.add(group, new ParamInfo("startAngle", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("endAngle", Consts.INPUT_DOUBLE));

		group = "axisLine";
		paramInfos.add(group, new ParamInfo("axisColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("axisLineStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("axisLineWeight",
				Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("axisArrow", Consts.INPUT_ARROW));

		group = "axisTitle";
		paramInfos.add(group, new ParamInfo("title"));
		paramInfos.add(group, new ParamInfo("titleFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("titleStyle",
				Consts.INPUT_FONTSTYLE));
		paramInfos
				.add(group, new ParamInfo("titleSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group,
				new ParamInfo("titleIndent", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("titleColor", Consts.INPUT_COLOR));
		paramInfos
				.add(group, new ParamInfo("titleAngle", Consts.INPUT_INTEGER));

		group = "labels";
		paramInfos.add(group, new ParamInfo("allowLabels",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("labelFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("labelStyle",
				Consts.INPUT_FONTSTYLE));
		paramInfos
				.add(group, new ParamInfo("labelSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group,
				new ParamInfo("labelIndent", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("labelColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("labelStep", Consts.INPUT_INTEGER));
		paramInfos
				.add(group, new ParamInfo("labelAngle", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("labelOverlapping",
				Consts.INPUT_CHECKBOX));

		group = "scaleLine";
		paramInfos.add(group,
				new ParamInfo("scalePosition", Consts.INPUT_TICKS));
		paramInfos.add(group, new ParamInfo("scaleStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group,
				new ParamInfo("scaleWeight", Consts.INPUT_INTEGER));
		paramInfos.add(group,
				new ParamInfo("scaleLength", Consts.INPUT_INTEGER));
		paramInfos.add(group,
				new ParamInfo("displayStep", Consts.INPUT_INTEGER));

		group = "region";
		paramInfos.add(group, new ParamInfo("allowRegions",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("regionLineStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("regionLineColor",
				Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("regionLineWeight",
				Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("regionColors",
				Consts.INPUT_CHARTCOLOR));
		paramInfos.add(group, new ParamInfo("regionTransparent",
				Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("isPolygonalRegion",
				Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取轴的名称
	 * 
	 * @return 名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取轴的方位
	 * 
	 * @return 轴位置，值参考Consts.AXIS_LOC_X;
	 */
	public int getLocation() {
		return location;
	}

	/**
	 * 当前轴是否可视
	 * @return 可视返回true，否则返回false
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * 绘图前的数据准备
	 * 调整一些设置不合理的属性，或者更方便使用的属性值
	 * 
	 * @param dataElements 数据图元
	 */
	public void prepare(ArrayList<DataElement> dataElements) {
		// 角坐标
		startAngle = in360(startAngle);
		endAngle = in360(endAngle);
		if (startAngle > endAngle) {
			double d = startAngle;
			startAngle = endAngle;
			endAngle = d;
		}

		// 刻度间隔的个数加1后，直接求余(%)，结果是否为0即可；刻度间隔为1表示为隔1个显示，也即 %2 的结果
		displayStep = displayStep + 1;
		labelStep = labelStep + 1;

		for (int i = 0; i < dataElements.size(); i++) {
			DataElement de = dataElements.get(i);
			if (de.hasGradientColor()) {
				useGradient = true;
				break;
			}
		}

	}

	/**
	 * 判断角轴的坐标域是否是个圆
	 * 圆域和区间域会造成刻度标识的不同
	 * 
	 * @return boolean 是圆域返回true，否则返回false
	 */
	public boolean isCircleAngle() {
		double angleRange = endAngle - startAngle;
		return angleRange == 360;
	}

	/**
	 * 获取轴的像素长度
	 * 其中角轴则为角轴范围
	 * @return 轴的长度
	 */
	public double getAxisLength() {
		Point2D p1, p2;
		p1 = getStartPoint();
		p2 = getEndPoint();
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			return Math.abs(p2.getX() - p1.getX());
		case Consts.AXIS_LOC_ANGLE:
		case Consts.AXIS_LOC_V:
			return Math.abs(p2.getY() - p1.getY());
		}
		return 0;
	}

	double in360(double angle) {
		double d = angle;
		while (d < 0) {
			d = d + 360;
		}
		while (d > 360) {
			d = d - 360;
		}
		return d;
	}

	private Point2D getPoint(boolean getStart) {
		double x = 0, y = 0;
		switch (location) {
		case Consts.AXIS_LOC_H:
			if (getStart) {
				x = e.getXPixel(xStart);
			} else {
				x = e.getXPixel(xEnd);
			}
			y = e.getYPixel(xPosition);
			break;
		case Consts.AXIS_LOC_V:
			x = e.getXPixel(yPosition);
			if (getStart) {
				y = e.getYPixel(yStart);
			} else {
				y = e.getYPixel(yEnd);
			}
			break;
		case Consts.AXIS_LOC_POLAR:
			if (getStart) {
				x = e.getXPixel(polarX);
			} else {
				x = e.getXPixel(polarX) + e.getXPixel(polarLength);
			}
			y = e.getYPixel(polarY);
			break;
		case Consts.AXIS_LOC_ANGLE:
			if (getStart) {
				y = startAngle;
			} else {
				y = endAngle;
			}
			break;
		}
		return new Point2D.Double(x, y);
	}

	Point2D getStartPoint() {
		return getPoint(true);
	}

	Point2D getEndPoint() {
		return getPoint(false);
	}

	/**
	 * 获取坐标轴的顶点Y坐标
	 * 角轴时返回endAngle
	 * @return y坐标值
	 */
	public double getTopY() {
		switch (location) {
		case Consts.AXIS_LOC_H:
			return e.getYPixel(xPosition);
		case Consts.AXIS_LOC_V:
			return Math.min(e.getYPixel(yStart), e.getYPixel(yEnd));
		case Consts.AXIS_LOC_POLAR:
			return e.getYPixel(polarY);
		case Consts.AXIS_LOC_ANGLE:
			return (int) endAngle;
		}
		return 0;
	}

	/**
	 * 获取坐标轴的底点Y坐标
	 * 角轴时返回startAngle
	 * @return y坐标值
	 */
	public double getBottomY() {
		switch (location) {
		case Consts.AXIS_LOC_H:
			return e.getYPixel(xPosition);
		case Consts.AXIS_LOC_V:
			return Math.max(e.getYPixel(yStart), e.getYPixel(yEnd));
		case Consts.AXIS_LOC_POLAR:
			return e.getYPixel(polarY);
		case Consts.AXIS_LOC_ANGLE:
			return (int) startAngle;
		}
		return 0;
	}

	/**
	 * 获取坐标轴的左边X坐标
	 * 角轴时无意义
	 * @return x坐标值
	 */
	public double getLeftX() {
		switch (location) {
		case Consts.AXIS_LOC_H:
			return Math.min(e.getXPixel(xStart), e.getXPixel(xEnd));
		case Consts.AXIS_LOC_V:
			return e.getXPixel(yPosition);
		case Consts.AXIS_LOC_POLAR:
			return e.getXPixel(polarX);
		}
		return 0;
	}

	/**
	 * 获取坐标轴的右边X坐标
	 * 角轴时无意义
	 * @return x坐标值
	 */
	public double getRightX() {
		switch (location) {
		case Consts.AXIS_LOC_H:
			return Math.max(e.getXPixel(xStart), e.getXPixel(xEnd));
		case Consts.AXIS_LOC_V:
			return e.getXPixel(yPosition);
		case Consts.AXIS_LOC_POLAR:
			return e.getXPixel(polarX) + e.getXPixel(polarLength);
		}
		return 0;
	}

	/*
	 * 直角坐标系时，由于是规则的矩形，画刻度线时只画一条边； 如果画整个regionShape，如果是虚线时重叠画会让虚线看起来不虚
	 * 第一步画填充色，第二步才是边线，否则后画的区间会覆盖边线，线要后画。
	 * 
	 * @param regionShape
	 *            Shape
	 * @param regionColor
	 *            Color
	 * @param location
	 *            int 每个区间只画右顶边，如果轴不在最边时，会缺区间线，所以第一个刻度时，使用doubleEdge画两边
	 */
	private void drawRegion(Shape regionShape, ChartColor regionColor, int step) {
		drawRegion(regionShape, regionColor, step, false);
	}

	private void drawRegion(Shape regionShape, ChartColor regionColor,
			int step, boolean doubleEdge) {
		Graphics2D g = e.getGraphics();
		Rectangle rect = regionShape.getBounds();
		if (step == 1) {
			boolean isSet = false;
			if (regionColor.isDazzle()) {
				CubeColor ccr = new CubeColor(regionColor.getColor1());
				ChartColor tmpcc = new ChartColor();
				tmpcc.setColor1(ccr.getF2());
				tmpcc.setColor2(ccr.getF1());
				tmpcc.setAngle(regionColor.getAngle());
				isSet = Utils.setPaint(g, rect.x, rect.y, rect.width,
						rect.height, tmpcc);
			} else {
				isSet = Utils.setPaint(g, rect.x, rect.y, rect.width,
						rect.height, regionColor);
			}
			if (isSet) {
				Utils.fillPaint(g, regionShape, regionTransparent);
			}
		} else if (step == 2) {
			if (Utils.setStroke(g, regionLineColor, regionLineStyle,
					regionLineWeight)) {
				int x, y;
				if (location == Consts.AXIS_LOC_H) {
					x = rect.x;
					y = rect.y;
					g.drawLine(x + rect.width, y, x + rect.width, y
							+ rect.height);
					if (doubleEdge) {
						g.drawLine(x, y, x, y + rect.height);
					}
				} else if (location == Consts.AXIS_LOC_V) {
					x = rect.x;
					y = rect.y;
					// 绘制上下边虽然有重叠绘制，但是如果最底边的轴线挪到中间后，不会出现底边空缺
					g.drawLine(x, y, x + rect.width, y);
					if (doubleEdge) {
						g.drawLine(x, y + rect.height, x + rect.width, y
								+ rect.height);
					}
				} else {
					g.draw(regionShape);
				}
			}
		}
	}

	// 将极坐标点转成直角坐标点，并添加到多边形中
	private void addPolarPoint(java.awt.Polygon polygon, PolarCoor pc,
			double polarLen, double angle) {
		Point2D p = new Point2D.Double(polarLen, angle);
		p = pc.getScreenPoint(p);
		polygon.addPoint((int) p.getX(), (int) p.getY());
	}

	/**
	 * 绘制背景层
	 */
	public void drawBack() {
		if (!isVisible()) {
			return;
		}
		// 需要先将区间画完
		drawRegionStep(1);
		drawRegionStep(2);
	}

	void drawAxisBorder() {
		Graphics2D g = e.getGraphics();
		ArrayList coorList = e.getCoorList();
		// 坐标轴
		if (Utils.setStroke(g, axisColor, axisLineStyle, axisLineWeight)) {
			double x, y, w, h;
			switch (location) {
			case Consts.AXIS_LOC_H:
			case Consts.AXIS_LOC_POLAR:
				Utils.setStroke(g, axisColor, axisLineStyle, axisLineWeight);
				// 1.画轴线
				Utils.drawLine(g, getStartPoint(), getEndPoint());
				x = getRightX();
				y = getBottomY();

				int style = Utils.getArrow(axisArrow);
				if (style != Consts.LINE_ARROW_NONE) {
					if (style == Consts.LINE_ARROW_L) {
						x = getLeftX();
						Utils.drawLine(g, x, y, x - ARROW_SHIFT, y);
						Utils.drawLineArrow(g, x - ARROW_SHIFT, y, 0, axisArrow);
					} else {
						Utils.drawLine(g, x, y, x + ARROW_SHIFT, y);
						Utils.drawLineArrow(g, x + ARROW_SHIFT, y, 0, axisArrow);
					}
				}

				if (location == Consts.AXIS_LOC_H) { // 直角坐标系绘制3D平行四边形
					for (int i = 0; i < coorList.size(); i++) {
						Object coor = coorList.get(i);
						if (!(coor instanceof CartesianCoor)) {
							continue;
						}
						CartesianCoor cc = (CartesianCoor) coor;
						if (cc.getXAxis() != this) {
							continue;
						}
						int coorShift = cc.get3DShift();
						TickAxis yAxis = cc.getYAxis();
						x = getLeftX();
						y = yAxis.getBottomY();
						Utils.setStroke(g, axisColor, axisLineStyle,
								axisLineWeight);
						double x2 = getRightX();
						if (coorShift == 0) {
							// 2.不是3D轴时，已经在1画轴线，此处不能再画；否则横轴不在底边时，绘制两条不同的线
							// g.drawLine((int)x, (int)y, (int)x2, (int)y);
						} else {
							java.awt.Polygon polygon = new java.awt.Polygon();
							polygon.addPoint((int) x, (int) y);
							polygon.addPoint((int) x + coorShift, (int) y
									- coorShift);
							polygon.addPoint((int) x2 + coorShift, (int) y
									- coorShift);
							polygon.addPoint((int) x2, (int) y);
							g.draw(polygon);
						}
					}
				}
				break;
			case Consts.AXIS_LOC_V:
				Utils.setStroke(g, axisColor, axisLineStyle, axisLineWeight);
				Utils.drawLine(g, getStartPoint(), getEndPoint());
				x = getLeftX();
				y = getTopY();

				int styl = Utils.getArrow(axisArrow);
				if (styl != Consts.LINE_ARROW_NONE) {
					if (styl == Consts.LINE_ARROW_L) {
						y = getBottomY();
						Utils.drawLine(g, x, y, x, y + ARROW_SHIFT);
						Utils.drawLineArrow(g, x, y + ARROW_SHIFT,
								-Math.PI / 2, axisArrow);
					} else {
						Utils.drawLine(g, x, y, x, y - ARROW_SHIFT);
						Utils.drawLineArrow(g, x, y - ARROW_SHIFT,
								-Math.PI / 2, axisArrow);
					}
				}

				for (int i = 0; i < coorList.size(); i++) {
					Object coor = coorList.get(i);
					if (!(coor instanceof CartesianCoor)) {
						continue;
					}
					CartesianCoor cc = (CartesianCoor) coor;
					if (cc.getYAxis() != this) {
						continue;
					}
					int coorShift = cc.get3DShift();
					Utils.setStroke(g, axisColor, axisLineStyle, axisLineWeight);
					TickAxis xAxis = (TickAxis) cc.getXAxis();
					double x1 = xAxis.getLeftX();
					double x2 = xAxis.getRightX();
					double thisX = getLeftX(); // 垂直轴自身的x
					double d1 = Math.abs(x1 - thisX);
					double d2 = Math.abs(x2 - thisX);
					x = (d1 < d2) ? x1 : x2; // 谁离得近就取谁
					y = getBottomY();
					double y2 = getTopY();
					if (coorShift == 0) {
						// 注释道理同上
						// g.drawLine((int)x, (int)y, (int)x, (int)y2);
					} else {
						// 立体时，平行四边形总是靠近当前轴的x
						java.awt.Polygon polygon = new java.awt.Polygon();
						polygon.addPoint((int) x, (int) y);
						polygon.addPoint((int) x, (int) y2);
						polygon.addPoint((int) x + coorShift, (int) y2
								- coorShift);
						polygon.addPoint((int) x + coorShift, (int) y
								- coorShift);
						g.draw(polygon);
					}
				}
				break;
			case Consts.AXIS_LOC_ANGLE:
				Point2D p1 = null,
				p2;
				for (int i = 0; i < coorList.size(); i++) {
					Object coor = coorList.get(i);
					if (!(coor instanceof PolarCoor)) {
						continue;
					}
					PolarCoor pc = (PolarCoor) coor;
					if (pc.getAngleAxis() != this) {
						continue;
					}
					TickAxis polarAxis = (TickAxis) pc.getPolarAxis();
					double polarLen = polarAxis.getAxisLength();
					if (isPolygonalRegion) {
						int tCount = t_coorValue.length();
						for (int t = 1; t <= tCount; t++) {
							Object tickVal = t_coorValue.get(t);
							double angle = getTickPosition(tickVal);
							p2 = new Point2D.Double(polarLen, angle);
							p2 = pc.getScreenPoint(p2);
							Utils.drawLine(g, p1, p2);
							p1 = p2;
							if (isCircleAngle() && t == tCount) {
								p2 = new Point2D.Double(polarLen, 360);
								p2 = pc.getScreenPoint(p2);
								Utils.drawLine(g, p1, p2);
							}
						}
					} else { // 扇形
						Point2D orginalPoint = new Point2D.Double(
								polarAxis.getLeftX(), polarAxis.getBottomY()); // 原点
						x = orginalPoint.getX() - polarLen;
						y = orginalPoint.getY() - polarLen;
						w = polarLen * 2;
						h = w;
						java.awt.geom.Arc2D axisArc = new java.awt.geom.Arc2D.Double(
								x, y, w, h, startAngle, endAngle - startAngle,
								java.awt.geom.Arc2D.OPEN);
						g.draw(axisArc);
					}
				}
				break;
			}
		} else if (useGradient) {// 无边框而且使用了渐变色的3D坐标轴，绘制3d炫平台
			double x, y, w, h;
			if (location == Consts.AXIS_LOC_H) { // 直角坐标系绘制3DCube平台
				for (int i = 0; i < coorList.size(); i++) {
					Object coor = coorList.get(i);
					if (!(coor instanceof CartesianCoor)) {
						continue;
					}
					CartesianCoor cc = (CartesianCoor) coor;
					if (cc.getXAxis() != this) {
						continue;
					}
					int coorShift = cc.get3DShift();
					if (coorShift == 0) {
						continue;
					}
					TickAxis yAxis = (TickAxis) cc.getYAxis();
					x = (int) getLeftX();
					y = (int) yAxis.getBottomY();
					double x2 = getRightX();
					w = (int) x2 - x;
					h = Utils.getPlatformH(coorShift);
					coorThick = h;
					ChartColor fillColor = new ChartColor(axisColor);
					Utils.draw3DRect(g, x, y, w, h, null, 0, 0, false, false,
							1, fillColor, true, coorShift);
				}
			} else if (location == Consts.AXIS_LOC_V) {
				for (int i = 0; i < coorList.size(); i++) {
					Object coor = coorList.get(i);
					if (!(coor instanceof CartesianCoor)) {
						continue;
					}
					CartesianCoor cc = (CartesianCoor) coor;
					if (cc.getYAxis() != this) {
						continue;
					}
					int coorShift = cc.get3DShift();
					if (coorShift == 0) {
						continue;
					}
					// 立体时，平行四边形总是靠近当前轴的x
					TickAxis xAxis = (TickAxis) cc.getXAxis();
					double x1 = xAxis.getLeftX();
					double x2 = xAxis.getRightX();
					double thisX = getLeftX(); // 垂直轴自身的x
					double d1 = Math.abs(x1 - thisX);
					double d2 = Math.abs(x2 - thisX);
					w = Utils.getPlatformH(coorShift);
					x = (int) (((d1 < d2) ? x1 : x2) - w); // 谁离得近就取谁
					y = (int) getTopY();
					h = (int) getBottomY() - y;
					coorThick = w;
					ChartColor fillColor = new ChartColor(axisColor);
					Utils.draw3DRect(g, x, y, w, h, null, 0, 0, false, false,
							1, fillColor, false, coorShift);
				}

			}

		}

	}

	/*
	 * 计算对象值val在轴上长度； 有两种计算方式。
	 * 1：绝对长度 即val相对于轴刻度范围，所占有的长度，一般用于半径计算。
	 * 枚举轴是val比系列宽度；日期轴是按val天算长度，数值轴则按val算长度；不分大小与1的情况； 
	 * 2:刻度长度  即val作为刻度范围在轴上相对于轴原点的长度 
	 * 举例：轴的刻度范围是[50,60,70,80,90,100] 计算值60的长度，
	 * 1方式为axisLen*60/(100-50)， 
	 * 2方式为刻度60到刻度50的长度；
	 */
	abstract double getValueLength(Object val, boolean isAbsolute);

	/**
	 * 用于动画插值计算时，将该轴数据转换为数值坐标。
	 * @param val 要转换的刻度值
	 * @return double精度的数值坐标
	 */
	public abstract double animateDoubleValue(Object val);
	
	/**
	 * 获取数值val在轴上的作为半径用的长度
	 * 
	 * @param val 逻辑数值
	 * @return 
	 */
	public double getValueRadius(double val) {
		return getValueLength(new Double(val), true);
	}

	/**
	 * 获取数据值val在轴上的长度
	 * 
	 * @param val 数据值
	 * @return 像素单位的长度
	 */
	public double getValueLen(Object val) {
		return getValueLength(val, false);
	}

	/**
	 * 计算刻度值根据轴所处的位置，位于屏幕的坐标值 由于数值轴有变换，所以数值轴的tickVal要先反变换，才能计算刻度位置
	 * 
	 * @param tickVal
	 * @return
	 */
	protected double getTickPosition(Object tickVal) {
		double len = getValueLength(tickVal, false);// 计算刻度位置
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

	private void drawRegionStep(int step) {
		if (!allowRegions)
			return;
		// 只有第一个区间需要画双边
		boolean doubleEdge = true;

		ArrayList<ICoor> coorList = e.getCoorList();
		ChartColor tmpcc;
		Shape regionShape;
		Point2D p1, p2;
		Graphics2D g = e.getGraphics();
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
				p1 = new Point2D.Double(getLeftX() + coorShift, yAxis.getTopY()
						- coorShift);
				int tCount = t_coorValue.length();
				int rc = 0;
				for (int t = 1; t <= tCount; t++) {
					if ((t - 1) % displayStep != 0) {
						continue;
					}
					Object tickVal = t_coorValue.get(t);
					double tickPosition = getTickPosition(tickVal);
					// 不同类型的轴或者不同位置的轴，刻度标注点起，终位置不同
					if (t == 1 && tickPosition == getLeftX()) {
						continue;
					}
					if (Utils.setStroke(g, axisColor, scaleStyle, scaleWeight)) { // 3D轴的刻度线
						Utils.drawLine(g, tickPosition, yAxis.getBottomY(),
								tickPosition + coorShift, yAxis.getBottomY()
										- coorShift);
					}

					p2 = new Point2D.Double(tickPosition + coorShift,
							yAxis.getTopY() - coorShift);
					regionShape = new Rectangle2D.Double(p1.getX(), p1.getY(),
							p2.getX() - p1.getX(), yAxis.getAxisLength());
					tmpcc = regionColors.chartColorValue(++rc);
					drawRegion(regionShape, tmpcc, step, doubleEdge);
					doubleEdge = false;
					p1 = p2;

					if (t == tCount && tickPosition < getRightX()) {
						p2 = new Point2D.Double(getRightX() + coorShift, 0);
						regionShape = new Rectangle2D.Double(p1.getX(),
								p1.getY(), p2.getX() - p1.getX(),
								yAxis.getAxisLength());
						tmpcc = regionColors.chartColorValue(++rc);
						drawRegion(regionShape, tmpcc, step);
					}
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
				p1 = new Point2D.Double(xAxis.getLeftX() + coorShift,
						getBottomY() - coorShift);
				int tCount = t_coorValue.length();
				int rc = 0;
				for (int t = 1; t <= tCount; t++) {
					if ((t - 1) % displayStep != 0) {
						continue;
					}
					Object tickVal = t_coorValue.get(t);
					double tickPosition = getTickPosition(tickVal);
					if (t == 1 && tickPosition == getBottomY()) {
						continue;
					}
					if (t != 1
							&& t != tCount
							&& Utils.setStroke(g, axisColor, scaleStyle,
									scaleWeight)) { // 3D轴的刻度线
						// 两头的线会跟四边形重叠，就不要了
						// 立体时，平行四边形总是靠近当前轴的x
						double x1 = xAxis.getLeftX();
						double x2 = xAxis.getRightX();
						double thisX = getLeftX(); // 垂直轴自身的x
						double d1 = Math.abs(x1 - thisX);
						double d2 = Math.abs(x2 - thisX);
						double x = (d1 < d2) ? x1 : x2; // 谁离得近就取谁
						Utils.drawLine(g, x, tickPosition, x + coorShift,
								tickPosition - coorShift);
					}

					p2 = new Point2D.Double(p1.getX(), tickPosition - coorShift);
					regionShape = new Rectangle2D.Double(p2.getX(), p2.getY(),
							xAxis.getAxisLength(), p1.getY() - p2.getY());

					tmpcc = regionColors.chartColorValue(++rc);
					drawRegion(regionShape, tmpcc, step, doubleEdge);
					doubleEdge = false;
					p1 = p2;

					if (t == tCount && tickPosition != getTopY()) {
						p2 = new Point2D.Double(p1.getX(),// + coorShift
								getTopY() - coorShift);
						regionShape = new Rectangle2D.Double(p2.getX(),
								p2.getY(), xAxis.getAxisLength(), p1.getY()
										- p2.getY());
						tmpcc = regionColors.chartColorValue(++rc);
						drawRegion(regionShape, tmpcc, step);
					}
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
				java.awt.geom.Area area1 = null, area2;
				int tCount = t_coorValue.length();
				int rc = 0;
				for (int t = 1; t <= tCount; t++) {
					if ((t - 1) % displayStep != 0) {
						continue;
					}
					Object tickVal = t_coorValue.get(t);
					double tickPosition = getTickPosition(tickVal);
					if (t == 1 && tickPosition == getLeftX()) {
						continue;
					}

					if (isPolygonalRegion) {
						java.awt.Polygon polygon = new java.awt.Polygon();
						double polarLen = getTickPosition(tickVal) - getLeftX();
						for (int n = 1; n <= angleAxis.t_coorValue.length(); n++) {
							Object angleTick = angleAxis.t_coorValue.get(n);
							double angle = angleAxis.getTickPosition(angleTick);
							Point2D polarPoint = new Point2D.Double(polarLen,
									angle);
							Point2D p = pc.getScreenPoint(polarPoint);
							polygon.addPoint((int) p.getX(), (int) p.getY());
						}
						if (!angleAxis.isCircleAngle()) {
							polygon.addPoint((int) orginalPoint.getX(),
									(int) orginalPoint.getY());
						}
						area2 = new java.awt.geom.Area(polygon);
						regionShape = new java.awt.geom.Area(area2);
						if (area1 != null) {
							((java.awt.geom.Area) regionShape).subtract(area1);
						}
						tmpcc = regionColors.chartColorValue(++rc);
						drawRegion(regionShape, tmpcc, step);
						area1 = area2;

						if (t == tCount && tickPosition != getRightX()) {
							polarLen = getAxisLength();
							polygon = new java.awt.Polygon();
							for (int n = 1; n <= angleAxis.t_coorValue.length(); n++) {
								Object angleTick = angleAxis.t_coorValue.get(n);
								double angle = angleAxis
										.getTickPosition(angleTick);
								Point2D polarPoint = new Point2D.Double(
										polarLen, angle);
								Point2D p = pc.getScreenPoint(polarPoint);
								polygon.addPoint((int) p.getX(), (int) p.getY());
							}
							if (!angleAxis.isCircleAngle()) {
								polygon.addPoint((int) orginalPoint.getX(),
										(int) orginalPoint.getY());
							}
							regionShape = new java.awt.geom.Area(polygon);
							((java.awt.geom.Area) regionShape).subtract(area1);
							tmpcc = regionColors.chartColorValue(++rc);
							drawRegion(regionShape, tmpcc, step);
						}
					} else { // 扇形
						double x, y, w, h, tmpLen;
						tmpLen = tickPosition - orginalPoint.getX();
						x = orginalPoint.getX() - tmpLen;
						y = orginalPoint.getY() - tmpLen;
						w = tmpLen * 2;
						h = w;

						Arc2D sector = new Arc2D.Double(x, y, w, h,
								angleAxis.startAngle, angleAxis.endAngle
										- angleAxis.startAngle,
								java.awt.geom.Arc2D.PIE);

						area2 = new java.awt.geom.Area(sector);
						regionShape = new java.awt.geom.Area(area2);
						if (area1 != null) {
							((java.awt.geom.Area) regionShape).subtract(area1);
						}
						tmpcc = regionColors.chartColorValue(++rc);
						drawRegion(regionShape, tmpcc, step);
						area1 = area2;

						if (t == tCount && tickPosition != getRightX()) {
							tmpLen = getRightX() - orginalPoint.getX();
							x = orginalPoint.getX() - tmpLen;
							y = orginalPoint.getY() - tmpLen;
							w = tmpLen * 2;
							h = w;

							sector = new java.awt.geom.Arc2D.Double(x, y, w, h,
									angleAxis.startAngle, angleAxis.endAngle
											- angleAxis.startAngle,
									java.awt.geom.Arc2D.PIE);

							regionShape = new java.awt.geom.Area(sector);
							if (area1 != null) {
								((java.awt.geom.Area) regionShape)
										.subtract(area1);
							}
							tmpcc = regionColors.chartColorValue(++rc);
							drawRegion(regionShape, tmpcc, step);
						}
					}
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
				double angle1 = 0, angle2, polarLen;
				polarLen = polarAxis.getAxisLength();
				int tCount = t_coorValue.length();

				double x, y, w, h;
				x = orginalPoint.getX() - polarLen;
				y = orginalPoint.getY() - polarLen;
				w = polarLen * 2;
				h = w;
				int rc = 0;
				for (int t = 1; t <= tCount; t++) {
					Object tickVal = t_coorValue.get(t);
					double tickPosition = getTickPosition(tickVal);
					if (t == 1) {
						angle1 = tickPosition;
						continue;
					}
					angle2 = tickPosition;

					if (isPolygonalRegion) {
						java.awt.Polygon polygon = new java.awt.Polygon();
						addPolarPoint(polygon, pc, polarLen, angle1);
						addPolarPoint(polygon, pc, polarLen, angle2);
						polygon.addPoint((int) orginalPoint.getX(),
								(int) orginalPoint.getY());

						regionShape = polygon;
						tmpcc = regionColors.chartColorValue(++rc);
						drawRegion(regionShape, tmpcc, step);

						angle1 = angle2;

						if (isCircleAngle() && t == tCount) {
							polygon = new java.awt.Polygon();
							addPolarPoint(polygon, pc, polarLen, angle1);
							addPolarPoint(polygon, pc, polarLen, 360);
							polygon.addPoint((int) orginalPoint.getX(),
									(int) orginalPoint.getY());
							regionShape = polygon;
							tmpcc = regionColors.chartColorValue(++rc);
							drawRegion(regionShape, tmpcc, step);
						}
					} else { // 扇形
						Arc2D sector = new Arc2D.Double(x, y, w, h, angle1,
								angle2 - angle1, java.awt.geom.Arc2D.PIE);

						regionShape = new java.awt.geom.Area(sector);
						tmpcc = regionColors.chartColorValue(++rc);
						drawRegion(regionShape, tmpcc, step);
						angle1 = angle2;

						if (isCircleAngle() && t == tCount) {
							sector = new java.awt.geom.Arc2D.Double(x, y, w, h,
									angle1, 360 - angle1,
									java.awt.geom.Arc2D.PIE);
							regionShape = new java.awt.geom.Area(sector);
							tmpcc = regionColors.chartColorValue(++rc);
							drawRegion(regionShape, tmpcc, step);
						}
					}
				}
			}
			break;
		}

		Utils.setTransparent(g, 1.0f);

	}

	/**
	 * 绘制中间层
	 */
	public void draw() {
		if (!isVisible()) {
			return;
		}
		drawAxisBorder();

		int tickSize = t_coorValue.length();
		if (tickSize == 0) {
			return;
		}

		ArrayList coorList = e.getCoorList();
		double x, y;
		Color tickColor = axisColor;
		Graphics2D g = e.getGraphics();
		if (Utils.setStroke(g, tickColor, scaleStyle, scaleWeight)) {
			switch (location) {
			case Consts.AXIS_LOC_H:
			case Consts.AXIS_LOC_POLAR:
				// 绘制刻度线
				y = getBottomY() + coorThick;
				for (int t = 1; t <= tickSize; t++) {
					if ((t - 1) % displayStep != 0) { // t-1， 第1个刻度开始绘制，然后再间隔
						continue;
					}
					x = getTickPosition(t_coorValue.get(t));
					if (scalePosition == Consts.TICK_RIGHTUP) {
						Utils.drawLine(g, x, y - scaleLength, x, y);
					} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
						Utils.drawLine(g, x, y, x, y + scaleLength);
					} else if (this.scalePosition == Consts.TICK_CROSS) {
						Utils.drawLine(g, x, y - scaleLength / 2, x, y
								+ scaleLength / 2);
					}
				}
				break;
			case Consts.AXIS_LOC_V:
				x = getLeftX() - coorThick;
				for (int t = 1; t <= tickSize; t++) {
					if ((t - 1) % displayStep != 0) {
						continue;
					}
					y = getTickPosition(t_coorValue.get(t));
					if (scalePosition == Consts.TICK_RIGHTUP) {
						Utils.drawLine(g, x, y, x + scaleLength, y);
					} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
						Utils.drawLine(g, x - scaleLength, y, x, y);
					} else if (this.scalePosition == Consts.TICK_CROSS) {
						Utils.drawLine(g, x - scaleLength / 2, y, x
								+ scaleLength / 2, y);
					}
				}
				break;
			case Consts.AXIS_LOC_ANGLE:
				for (int i = 0; i < coorList.size(); i++) {
					Object coor = coorList.get(i);
					if (!(coor instanceof PolarCoor)) {
						continue;
					}
					PolarCoor pc = (PolarCoor) coor;
					if (pc.getAngleAxis() != this) {
						continue;
					}
					TickAxis polarAxis = (TickAxis) pc.getPolarAxis();
					double angle, polarLen;
					polarLen = polarAxis.getAxisLength();
					int tCount = t_coorValue.length();
					for (int t = 1; t <= tCount; t++) {
						if ((t - 1) % displayStep != 0) {
							continue;
						}
						Object tickVal = t_coorValue.get(t);
						angle = getTickPosition(tickVal);

						Point2D b = null, e = null;
						if (scalePosition == Consts.TICK_RIGHTUP) {
							b = new Point2D.Double(polarLen, angle);
							e = new Point2D.Double(polarLen + scaleLength,
									angle);
						} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
							b = new Point2D.Double(polarLen - scaleLength,
									angle);
							e = new Point2D.Double(polarLen, angle);
						} else if (this.scalePosition == Consts.TICK_CROSS) {
							b = new Point2D.Double(polarLen - scaleLength / 2,
									angle);
							e = new Point2D.Double(polarLen + scaleLength / 2,
									angle);
						}
						if (b != null) {
							b = pc.getScreenPoint(b);
							e = pc.getScreenPoint(e);
							Utils.drawLine(g, b, e);
						}
					}
				}
				break;
			}
		}

	}

	protected int adjustLabelPosition(Point2D p) {
		double x = p.getX(), y = p.getY();
		int locationType = Consts.LOCATION_CB;
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			if (scalePosition == Consts.TICK_RIGHTUP) {
				y -= scaleLength;
				y -= labelIndent;
				locationType = Consts.LOCATION_CB;
			} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
				y += scaleLength;
				y += labelIndent;
				locationType = Consts.LOCATION_CT;
			} else { // 交叉和无刻度时，标签默认画在下边，下边最常用；
				y += scaleLength / 2;
				y += labelIndent;
				locationType = Consts.LOCATION_CT;
			}
			break;
		case Consts.AXIS_LOC_V:
			if (scalePosition == Consts.TICK_RIGHTUP) {
				x += scaleLength;
				x += labelIndent;
				locationType = Consts.LOCATION_LM;
			} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
				x -= scaleLength;
				x -= labelIndent;
				locationType = Consts.LOCATION_RM;
			} else { // 交叉和无刻度时，标签默认画在左边，左边最常用；
				x -= scaleLength / 2;
				x -= labelIndent;
				locationType = Consts.LOCATION_RM;
			}
			break;
		case Consts.AXIS_LOC_ANGLE:
			locationType = Utils.getAngleTextLocation(y);
			break;
		}
		p.setLocation(x, y);
		return locationType;
	}

	/**
	 * 绘制前景层
	 */
	public void drawFore() {
		if (!isVisible()) {
			return;
		}
		int tickSize = t_coorValue.length();
		if (tickSize == 0) {
			return;
		}

		double x, y, length;
		int locationType;
		length = getAxisLength();
		Color c;
		ArrayList<ICoor> coorList = e.getCoorList();
		Point2D p;
		Font font;
		if (allowLabels) {
			if (labelColor == null) {
				c = axisColor;
			} else {
				c = labelColor;
			}
			font = Utils.getFont(labelFont, labelStyle, labelSize);
			switch (location) {
			case Consts.AXIS_LOC_H:
			case Consts.AXIS_LOC_POLAR:
				for (int t = 1; t <= tickSize; t++) {
					if ((t - 1) % labelStep != 0) {
						continue;
					}
					x = getTickPosition(t_coorValue.get(t));
					y = getBottomY() + coorThick;
					p = new Point2D.Double(x, y);
					locationType = adjustLabelPosition(p);
					String txt = getCoorText(t_coorValue.get(t));
					Utils.drawText(e, txt, p.getX(), p.getY(), font, c,
							labelStyle, labelAngle, locationType,
							labelOverlapping);
				}
				break;
			case Consts.AXIS_LOC_V:
				for (int t = 1; t <= tickSize; t++) {
					if ((t - 1) % labelStep != 0) {
						continue;
					}
					x = getLeftX() - coorThick;
					y = getTickPosition(t_coorValue.get(t));
					p = new Point2D.Double(x, y);
					locationType = adjustLabelPosition(p);
					String txt = getCoorText(t_coorValue.get(t));
					Utils.drawText(e, txt, p.getX(), p.getY(), font, c,
							labelStyle, labelAngle, locationType,
							labelOverlapping);
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
					double angle, polarLen;
					polarLen = polarAxis.getAxisLength();
					int tCount = t_coorValue.length();
					for (int t = 1; t <= tCount; t++) {
						if ((t - 1) % labelStep != 0) {
							continue;
						}
						Object tickVal = t_coorValue.get(t);
						angle = getTickPosition(tickVal);
						p = new Point2D.Double(polarLen, angle);
						locationType = adjustLabelPosition(p);
						p = pc.getScreenPoint(p);
						String txt = getCoorText(tickVal);
						Utils.drawText(e, txt, p.getX(), p.getY(), font, c,
								labelStyle, labelAngle, locationType,
								labelOverlapping);
					}
				}
				break;
			}
		}

		// Draw axis title
		if (!StringUtils.isValidString(title)) {
			return;
		}
		font = Utils.getFont(titleFont, titleStyle, titleSize);
		if (titleColor == null) {
			c = axisColor;
		} else {
			c = titleColor;
		}
		double tmp;
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			x = getLeftX() + length / 2;
			tmp = maxLabelHeight() + coorThick;
			y = getBottomY();
			if (scalePosition == Consts.TICK_RIGHTUP) {
				y -= scaleLength;
				y -= labelIndent * 2; // 标题与标签之见也空出indent
				y -= tmp;
				y -= titleIndent;
				locationType = Consts.LOCATION_CB;
			} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
				y += scaleLength;
				y += labelIndent * 2;
				y += tmp;
				y += titleIndent;
				locationType = Consts.LOCATION_CT;
			} else { // 交叉和无刻度时，标题默认画在下边，下边最常用；
				y += scaleLength / 2;
				y += labelIndent * 2;
				y += tmp;
				y += titleIndent;
				locationType = Consts.LOCATION_CT;
			}
			Utils.drawText(e, title, x, y, font, c, titleStyle, titleAngle,
					locationType, true);
			break;
		case Consts.AXIS_LOC_V:
			tmp = maxLabelWidth();
			x = getLeftX();
			y = getTopY() + length / 2;
			if (scalePosition == Consts.TICK_RIGHTUP) {
				x += scaleLength;
				x += labelIndent * 2;
				x += tmp;
				x += titleIndent;
				locationType = Consts.LOCATION_LM;
			} else if (this.scalePosition == Consts.TICK_LEFTDOWN) {
				x -= scaleLength;
				x -= labelIndent * 2;
				x -= tmp;
				x -= titleIndent;
				locationType = Consts.LOCATION_RM;
			} else { // 交叉和无刻度时，标题默认画在左边，左边最常用；
				x -= scaleLength / 2;
				x -= labelIndent * 2;
				x -= tmp;
				x -= titleIndent;
				locationType = Consts.LOCATION_RM;
			}
			Utils.drawText(e, title, x, y, font, c, titleStyle, titleAngle,
					locationType, true);
			break;
		case Consts.AXIS_LOC_ANGLE:
			// 角轴标题不绘制
			break;
		}
	}

	String getCoorText(Object coorValue) {
		return coorValue.toString();
	}

	private int maxLabelSize(boolean getHeight) {
		if (!allowLabels) {
			return 0;
		}
		int size = t_coorValue.length();
		Graphics2D g = e.getGraphics();
		int max = 0;
		for (int i = 1; i <= size; i++) {
			Object coory = t_coorValue.get(i);
			if (coory == null) {
				continue;
			}
			String txt = getCoorText(coory);
			Font font = Utils.getFont(labelFont, labelStyle, labelSize);
			Rectangle rect = Utils.getTextSize(txt, g, labelStyle, labelAngle,
					font);
			if (getHeight) {
				if (rect.height > max) {
					max = rect.height;
				}
			} else {
				if (rect.width > max) {
					max = rect.width;
				}
			}
		}
		return max;
	}

	/**
	 * 获取刻度标签的高度
	 * @return 返回标签中最高的值
	 */
	public int maxLabelHeight() {
		return maxLabelSize(true);
	}

	/**
	 * 获取刻度标签的宽度
	 * @return 返回标签中最宽的值
	 */
	public int maxLabelWidth() {
		return maxLabelSize(false);
	}

	/**
	 * 获取绘图参考点（基点）的坐标
	 * 默认情况下，基点为坐标的左下点，目前不支持坐标值从上往下以及右往左的情形。
	 * 数值轴（NumericAxis）的基点会被用户设置，数值轴时覆盖该方法。
	 * 角轴没有基点
	 */
	public Point2D getBasePoint(ICoor coor) {
		TickAxis otherAxis;
		if (coor.getAxis1() == this) {
			otherAxis = coor.getAxis2();
		} else {
			otherAxis = coor.getAxis1();
		}
		switch (location) {
		case Consts.AXIS_LOC_H:
			return new Point2D.Double(getLeftX(), otherAxis.getBottomY());
		case Consts.AXIS_LOC_POLAR:
			return new Point2D.Double(getLeftX(), getBottomY());
		case Consts.AXIS_LOC_V:
			return new Point2D.Double(otherAxis.getLeftX(), getBottomY());
		}
		return null;
	}

	transient Sequence t_coorValue = new Sequence();
	transient int t_coorWidth = 0;
	transient Engine e;
	transient double coorThick = 0;// 缺省3D坐标台的厚度
	transient boolean useGradient = false;// 该属性根据轴上的图元是否使用了渐变色来绘制一些炫效果，比如3D平台

	/**
	 * 设置图形引擎
	 * @param e 图形引擎
	 */
	public void setEngine(Engine e) {
		this.e = e;
		// 目前的轴属性没有支持Para对象的，也即不支持属性使用映射轴
	}

	/**
	 * 获取图形引擎
	 * @return 图形引擎
	 */
	public Engine getEngine() {
		return e;
	}

	/**
	 * 比较两个轴是否相等
	 * 由于图形里面的轴名称是唯一的，
	 * 所以，该方法中仅用名字来判断是否相等，而不必比较详细属性。
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		String otherName = ((IAxis) obj).getName();
		return otherName.equals(name);
	}

	/**
	 * 获取超链接坐标区域
	 * 
	 * @return Shape 该方法无意义，返回null
	 */
	public ArrayList<Shape> getShapes() {
		return null;
	}

	/**
	 * 获取超链接
	 * 
	 * @return 该方法无意义，返回null
	 */
	public ArrayList<String> getLinks() {
		return null;
	}

	public abstract boolean isEnumAxis();

	public abstract boolean isDateAxis();

	public abstract boolean isNumericAxis();
	public abstract void checkDataMatch(Sequence data);

}
