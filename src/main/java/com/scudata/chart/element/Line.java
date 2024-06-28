package com.scudata.chart.element;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

import com.scudata.chart.*;
import com.scudata.chart.edit.*;
import com.scudata.chart.resources.ChartMessage;
import com.scudata.common.MessageManager;
import com.scudata.dm.*;
/**
 * 线图元
 * 线图元仅可表现折线图，也可配合属性closeArea表现为区域图
 * @author Joancy
 *
 */
public class Line extends Dot {
	// **首尾相连
	public boolean endToHead = false;

	// 填充跟坐标轴或者回转的封闭区域
	public boolean closedArea = false;
	public Para areaColor = new Para(null); // 区域填充颜色

	// 堆积的直线或区域
	public int stackType = Consts.STACK_NONE;

	// 后续点都从第一点放射连接
	public boolean radiateLine = false;

	// 相邻点都使用阶梯线连接
	public boolean stairLine = false;

	// 箭头，固定长度
	public int arrow = Consts.LINE_ARROW_NONE;

	/**
	 * 缺省参数的构造函数
	 */
	public Line() {
	}

	/**
	 * 是否堆积类型
	 * @return 如果是返回true，否则false
	 */
	public boolean isStacked() {
		return stackType > Consts.STACK_NONE;
	}

	/**
	 * 获取编辑用的参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();

		ParamInfo.setCurrent(Line.class, this);
		paramInfos.add(new ParamInfo("endToHead", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("closedArea", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("areaColor", Consts.INPUT_CHARTCOLOR));
		paramInfos.add(new ParamInfo("stackType", Consts.INPUT_STACKTYPE));
		paramInfos.add(new ParamInfo("radiateLine", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("stairLine", Consts.INPUT_CHECKBOX));
		
		ParamInfo arrowPI = new ParamInfo("arrow", Consts.INPUT_SIMPLE_ARROW);

		ParamInfoList superPF = super.getParamInfoList();
		MessageManager mm = ChartMessage.get();
		String groupName = mm.getMessage("point");
		superPF.getParams(groupName).add(2,arrowPI);//放到线形一起
		
		paramInfos.addAll( superPF );
		return paramInfos;
	}

	/**
	 * 绘制背景层
	 */
	public void drawBack() {
		if (!isVisible() || !shadow) {
			return;
		}
		drawStep(1);
	}

	/**
	 * 绘制中间层
	 */
	public void draw() {
		if (!isVisible()) {
			return;
		}
		drawStep(2);
	}

	private Point2D getStairPoint(Point2D p1, Point2D p2) {
		if (p1 == null)
			return null;
		return new Point2D.Double(p1.getX(), p2.getY());
	}

	private void drawStep2(int index, ArrayList<Point2D> linePoints) {
		Color bc = lineColor.colorValueNullAsDef(index);
		int style = lineStyle.intValue(index);
		float weight = lineWeight.floatValue(index);
		ChartColor areaCcr = areaColor.chartColorValue(index);

		Graphics2D g = e.getGraphics();
		ICoor coor = getCoor();
		Shape shape;
		if (stairLine) {// 阶梯线时，增加两点之间的阶梯点
			ArrayList<Point2D> stairPoints = new ArrayList<Point2D>();
			Point2D last = linePoints.get(0);
			for (int i = 1; i < linePoints.size(); i++) {
				Point2D p = linePoints.get(i);
				Point2D m = getStairPoint(last, p);
				stairPoints.add(last);
				stairPoints.add(m);
				last = p;
			}
			stairPoints.add(last);
			linePoints = stairPoints;
		}
		if (closedArea) {
			// || stairLine , 阶梯线的封闭区域只能跟坐标封闭，也即要添加下述点；
			if (!endToHead || stairLine) {// 不回转时，需要添加跟坐标轴的起始和结束基点
				if (isPhysicalCoor() || coor.isCartesianCoor()) {
					Point2D p1 = linePoints.get(0);
					Point2D pL = linePoints.get(linePoints.size() - 1);// Point
																		// last
					Point2D p0, pe;// Point end
					if( isPhysicalCoor() ){
						p0 = new Point2D.Double(p1.getX(), 0);
						pe = new Point2D.Double(pL.getX(), 0);
					}
					else{
						TickAxis ta1 = coor.getAxis1();
						Point2D pb = ta1.getBasePoint(coor);
						if (ta1.getLocation() == Consts.AXIS_LOC_H) {// 垂向横轴时
							p0 = new Point2D.Double(p1.getX(), pb.getY());
							pe = new Point2D.Double(pL.getX(), pb.getY());
						} else {// 垂向纵轴
							p0 = new Point2D.Double(pb.getX(), p1.getY());
							pe = new Point2D.Double(pb.getX(), pL.getY());
						}
					}
					linePoints.add(0, p0);
					linePoints.add(pe);
				} else {// 极坐标系总是以极轴原点为基点
					PolarCoor pc = (PolarCoor) coor;
					TickAxis ta = pc.getPolarAxis();
					linePoints.add(ta.getBasePoint(coor));
				}
			}
			shape = Utils.getPath2D(linePoints, true);
			Rectangle rect = shape.getBounds();
			if(Utils.setPaint(g, rect.x, rect.y, rect.width, rect.height, areaCcr)){
				Utils.fillPaint(g, shape, transparent);
			}
//			包含封闭区域的shape仍然保留直接画shape
			if (Utils.setStroke(g, bc, style, weight)) {
				g.draw(shape);
			}
		} else {// 不填充，画折线
//			shape = Utils.getPath2D(linePoints, false);
//			直线使用Path2D时，某种极限情况下，线都在同一条斜线上，且线特别多，目前测试300段以上，会出现绘制的shape毛刺，然后中间多出
//			莫名其妙的横线，改回折线从头开始一段段画  2019年11月29日
			if (Utils.setStroke(g, bc, style, weight)) {
//				g.draw(shape);
				Point2D last = linePoints.get(0);
				for (int i = 1; i < linePoints.size(); i++) {
					Point2D p = linePoints.get(i);
					Utils.drawLine(g, last, p, arrow);
					last=p;
				}
				if(endToHead){
					Utils.drawLine(g, last, linePoints.get(0),arrow);
				}
			}
		}
	}

