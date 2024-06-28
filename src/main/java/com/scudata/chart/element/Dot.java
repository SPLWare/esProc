package com.scudata.chart.element;

import java.awt.*;
import java.awt.geom.*;

import com.scudata.chart.*;
import com.scudata.chart.edit.*;
import com.scudata.common.*;
/**
 * 点图元
 * 点图元通常可以表现气泡图
 * @author Joancy
 *
 */
public class Dot extends DataElement {
	public static String NOT_IN_DEFINE = "Data is not in defined.";
	public Para lineStyle = new Para(new Integer(Consts.LINE_SOLID));
	public Para lineWeight = new Para(new Float(1));
	public Para lineColor = new Para(Consts.LEGEND_P_LINECOLOR);

	public Para markerStyle = new Para(new Integer(Consts.PT_CIRCLE),Consts.LEGEND_P_MARKERSTYLE); // 点的形状
	public Para markerColor = new Para(Consts.LEGEND_P_FILLCOLOR); // 点的填充颜色，边框颜色用lineColor

	// radius1或2为0时，线或点的粗度或半径,该半径缺省0值的意义为，自动跟随lineWeight调整，
	// 调整过后的值不能小于2，也即总有半径，不能出现半径为0的情况，不画点需要设置点的形状为空，不能设置该半径为0
	public Para markerWeight = new Para(0);

	public Para radius1 = new Para(0);// 点在1轴的业务半径,值为轴计算长度
	public Para radius2 = new Para(0);// 点在2轴的业务半径,值为轴计算长度

	// 标示文字
	public Para text = new Para(null);
	public Para textFont = new Para();//"宋体"
	public Para textStyle = new Para(new Integer(0));
	public Para textSize = new Para(new Integer(12));
	public Para textColor = new Para(Color.black);

	// 透明度
	public float transparent = 1f;

	// 文字重叠显示
	public boolean textOverlapping = true;

	// 是否有阴影
	public boolean shadow = false;

	/**
	 * 缺省参数的构造函数
	 */
	public Dot() {
	}

	protected String getText(int index){
		return text.stringValue(index);
	}

	/**
	 * 获取点图元的编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();

		ParamInfo.setCurrent(Dot.class, this);

		paramInfos.add(new ParamInfo("transparent", Consts.INPUT_DOUBLE));
		paramInfos.add(new ParamInfo("textOverlapping", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("shadow", Consts.INPUT_CHECKBOX));

		String group = "point";
		paramInfos.add(group, new ParamInfo("markerStyle",
				Consts.INPUT_POINTSTYLE));
		paramInfos.add(group,
				new ParamInfo("lineStyle", Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("lineWeight", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("lineColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("markerColor",
				Consts.INPUT_CHARTCOLOR));
		paramInfos.add(group,
				new ParamInfo("markerWeight", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("radius1", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("radius2", Consts.INPUT_DOUBLE));

		group = "text";
		paramInfos.add(group, new ParamInfo("text"));
		paramInfos.add(group, new ParamInfo("textFont", Consts.INPUT_FONT));
		paramInfos.add(group,
				new ParamInfo("textStyle", Consts.INPUT_FONTSTYLE));
		paramInfos.add(group, new ParamInfo("textSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("textColor", Consts.INPUT_COLOR));

		// group = "链接";
		// paramInfos.add(group,new ParamInfo("tip"));
		// paramInfos.add(group,new ParamInfo("url"));
		// paramInfos.add(group,new ParamInfo("target"));

		paramInfos.addAll(super.getParamInfoList());
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

	/**
	 * 绘制前景层
	 */
	public void drawFore() {
		if (!isVisible()) {
			return;
		}
		drawStep(3);
	}

	private void drawStep(int step) {
		// 数据点
		int size = pointSize();
		for (int i = 1; i <= size; i++) {
			try {
				Point2D p = getNumericPoint(i,false);
				Shape shape = drawADot(i, p, step);
				if(shape!=null){
					String title = getTipTitle(i);
					addLink(shape, htmlLink.stringValue(i), title,linkTarget.stringValue(i));
				}
			} catch (RuntimeException re) {
				// 点不分基于枚举轴的分类来绘图，当有不在枚举轴中定义的分类或者系列时，丢弃该点
				if (re.getMessage().startsWith(NOT_IN_DEFINE)) {
					continue;
				}
				throw re;
			}
		}
	}

