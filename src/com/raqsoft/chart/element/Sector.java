package com.raqsoft.chart.element;

import com.raqsoft.chart.*;
import com.raqsoft.common.*;

import java.awt.*;
import java.awt.geom.*;

import com.raqsoft.dm.*;
import com.raqsoft.util.Variant;
import com.raqsoft.chart.edit.*;

/**
 * 扇形图，只能应用于极坐标系
 * 通常表现为饼图
 * @author Joancy
 *
 */
public class Sector extends Ring {
	// 文字引导线
	public Para textLineStyle = new Para(new Integer(Consts.LINE_SOLID));
	public Para textLineWeight = new Para(new Float(1));
	public Para textLineColor = new Para(Color.lightGray);

	// 有标示文字引线时， 引线的最大边界
	private transient double outerRadius = 0;

	/**
	 * 缺省参数的构造函数
	 */
	public Sector() {
		stackType = Consts.STACK_PERCENT;
	}

	/**
	 * 绘制背景层
	 */
	public void drawBack() {
		super.drawBack();
		// 有标示文字引线时， 引线的最大边界
		ICoor coor = getCoor();
		if (coor.isPolarCoor()) {
			PolarCoor pc = (PolarCoor) coor;
			TickAxis ta = (TickAxis) pc.getPolarAxis();
			double outLen = ta.getAxisLength() / 10;
			outerRadius = ta.getAxisLength() + outLen;
		}
	}

	/**
	 * 绘制中间层
	 */
	public void draw() {
		ICoor coor = getCoor();
		PolarCoor pc = (PolarCoor) coor;
		EnumAxis ea = coor.getEnumAxis();
		Point2D p;
		if (categories.length() == 1) {
			int serCount = series.length();
			Point2D lastPoint = null;
			String catName = categories.get(1).toString();
			for (int s = 1; s <= serCount; s++) {
				String serName = (String) series.get(s);
				int index = Utils.indexOf(data1, catName, serName);
				if (index == 0) { // 某个分类和系列的数值缺少
					continue;
				}
				Object val1 = discardSeries(data1.get(index));
				Object val2 = discardSeries(data2.get(index));
				if (stackType == Consts.STACK_PERCENT) {
					if(val2 instanceof Number){
						val2 = getPercentValue(val1,val2,data1,data2);
					}else{
						val1 = getPercentValue(val2,val1,data2,data1);
					}
				}
				p = pc.getPolarPoint(val1, val2);
				TickAxis angleAxis = pc.getAngleAxis();
				double start, extent;
				if (lastPoint == null) {
					start = angleAxis.startAngle;
				} else {
					start = lastPoint.getY();
				}
				extent = p.getY();
				double angle = start + extent / 2;
				Point2D txtP = new Point2D.Double(p.getX(), angle);
				drawCategoryAndLine(s,txtP);
				lastPoint = new Point2D.Double(p.getX(), start + extent);
			}
		} else {
			int catCount = categories.length();
			TickAxis axisP = pc.getPolarAxis();
			TickAxis axisA = pc.getAngleAxis();
			double angleRange = axisA.getAxisLength();
			for( int i=1; i<=catCount; i++){
				Object cat = categories.get(i);
				double pR = axisP.getValueLen(cat);
				double pA = axisA.startAngle + angleRange*i/(catCount+1);
				p = new Point2D.Double(pR, pA);
				drawCategoryAndLine(i,p);
			}
		}
	}

	/**
	 * 获取柱宽
	 * @param ia 刻度轴
	 * @param index 数值序号
	 * @return 实数精度的柱宽
	 */
	public double getColumnWidth(TickAxis ia, int index) {
		ICoor coor = getCoor();
		EnumAxis ea = coor.getEnumAxis();
		double colWidth = series.length();
		if (colWidth == 0)
			colWidth = 1;
		double tmp = ia.getValueRadius(colWidth);
		return tmp;

	}

	protected boolean isFillPie() {
		return true;
	}