	private void drawLine(int index, ArrayList<Point2D> linePoints, int step) {
		if(linePoints==null || linePoints.isEmpty()) return;
		
		Color bc = lineColor.colorValueNullAsDef(index);
		int style = lineStyle.intValue(index);
		float weight = lineWeight.floatValue(index);
		Graphics2D g = e.getGraphics();
		Point2D last = null;
		switch (step) {
		case 1:
			if (stairLine) {
				for (int i = 0; i < linePoints.size(); i++) {
					Point2D p = linePoints.get(i);
					Point2D m = getStairPoint(last, p);
					Utils.drawLine1(g, last, m, style, weight);
					Utils.drawLine1(g, m, p, style, weight);
					last = p;
				}
			} else if (radiateLine) {
				last = linePoints.get(0);
				for (int i = 1; i < linePoints.size(); i++) {
					Point2D p = linePoints.get(i);
					Utils.drawLine1(g, last, p, style, weight);
				}
			} else {
				for (int i = 0; i < linePoints.size(); i++) {
					Point2D p = linePoints.get(i);
					Utils.drawLine1(g, last, p, style, weight);
					last = p;
				}
			}
			break;
		case 2:
			if (stairLine) {
				drawStep2(index, linePoints);
			} else if (radiateLine) {// 此处不能调整为if(radiateLine)
										// else结构，得stairLine优先画
				if (Utils.setStroke(g, bc, style, weight)) {
					last = linePoints.get(0);
					for (int i = 1; i < linePoints.size(); i++) {
						Point2D p = linePoints.get(i);
						Utils.drawLine(g, last, p, arrow);
					}
				}
			} else {
				drawStep2(index, linePoints);
			}
			break;
		case 3:
			break;
		}
	}