	/*
	 * 画点图元的一个数据点
	 * 要绘制点p的【数值坐标】；
	 * 注意，物理坐标系时，数值坐标已经等于 屏幕坐标。
	 */
	protected Shape drawADot(int index, Point2D p, int step) {
		ICoor coor = getCoor();
		Graphics2D g = e.getGraphics();
		int shape = markerStyle.intValue(index);
		Shape linkShape=null;
		
			if (isPhysicalCoor() || coor.isCartesianCoor()) {
				double radiusx, radiusy, val;
				if(isPhysicalCoor()){
					val = radius1.doubleValue(index);
					radiusx = e.getXPixel(val);
					val = radius2.doubleValue(index);
					radiusy = e.getXPixel(val);
				}else{
					CartesianCoor cc = (CartesianCoor) coor;
					p = cc.getScreenPoint(p); 
					TickAxis ia = cc.getXAxis();
					if (ia == cc.getAxis1()) {
						val = radius1.doubleValue(index);
						radiusx = ia.getValueRadius(val);
						val = radius2.doubleValue(index);
						radiusy = cc.getAxis2().getValueRadius(val);
					} else {
						val = radius1.doubleValue(index);
						radiusy = ia.getValueRadius(val);
						val = radius2.doubleValue(index);
						radiusx = cc.getAxis2().getValueRadius(val);
					}
				}

				double rw = markerWeight.doubleValue(index);
				int style = lineStyle.intValue(index);
				float weight = lineWeight.floatValue(index);
				switch (step) {
				case 1:
					linkShape = Utils.drawCartesianPoint1(g, p, shape, radiusx, radiusy, rw,
							style, weight,transparent);
					break;
				case 2:
					ChartColor backColor = markerColor.chartColorValue(index);
					Color foreColor = lineColor.colorValueNullAsDef(index);
					Utils.drawCartesianPoint2(g, p, shape, radiusx, radiusy, rw,
							style, weight, backColor, foreColor, transparent);
					break;
				case 3:
					String txt = text.stringValue(index);
					if (!StringUtils.isValidString(txt)) {
						return null;
					}
					double x = p.getX();
					double y = p.getY();
					// 数据点上的标示文字
					x = p.getX();
					if (radiusy > 0) {
						y = p.getY() - radiusy;
					} else {
						y = p.getY() - rw;
					}
					String fontName = textFont.stringValue(index);
					int fontStyle = textStyle.intValue(index);
					int fontSize = textSize.intValue(index);
					Font font = Utils.getFont(fontName, fontStyle, fontSize);
					Color c = textColor.colorValue(index);
					Utils.drawText(e, txt, x, y, font, c, fontStyle, 0,
							Consts.LOCATION_CB, textOverlapping);
					break;
				}
		} else {
			// 极坐标系
			PolarCoor pc = (PolarCoor) coor;
			TickAxis ia = pc.getPolarAxis();
			double radiusR, radiusA, val;
			if (ia == pc.getAxis1()) {
				val = radius1.doubleValue(index);
				radiusR = ia.getValueRadius(val);
				val = radius2.doubleValue(index);
				radiusA = pc.getAxis2().getValueRadius(val);
			} else {
				val = radius1.doubleValue(index);
				radiusA = ia.getValueRadius(val);
				val = radius2.doubleValue(index);
				radiusR = pc.getAxis2().getValueRadius(val);
			}

			double rw = markerWeight.doubleValue(index);
			int style = lineStyle.intValue(index);
			float weight = lineWeight.floatValue(index);
			// p = pc.getPolarPoint(v1, v2);
			switch (step) {
			case 1:
				linkShape = Utils.drawPolarPoint1(g, p, shape, radiusR, radiusA, rw, style,
						weight, pc,transparent);
				break;
			case 2:
				ChartColor backColor = markerColor.chartColorValue(index);
				Color foreColor = lineColor.colorValueNullAsDef(index);
				Utils.drawPolarPoint2(g, p, shape, radiusR, radiusA, rw, style,
						weight, pc, backColor, foreColor, transparent);
				break;
			case 3:
				String txt = text.stringValue(index);
				if (!StringUtils.isValidString(txt)) {
					return null;
				}
				double angle = p.getY();
				Point2D txtP = new Point2D.Double(p.getX() + radiusR, angle);
				String fontName = textFont.stringValue(index);
				int fontStyle = textStyle.intValue(index);
				int fontSize = textSize.intValue(index);
				Color c = textColor.colorValue(index);

				Utils.drawPolarPointText(e, txt, pc, txtP, fontName, fontStyle,
						fontSize, c, textOverlapping);
				break;
			}
		}
		return linkShape;

	}

	/**
	 * 图元是否定义了渐变颜色
	 * @return 如果有渐变色返回true，否则返回false
	 */
	public boolean hasGradientColor() {
		return markerColor.hasGradientColor();
	}
	
	/**
	 * 当前属性值克隆到点图元d
	 * @param d 点图元实例
	 */
	public void clone(Dot d){
		super.clone(d);
		d.lineStyle = lineStyle;
		d.lineWeight = lineWeight;
		d.lineColor = lineColor;
		d.markerStyle = markerStyle;
		d.markerColor = markerColor;
		d.markerWeight = markerWeight;
		d.radius1 = radius1;
		d.radius2 = radius2;
		d.text = text;
		d.textFont = textFont;
		d.textStyle = textStyle;
		d.textSize = textSize;
		d.textColor = textColor;
		d.transparent = transparent;
		d.textOverlapping = textOverlapping;
		d.shadow = shadow;
	}
	
	/**
	 * 克隆一个点图元
	 * @return 克隆后的点图元
	 */
	public Object deepClone() {
		Dot d = new Dot();
		clone(d);
		return d;
	}

}