	// 画引线以及分类
	protected void drawCategoryAndLine(int index, Point2D polarIn) {
		PolarCoor pc = (PolarCoor) getCoor();
		EnumAxis ea = pc.getEnumAxis();
		String txt;
		if (categories.length() == 1) {
			txt = Variant.toString(series.get(index));
		} else {
			txt = Variant.toString(categories.get(index));
		}
		if (!StringUtils.isValidString(txt)) {
			return;
		}
		// 文字引线
		Graphics2D g = e.getGraphics();
		int style = textLineStyle.intValue(index);
		float weight = textLineWeight.intValue(index);
		Point2D pIn = pc.getScreenPoint(polarIn);
		Point2D polarOut = new Point2D.Double(outerRadius, polarIn.getY());
		Point2D pOut = pc.getScreenPoint(polarOut);
		if (Utils.setStroke(g, textLineColor.colorValue(index), style, weight)) {
			Utils.drawLine(g, pIn, pOut);
		}

		String fontName = textFont.stringValue(index);
		int fontStyle = textStyle.intValue(index);
		int fontSize = textSize.intValue(index);
		Color c = textColor.colorValue(index);
		Utils.drawPolarPointText(e, txt, pc, polarOut, fontName, fontStyle,
				fontSize, c, textOverlapping);
	}

	/**
	 * 获取参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(Sector.class, this);

		String group = "textline";
		paramInfos.add(group, new ParamInfo("textLineStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("textLineWeight",
				Consts.INPUT_INTEGER));
		paramInfos.add(group,
				new ParamInfo("textLineColor", Consts.INPUT_COLOR));

		paramInfos.addAll(super.getParamInfoList());

		//		去掉扇形的物理坐标描述
		ParamInfo pi = paramInfos.getParamInfoByName("data1");
		String tmp = pi.getTitle();
		int i = tmp.indexOf('/');
		if(i>0){
			tmp = tmp.substring(0,i);
		}
		pi.setTitle( tmp );

		pi = paramInfos.getParamInfoByName("data2");
		tmp = pi.getTitle();
		i = tmp.indexOf('/');
		if(i>0){
			tmp = tmp.substring(0,i);
		}
		pi.setTitle( tmp );
		return paramInfos;
	}

	/**
	 * 绘图前的计算准备，数据合法性检查
	 */
	public void prepare() {
		super.prepare();
		ICoor coor = getCoor();
		if (coor.isCartesianCoor()) {
			throw new RuntimeException(
					"Sector graph does not support cartesian coordinate system.");
		}

		if (!isStacked()) { // 扇图要求堆积类型
			throw new RuntimeException(
					"Sector graph must be stacked by value or percent.");
		}
		EnumAxis ea = coor.getEnumAxis();
		if (ea.getLocation() != Consts.AXIS_LOC_POLAR) {
			throw new RuntimeException(
					"Sector graph must specify an enumeration axis as polar axis.");
		}

		// 形如["张三","李四","王五"]的枚举数据；由于轴总是按照分类来解释长度；对于饼图来说，应该是同一个圈，应该隶属同一个
		// 分类，故调整数据为: [" ,张三"," ,李四"," ,王五"]；即都是 空 分类。饼图数据就统一了，空分类不影响标签绘制
		String eaName = ea.getName();
		Sequence enumData = getAxisData(eaName);
		Sequence data = null;
		int size = enumData.length();
		for (int i = 1; i <= size; i++) {
			Object val = enumData.get(i);
			Object series = Utils.parseSeries(val);
			if (series == null) {
				if (data == null) {
					data = new Sequence();
				}
				data.set(i, " ," + val);
			} else {
				break;
			}
		}
		setAxisData(eaName, data);
		ea.gapRatio = 0;// 扇形图，gap总是为0
//		父类中准备数据时对当前categories赋值，sector调整了分类值，需要重新prepare
		super.prepare();
	}
	
	/**
	 * 克隆数据值
	 * @param s 另一个扇形
	 */
	public void clone(Sector s){
		super.clone(s);
	}
	
	/**
	 * 深度克隆扇图元
	 * @return 克隆后的扇图元
	 */
	public Object deepClone() {
		Sector s = new Sector();
		clone(s);
		return s;
	}

}