	// 直角系垂向轴为枚举轴或者极坐标系的角轴为枚举轴时，可以堆积
	private void drawEnumBasedLine(int step) {
		ICoor coor = getCoor();
		EnumAxis ea = (EnumAxis) coor.getAxis1();// 只有1轴为枚举轴才是枚举基础绘制
		Sequence enumData = getAxisData(ea.getName());

		int catCount = categories.length();//ea.
		int serCount = series.length();
		if (serCount == 0) {
			ArrayList<Point2D> linePoints = new ArrayList<Point2D>();
			for (int c = 1; c <= catCount; c++) { // 挨个分类画柱子
				String catName = (String) categories.get(c);
				int index = Utils.indexOf(enumData, catName, null);
				if (index == 0) { // 某个分类和系列的数值缺少
					continue;
				}
				Object val1 = data1.get(index);
				Object val2 = data2.get(index);
				Point2D numericP = coor.getNumericPoint(val1, val2);
				Point2D p = coor.getScreenPoint( numericP );
				linePoints.add(p);
			}
			drawLine(1, linePoints, step);
			drawDots(step, false);
			return;
		}

		ArrayList[] serLines = new ArrayList[serCount];
		for (int s = 0; s < serCount; s++) {
			ArrayList<Point2D> linePoints = new ArrayList<Point2D>();
			serLines[s] = linePoints;
		}
		ArrayList<MarkerPoint> markerPoints = new ArrayList<MarkerPoint>();
		for (int c = 1; c <= catCount; c++) {
			String catName = (String) categories.get(c);
			int index = 0;
			if (isStacked()) {
				Point2D basePoint = ea.getBasePoint(coor);
				Point2D lastPoint = null;
				double len = 0;
				for (int s = 1; s <= serCount; s++) {
					String serName = (String) series.get(s);
					index = Utils.indexOf(enumData, catName, serName);
					if (index == 0) { // 某个分类和系列的数值缺少
						continue;
					}
					Object val1 = Column.discardSeries(data1.get(index));
					Object val2 = Column.discardSeries(data2.get(index));
					if (stackType == Consts.STACK_PERCENT) {
						if(val2 instanceof Number){
							val2 = Column.getPercentValue(val1,val2,data1,data2);
						}else{
							val1 = Column.getPercentValue(val2,val1,data2,data1);
						}
					}

					Point2D p;
					if (coor.isCartesianCoor()) {
						p = coor.getScreenPoint(val1, val2);
						if (s > 1) {
							if (ea.getLocation() == Consts.AXIS_LOC_H) {
								len = basePoint.getY() - p.getY();
								p = new Point2D.Double(p.getX(),
										lastPoint.getY() - len);
							} else {
								len = p.getX() - basePoint.getX();
								p = new Point2D.Double(lastPoint.getX() + len,
										p.getY());
							}
						}
						serLines[s - 1].add(p);
						lastPoint = p;
					} else {
						PolarCoor pc = (PolarCoor) coor;
						p = pc.getPolarPoint(val1, val2);
						if (s > 1) {
							if (ea.getLocation() == Consts.AXIS_LOC_POLAR) {
								p = new Point2D.Double(p.getX(), p.getY()
										+ lastPoint.getY());
							} else {
								p = new Point2D.Double(lastPoint.getX()
										+ p.getX(), p.getY());
							}
						}
						serLines[s - 1].add(pc.getScreenPoint(p));
						lastPoint = p;
					}
					markerPoints.add(new MarkerPoint(index, lastPoint));
				}
			} else {
				for (int s = 1; s <= serCount; s++) {
					String serName = (String) series.get(s);
					index = Utils.indexOf(enumData, catName, serName);
					if (index == 0) { // 某个分类和系列的数值缺少
						continue;
					}
					Object val1 = Column.discardSeries(data1.get(index));
					Object val2 = Column.discardSeries(data2.get(index));
					Point2D p;
					if (coor.isCartesianCoor()) {
						p = coor.getScreenPoint(val1, val2);
						serLines[s - 1].add(p);
					} else {
						PolarCoor pc = (PolarCoor) coor;
						p = pc.getPolarPoint(val1, val2);
						serLines[s - 1].add(pc.getScreenPoint(p));
					}

					markerPoints.add(new MarkerPoint(index, p));
				}
			}

		}

		for (int i = serCount - 1; i >= 0; i--) {
			drawLine(i + 1, serLines[i], step);
		}

		for (int i = 0; i < markerPoints.size(); i++) {
			MarkerPoint sp = markerPoints.get(i);
			Shape shape = drawADot(sp.index, sp.p, step);
			if(shape!=null){
				int index = sp.index;
				String title = getTipTitle(index);
				addLink(shape, htmlLink.stringValue(index), title,linkTarget.stringValue(index));
			}
		}
	}

	private void drawDots(int step, boolean discardSeries) {
		// 枚举轴的折线，都只按分类计算枚举轴的坐标，点后于折线绘制，点要盖住线
		int size = pointSize();
		
		for (int i = 1; i <= size; i++) {
			Point2D p = getNumericPoint(i,discardSeries);
			Shape shape = drawADot(i, p, step);
			if(shape!=null){
				String title = getTipTitle(i);
				addLink(shape, htmlLink.stringValue(i), title,linkTarget.stringValue(i));
			}
		}
	}

	private void drawStep(int step) {
		ICoor coor = getCoor();
		if (coor!=null && coor.isEnumBased()) {// 枚举轴基础的直线，要根据系列将数据拆分成多根线
			drawEnumBasedLine(step);
		} else {// 否则就是一根线
			ArrayList<Point2D> linePoints = new ArrayList<Point2D>();
			int size = pointSize();
			for (int i = 1; i <= size; i++) {
				Point2D p = getScreenPoint(i);
				linePoints.add(p);
			}
			drawLine(1, linePoints, step);
			drawDots(step, false);
		}
	}

	/**
	 * 绘制前景层
	 */
	public void drawFore() {
		if (!isVisible()) {
			return;
		}
		drawStep(3);
	}

	/**
	 * 绘图前的数据准备
	 */
	public void prepare() {
		super.prepare();
		Column.checkStackProperties(this);
	}

	/**
	 * 克隆线图元的属性到l
	 * @param l 线图元
	 */
	public void clone(Line l){
		super.clone(l);
		l.endToHead = endToHead;
		l.closedArea = closedArea;
		l.areaColor = areaColor;
		l.stackType = stackType;
		l.radiateLine = radiateLine;
		l.stairLine = stairLine;
		l.arrow = arrow;
	}
	
	/**
	 * 深度克隆一个线图元
	 * @return 克隆好的线图元对象
	 */
	public Object deepClone() {
		Line l = new Line();
		clone(l);
		return l;
	}
	

	class MarkerPoint {
		int index;
		Point2D p;

		public MarkerPoint(int index, Point2D p) {
			this.index = index;
			this.p = p;
		}
	}
}
